package com.opsmind.knowledge.document.application;

import com.opsmind.knowledge.document.config.EmbeddingProperties;
import com.opsmind.knowledge.document.config.HybridSearchProperties;
import com.opsmind.knowledge.document.domain.SemanticSearchHit;
import com.opsmind.knowledge.document.domain.SemanticSearchResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HybridSearchServiceTests {

    @Test
    void shouldFuseAndDeduplicateVectorAndLexicalRankingsWithRrf() {
        UUID documentId = UUID.randomUUID();
        SemanticSearchHit a = hit(documentId, "A");
        SemanticSearchHit b = hit(documentId, "B");
        SemanticSearchHit c = hit(documentId, "C");
        SemanticSearchService vector = mock(SemanticSearchService.class);
        Bm25SearchService lexical = mock(Bm25SearchService.class);
        when(vector.search("rollback", 20, documentId)).thenReturn(result("vector-model", documentId, a, b, c));
        when(lexical.search("rollback", documentId, 20)).thenReturn(result(Bm25SearchService.MODEL, documentId, b, c, a));
        var properties = new EmbeddingProperties("local", "vector-model", 256, 5, 50, "", "", 20);
        var service = new HybridSearchService(vector, lexical, properties, new HybridSearchProperties(60, 4, 20));

        var result = service.search("rollback", 3, documentId);

        assertThat(result.model()).isEqualTo("hybrid-rrf[vector-model+bm25-char-ngram-v1]");
        assertThat(result.hits()).extracting(SemanticSearchHit::headingPath).containsExactly("B", "A", "C");
        assertThat(result.hits()).extracting(SemanticSearchHit::chunkId).doesNotHaveDuplicates();
        verify(vector).search("rollback", 20, documentId);
        verify(lexical).search("rollback", documentId, 20);
    }

    private SemanticSearchResult result(String model, UUID documentId, SemanticSearchHit... hits) {
        return new SemanticSearchResult("rollback", model, documentId, 3, List.of(hits));
    }

    private SemanticSearchHit hit(UUID documentId, String heading) {
        return new SemanticSearchHit(
                UUID.randomUUID(), UUID.randomUUID(), documentId, "runbook.md", heading,
                "child " + heading, "parent " + heading, 0.9);
    }
}
