package com.opsmind.knowledge.document.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChineseNgramTokenizerTests {

    private final ChineseNgramTokenizer tokenizer = new ChineseNgramTokenizer();

    @Test
    void shouldCreateChineseUnigramsBigramsAndNormalizedLatinTokens() {
        assertThat(tokenizer.tokenize("数据库 PostgreSQL17 回滚"))
                .contains("数", "据", "数据", "据库", "postgresql17", "回滚");
    }
}
