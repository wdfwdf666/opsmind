package com.opsmind.knowledge.document.infrastructure;

import com.opsmind.knowledge.document.domain.DocumentRepository;
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
                "SELECT COUNT(*) FROM \"flyway_schema_history\" WHERE \"version\" = '1' AND \"success\" = TRUE",
                Integer.class);
        assertThat(migrationCount).isEqualTo(1);
    }

    private KnowledgeDocument document(String checksum, String filename) {
        return new KnowledgeDocument(
                UUID.randomUUID(),
                filename,
                "text/plain",
                12,
                checksum,
                checksum + ".txt",
                DocumentStatus.UPLOADED,
                Instant.parse("2026-10-09T00:00:00Z"));
    }
}
