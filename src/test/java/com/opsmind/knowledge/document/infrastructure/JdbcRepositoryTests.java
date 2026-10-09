package com.opsmind.knowledge.document.infrastructure;

import com.opsmind.knowledge.document.domain.DocumentRepository;
import com.opsmind.knowledge.document.domain.DocumentQueryRepository;
import com.opsmind.knowledge.document.domain.DocumentChunk;
import com.opsmind.knowledge.document.domain.DocumentChunkRepository;
import com.opsmind.knowledge.document.domain.ChunkType;
import com.opsmind.knowledge.document.domain.ChunkEmbedding;
import com.opsmind.knowledge.document.domain.ChunkEmbeddingRepository;
import com.opsmind.knowledge.document.domain.DocumentStatus;
import com.opsmind.knowledge.document.domain.IngestionTaskRepository;
import com.opsmind.knowledge.document.domain.IngestionTaskStatus;
import com.opsmind.knowledge.document.domain.KnowledgeDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JdbcRepositoryTests {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private IngestionTaskRepository taskRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DocumentQueryRepository documentQueryRepository;

    @Autowired
    private DocumentChunkRepository documentChunkRepository;

    @Autowired
    private ChunkEmbeddingRepository chunkEmbeddingRepository;

    @Test
    void shouldPersistDocumentAndEnforceChecksumUniqueness() {
        String checksum = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        KnowledgeDocument first = document(checksum, "first.txt");
        KnowledgeDocument duplicate = document(checksum, "renamed.txt");

        DocumentRepository.SaveResult created = documentRepository.saveIfAbsent(first);
        DocumentRepository.SaveResult existing = documentRepository.saveIfAbsent(duplicate);

        assertThat(created.created()).isTrue();
        assertThat(existing.created()).isFalse();
        assertThat(existing.document().id()).isEqualTo(first.id());
        assertThat(documentRepository.findById(first.id())).contains(first);
    }

    @Test
    void shouldPersistSingleTaskPerDocumentAndApplyFlywayMigration() {
        String checksum = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        KnowledgeDocument document = document(checksum, "task-source.txt");
        documentRepository.saveIfAbsent(document);
        Instant now = Instant.parse("2026-10-09T01:00:00Z");

        var first = taskRepository.getOrCreate(document.id(), now);
        var second = taskRepository.getOrCreate(document.id(), now.plusSeconds(60));

        assertThat(first.status()).isEqualTo(IngestionTaskStatus.PENDING);
        assertThat(second.id()).isEqualTo(first.id());
        assertThat(taskRepository.findByDocumentId(document.id())).contains(first);
        Integer migrationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM \"flyway_schema_history\" WHERE \"version\" IN ('1', '2', '3', '4') AND \"success\" = TRUE",
                Integer.class);
        assertThat(migrationCount).isEqualTo(4);
    }

    @Test
    void shouldListDocumentsWithTasksInStableNewestFirstOrder() {
        KnowledgeDocument older = document(
                randomChecksum(), "older.txt", Instant.parse("2026-10-09T03:00:00Z"));
        KnowledgeDocument newer = document(
                randomChecksum(), "newer.txt", Instant.parse("2026-10-09T04:00:00Z"));
        documentRepository.saveIfAbsent(older);
        documentRepository.saveIfAbsent(newer);
        var olderTask = taskRepository.getOrCreate(older.id(), older.createdAt());
        var newerTask = taskRepository.getOrCreate(newer.id(), newer.createdAt());

        var result = documentQueryRepository.findPage(0, 100);
        var ids = result.items().stream().map(item -> item.document().id()).toList();

        assertThat(ids).contains(older.id(), newer.id());
        assertThat(ids.indexOf(newer.id())).isLessThan(ids.indexOf(older.id()));
        assertThat(result.items().stream().filter(item -> item.document().id().equals(newer.id())).findFirst())
                .hasValueSatisfying(item -> assertThat(item.taskId()).isEqualTo(newerTask.id()));
        assertThat(result.items().stream().filter(item -> item.document().id().equals(older.id())).findFirst())
                .hasValueSatisfying(item -> assertThat(item.taskId()).isEqualTo(olderTask.id()));
        assertThat(result.totalElements()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldAtomicallyReplaceAndPageDocumentChunks() {
        KnowledgeDocument document = document(randomChecksum(), "chunks.md");
        documentRepository.saveIfAbsent(document);
        UUID parentId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-10-09T05:00:00Z");
        DocumentChunk parent = new DocumentChunk(
                parentId, document.id(), null, ChunkType.PARENT, 0, "Guide > Start", "parent text",
                "b".repeat(64), 11, 3, createdAt);
        DocumentChunk child = new DocumentChunk(
                UUID.randomUUID(), document.id(), parentId, ChunkType.CHILD, 0, "Guide > Start", "child text",
                "c".repeat(64), 10, 3, createdAt);

        documentChunkRepository.replaceAll(document.id(), java.util.List.of(parent, child));
        var firstPage = documentChunkRepository.findPage(document.id(), ChunkType.CHILD, 0, 10);
        documentChunkRepository.replaceAll(document.id(), java.util.List.of(parent));
        var afterReplacement = documentChunkRepository.findPage(document.id(), ChunkType.CHILD, 0, 10);

        assertThat(firstPage.items()).containsExactly(child);
        assertThat(firstPage.totalElements()).isEqualTo(1);
        assertThat(afterReplacement.items()).isEmpty();
        assertThat(afterReplacement.totalElements()).isZero();
    }

    @Test
    void shouldReplaceEmbeddingsAndLoadSearchCandidatesWithParentContext() {
        KnowledgeDocument document = document(randomChecksum(), "embedded.md");
        documentRepository.saveIfAbsent(document);
        UUID parentId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-10-09T06:00:00Z");
        DocumentChunk parent = new DocumentChunk(
                parentId, document.id(), null, ChunkType.PARENT, 0, "Guide > Database", "complete parent context",
                "d".repeat(64), 23, 6, createdAt);
        DocumentChunk child = new DocumentChunk(
                childId, document.id(), parentId, ChunkType.CHILD, 0, "Guide > Database", "database child",
                "e".repeat(64), 14, 4, createdAt);
        documentChunkRepository.replaceAll(document.id(), java.util.List.of(parent, child));
        ChunkEmbedding first = new ChunkEmbedding(
                childId, document.id(), "test-model", 3, new double[]{1, 0, 0}, child.contentHash(), createdAt);
        ChunkEmbedding replacement = new ChunkEmbedding(
                childId, document.id(), "test-model", 3, new double[]{0, 1, 0}, child.contentHash(), createdAt.plusSeconds(1));

        chunkEmbeddingRepository.replaceAll(document.id(), "test-model", java.util.List.of(first));
        chunkEmbeddingRepository.replaceAll(document.id(), "test-model", java.util.List.of(replacement));
        var candidates = chunkEmbeddingRepository.findCandidates("test-model", document.id());

        assertThat(candidates).hasSize(1);
        assertThat(candidates.getFirst().chunkId()).isEqualTo(childId);
        assertThat(candidates.getFirst().parentText()).isEqualTo("complete parent context");
        assertThat(candidates.getFirst().vector()).containsExactly(0, 1, 0);
    }

    private KnowledgeDocument document(String checksum, String filename) {
        return document(checksum, filename, Instant.parse("2026-10-09T00:00:00Z"));
    }

    private KnowledgeDocument document(String checksum, String filename, Instant createdAt) {
        return new KnowledgeDocument(
                UUID.randomUUID(),
                filename,
                "text/plain",
                12,
                checksum,
                checksum + ".txt",
                DocumentStatus.UPLOADED,
                createdAt);
    }

    private String randomChecksum() {
        return UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
    }
}
