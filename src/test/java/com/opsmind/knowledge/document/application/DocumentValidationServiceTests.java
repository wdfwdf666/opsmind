package com.opsmind.knowledge.document.application;

import com.opsmind.knowledge.document.config.DocumentProperties;
import com.opsmind.knowledge.document.domain.DocumentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentValidationServiceTests {

    private DocumentValidationService validationService;

    @BeforeEach
    void setUp() {
        DocumentProperties properties = new DocumentProperties(DataSize.ofKilobytes(10), Path.of("target/test-documents"));
        validationService = new DocumentValidationService(properties);
    }

    @Test
    void shouldValidateTextDocumentAndSanitizeBrowserPath() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "C:\\fakepath\\runbook.md", "text/markdown", "# Recovery".getBytes());

        ValidatedDocument document = validationService.validate(file);

        assertThat(document.filename()).isEqualTo("runbook.md");
        assertThat(document.mediaType()).isEqualTo("text/markdown");
        assertThat(document.sha256()).hasSize(64);
    }

    @Test
    void shouldRejectUnsupportedExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "malware.exe", "application/octet-stream", new byte[]{1});

        assertThatThrownBy(() -> validationService.validate(file))
                .isInstanceOfSatisfying(DocumentException.class,
                        exception -> assertThat(exception.code()).isEqualTo("DOCUMENT_TYPE_NOT_SUPPORTED"));
    }

    @Test
    void shouldRejectSpoofedPdf() {
        MockMultipartFile file = new MockMultipartFile("file", "manual.pdf", "application/pdf", "not-pdf".getBytes());

        assertThatThrownBy(() -> validationService.validate(file))
                .isInstanceOfSatisfying(DocumentException.class,
                        exception -> assertThat(exception.code()).isEqualTo("DOCUMENT_SIGNATURE_INVALID"));
    }

    @Test
    void shouldRejectFileLargerThanConfiguredLimit() {
        byte[] oversized = new byte[(int) DataSize.ofKilobytes(10).toBytes() + 1];
        MockMultipartFile file = new MockMultipartFile("file", "large.txt", "text/plain", oversized);

        assertThatThrownBy(() -> validationService.validate(file))
                .isInstanceOfSatisfying(DocumentException.class,
                        exception -> assertThat(exception.code()).isEqualTo("DOCUMENT_TOO_LARGE"));
    }
}

