package com.opsmind.knowledge.document.infrastructure;

import com.opsmind.knowledge.document.config.DocumentProperties;
import com.opsmind.knowledge.document.domain.DocumentException;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TikaDocumentParserTests {

    @Test
    void shouldReportConfiguredTextLimitSeparatelyFromParseFailure() {
        DocumentProperties properties = new DocumentProperties(DataSize.ofMegabytes(1), Path.of("unused"), 10);
        TikaDocumentParser parser = new TikaDocumentParser(properties);

        assertThatThrownBy(() -> parser.parse(
                new ByteArrayInputStream("more than ten characters".getBytes()),
                "large.txt",
                "text/plain"))
                .isInstanceOfSatisfying(DocumentException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo("DOCUMENT_EXTRACTED_TEXT_TOO_LARGE"));
    }
}
