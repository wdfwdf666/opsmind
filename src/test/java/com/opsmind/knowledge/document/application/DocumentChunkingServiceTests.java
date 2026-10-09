package com.opsmind.knowledge.document.application;

import com.opsmind.knowledge.document.config.ChunkingProperties;
import com.opsmind.knowledge.document.domain.ChunkType;
import com.opsmind.knowledge.document.domain.DocumentContent;
import com.opsmind.knowledge.document.domain.DocumentException;
import com.opsmind.knowledge.document.domain.DocumentStatus;
import com.opsmind.knowledge.document.domain.KnowledgeDocument;
import com.opsmind.knowledge.document.infrastructure.InMemoryDocumentChunkRepository;
import com.opsmind.knowledge.document.infrastructure.InMemoryDocumentContentRepository;
import com.opsmind.knowledge.document.infrastructure.InMemoryDocumentRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentChunkingServiceTests {

    private static final Instant NOW = Instant.parse("2026-10-09T05:00:00Z");

    @Test
    void shouldReplaceChunksIdempotentlyAndPageChildren() {
        UUID documentId = UUID.randomUUID();
        InMemoryDocumentRepository documents = new InMemoryDocumentRepository();
        InMemoryDocumentContentRepository contents = new InMemoryDocumentContentRepository();
        InMemoryDocumentChunkRepository chunks = new InMemoryDocumentChunkRepository();
        documents.saveIfAbsent(document(documentId));
        contents.save(new DocumentContent(
                documentId, "手册", "text/markdown", "# 启动\n" + "检查配置和数据库连接。".repeat(20), 220, NOW));
        DocumentChunkingService service = service(documents, contents, chunks);

        DocumentChunkingResult first = service.rebuild(documentId);
        var firstPage = service.list(documentId, ChunkType.CHILD, 0, 100);
        DocumentChunkingResult second = service.rebuild(documentId);
        var secondPage = service.list(documentId, ChunkType.CHILD, 0, 100);

        assertThat(first.parentCount()).isPositive();
        assertThat(first.childCount()).isPositive();
        assertThat(second).isEqualTo(first);
        assertThat(secondPage.items()).extracting(chunk -> chunk.id())
                .containsExactlyElementsOf(firstPage.items().stream().map(chunk -> chunk.id()).toList());
        assertThat(secondPage.totalElements()).isEqualTo(first.childCount());
    }

    @Test
    void shouldRejectChunkingBeforeContentIsParsed() {
        UUID documentId = UUID.randomUUID();
        InMemoryDocumentRepository documents = new InMemoryDocumentRepository();
        documents.saveIfAbsent(document(documentId));
        DocumentChunkingService service = service(
                documents, new InMemoryDocumentContentRepository(), new InMemoryDocumentChunkRepository());

        assertThatThrownBy(() -> service.rebuild(documentId))
                .isInstanceOfSatisfying(DocumentException.class,
                        exception -> assertThat(exception.code()).isEqualTo("DOCUMENT_CONTENT_NOT_READY"));
    }

    private DocumentChunkingService service(
            InMemoryDocumentRepository documents,
            InMemoryDocumentContentRepository contents,
            InMemoryDocumentChunkRepository chunks) {
        return new DocumentChunkingService(
                documents,
                contents,
                chunks,
                new HeadingAwareDocumentChunker(new ChunkingProperties(120, 50, 10)),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private KnowledgeDocument document(UUID documentId) {
        return new KnowledgeDocument(
                documentId, "guide.md", "text/markdown", 100, "a".repeat(64), "guide.md",
                DocumentStatus.READY, NOW);
    }
}
