package com.opsmind.knowledge.document.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentTextCleanerTests {

    private final DocumentTextCleaner cleaner = new DocumentTextCleaner();

    @Test
    void shouldNormalizeLineEndingsControlsAndRepeatedBlankLines() {
        String source = "  标题  \r\n\r\n\r\n内容\u0000\t  \r下一行\u00a0 ";

        String cleaned = cleaner.clean(source);

        assertThat(cleaned).isEqualTo("标题\n\n内容\n下一行");
    }
}
