package com.opsmind.knowledge.document.application;

import com.opsmind.knowledge.document.config.DocumentProperties;
import com.opsmind.knowledge.document.domain.DocumentException;
import com.opsmind.knowledge.document.domain.DocumentStatus;
import com.opsmind.knowledge.document.domain.IngestionTaskStatus;
import com.opsmind.knowledge.document.infrastructure.InMemoryDocumentRepository;
import com.opsmind.knowledge.document.infrastructure.InMemoryIngestionTaskRepository;
import com.opsmind.knowledge.document.infrastructure.LocalDocumentStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentServiceTests {

    @TempDir
    Path storageDirectory;

    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        DocumentProperties properties = new DocumentProperties(DataSize.ofMegabytes(1), storageDirectory);
        documentService = new DocumentService(
                new DocumentValidationService(properties),
                new LocalDocumentStorage(properties),
                new InMemoryDocumentRepository(),
                new InMemoryIngestionTaskRepository(),
                Clock.fixed(Instant.parse("2026-10-09T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void shouldStoreOnceAndReturnExistingDocumentForDuplicateContent() throws Exception {
        MockMultipartFile firstFile = textFile("first-name.txt", "same content");
        MockMultipartFile renamedFile = textFile("renamed.txt", "same content");

        DocumentUploadResult first = documentService.upload(firstFile);
        DocumentUploadResult duplicate = documentService.upload(renamedFile);

        assertThat(first.duplicate()).isFalse();
        assertThat(first.document().status()).isEqualTo(DocumentStatus.UPLOADED);
        assertThat(first.task().status()).isEqualTo(IngestionTaskStatus.PENDING);
        assertThat(duplicate.duplicate()).isTrue();
        assertThat(duplicate.document().id()).isEqualTo(first.document().id());
        assertThat(duplicate.task().id()).isEqualTo(first.task().id());
        try (var files = Files.list(storageDirectory)) {
            assertThat(files.filter(Files::isRegularFile)).hasSize(1);
        }
    }

    @Test
    void shouldReturnDocumentAndTaskStatus() {
        DocumentUploadResult uploaded = documentService.upload(textFile("guide.txt", "restart service"));

        DocumentUploadResult found = documentService.find(uploaded.document().id());

        assertThat(found.document()).isEqualTo(uploaded.document());
        assertThat(found.task()).isEqualTo(uploaded.task());
    }

    @Test
    void shouldReportMissingDocument() {
        assertThatThrownBy(() -> documentService.find(UUID.randomUUID()))
                .isInstanceOfSatisfying(DocumentException.class,
                        exception -> assertThat(exception.code()).isEqualTo("DOCUMENT_NOT_FOUND"));
    }

    private MockMultipartFile textFile(String filename, String content) {
        return new MockMultipartFile("file", filename, "text/plain", content.getBytes());
    }
}

