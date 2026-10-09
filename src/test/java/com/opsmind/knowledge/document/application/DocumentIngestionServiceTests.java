package com.opsmind.knowledge.document.application;

import com.opsmind.knowledge.document.config.DocumentProperties;
import com.opsmind.knowledge.document.domain.DocumentException;
import com.opsmind.knowledge.document.domain.DocumentStatus;
import com.opsmind.knowledge.document.domain.IngestionTaskStatus;
import com.opsmind.knowledge.document.infrastructure.InMemoryDocumentContentRepository;
import com.opsmind.knowledge.document.infrastructure.InMemoryDocumentRepository;
import com.opsmind.knowledge.document.infrastructure.InMemoryIngestionTaskRepository;
import com.opsmind.knowledge.document.infrastructure.LocalDocumentStorage;
import com.opsmind.knowledge.document.infrastructure.TikaDocumentParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentIngestionServiceTests {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-09T02:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path storageDirectory;

    private InMemoryDocumentRepository documentRepository;
    private InMemoryIngestionTaskRepository taskRepository;
    private InMemoryDocumentContentRepository contentRepository;
    private DocumentService documentService;
    private DocumentProperties properties;

    @BeforeEach
    void setUp() {
        properties = new DocumentProperties(DataSize.ofMegabytes(1), storageDirectory, 10_000);
        documentRepository = new InMemoryDocumentRepository();
        taskRepository = new InMemoryIngestionTaskRepository();
        contentRepository = new InMemoryDocumentContentRepository();
        documentService = new DocumentService(
                new DocumentValidationService(properties),
                new LocalDocumentStorage(properties),
                documentRepository,
                taskRepository,
                CLOCK);
    }

    @Test
    void shouldParseCleanPersistAndReturnIdempotentResult() {
        var uploaded = documentService.upload(new MockMultipartFile(
                "file", "runbook.html", "text/html",
                "<html><head><title>故障手册</title></head><body><h1>排查</h1><p>检查连接池</p></body></html>"
                        .getBytes()));
        DocumentIngestionService service = service(new TikaDocumentParser(properties));

        DocumentIngestionResult first = service.process(uploaded.document().id());
        DocumentIngestionResult second = service.process(uploaded.document().id());

        assertThat(first.document().status()).isEqualTo(DocumentStatus.READY);
        assertThat(first.task().status()).isEqualTo(IngestionTaskStatus.SUCCEEDED);
        assertThat(first.content().title()).isEqualTo("故障手册");
        assertThat(first.content().text()).contains("排查", "检查连接池");
        assertThat(first.content().characterCount()).isEqualTo(first.content().text().length());
        assertThat(second.content()).isEqualTo(first.content());
    }

    @Test
    void shouldRecordFailureAndAllowRetry() {
        var uploaded = documentService.upload(new MockMultipartFile(
                "file", "empty.txt", "text/plain", "placeholder".getBytes()));
        DocumentParser emptyParser = (stream, filename, mediaType) -> new ParsedDocument(null, mediaType, " \n\u0000 ");

        assertThatThrownBy(() -> service(emptyParser).process(uploaded.document().id()))
                .isInstanceOfSatisfying(DocumentException.class,
                        exception -> assertThat(exception.code()).isEqualTo("DOCUMENT_TEXT_EMPTY"));
        assertThat(documentRepository.findById(uploaded.document().id()).orElseThrow().status())
                .isEqualTo(DocumentStatus.FAILED);
        assertThat(taskRepository.findByDocumentId(uploaded.document().id()).orElseThrow().status())
                .isEqualTo(IngestionTaskStatus.FAILED);

        DocumentParser recoveredParser = (stream, filename, mediaType) ->
                new ParsedDocument("Recovered", mediaType, "recovered content");
        DocumentIngestionResult retried = service(recoveredParser).process(uploaded.document().id());

        assertThat(retried.document().status()).isEqualTo(DocumentStatus.READY);
        assertThat(retried.task().status()).isEqualTo(IngestionTaskStatus.SUCCEEDED);
        assertThat(retried.task().errorMessage()).isNull();
    }

    @Test
    void shouldRejectActiveLeaseAndReclaimStaleRunningTask() {
        UUID documentId = UUID.randomUUID();
        Instant startedAt = CLOCK.instant();
        taskRepository.getOrCreate(documentId, startedAt);

        boolean firstClaim = taskRepository.tryMarkRunning(
                documentId, startedAt, startedAt.minus(Duration.ofMinutes(5)));
        boolean activeClaim = taskRepository.tryMarkRunning(
                documentId, startedAt.plusSeconds(60), startedAt.minus(Duration.ofMinutes(4)));
        boolean staleClaim = taskRepository.tryMarkRunning(
                documentId, startedAt.plus(Duration.ofMinutes(6)), startedAt.plus(Duration.ofMinutes(1)));

        assertThat(firstClaim).isTrue();
        assertThat(activeClaim).isFalse();
        assertThat(staleClaim).isTrue();
    }

    private DocumentIngestionService service(DocumentParser parser) {
        return new DocumentIngestionService(
                new LocalDocumentStorage(properties),
                parser,
                new DocumentTextCleaner(),
                documentRepository,
                taskRepository,
                contentRepository,
                CLOCK);
    }
}
