package com.opsmind.knowledge.document.application;

import com.opsmind.knowledge.document.config.EmbeddingProperties;
import com.opsmind.knowledge.document.config.RerankingProperties;
import com.opsmind.knowledge.document.domain.SemanticSearchHit;
import com.opsmind.knowledge.document.domain.SemanticSearchResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RerankedSearchServiceTests {

    @Test
    void shouldRetrieveWideCandidateSetAndReturnRerankerTopK() {
        UUID documentId = UUID.randomUUID();
        SemanticSearchHit first = hit(documentId, "First", 0.03);
        SemanticSearchHit second = hit(documentId, "Second", 0.02);
        HybridSearchService hybrid = mock(HybridSearchService.class);
        RerankingProvider reranker = mock(RerankingProvider.class);
        when(hybrid.search("query", 20, documentId)).thenReturn(new SemanticSearchResult(
                "query", "hybrid-model", documentId, 30, List.of(first, second)));
        when(reranker.model()).thenReturn("rerank-model");
        when(reranker.rerank("query", List.of(first, second), 1)).thenReturn(List.of(second));
        var embedding = new EmbeddingProperties("local", "vector-model", 256, 5, 50, "", "", 20);
        var reranking = new RerankingProperties(
                "local", "rerank-model", "", "", "instruction", 4, 20);
        var service = new RerankedSearchService(hybrid, reranker, embedding, reranking);

        SemanticSearchResult result = service.search("query", 1, documentId);

        assertThat(result.model()).isEqualTo("hybrid-model->rerank[rerank-model]");
        assertThat(result.candidateCount()).isEqualTo(30);
        assertThat(result.hits()).containsExactly(second);
        verify(hybrid).search("query", 20, documentId);
        verify(reranker).rerank("query", List.of(first, second), 1);
    }

    private SemanticSearchHit hit(UUID documentId, String heading, double score) {
        return new SemanticSearchHit(
                UUID.randomUUID(), UUID.randomUUID(), documentId, "runbook.md", heading,
                "child " + heading, "parent " + heading, score);
    }
}
