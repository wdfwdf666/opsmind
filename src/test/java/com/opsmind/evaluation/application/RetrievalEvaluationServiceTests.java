package com.opsmind.evaluation.application;

import com.opsmind.evaluation.domain.RetrievalEvaluationCase;
import com.opsmind.evaluation.domain.RetrievalEvaluationDataset;
import com.opsmind.evaluation.domain.RetrievalStrategy;
import com.opsmind.knowledge.document.application.HybridSearchService;
import com.opsmind.knowledge.document.application.SemanticSearchService;
import com.opsmind.knowledge.document.domain.SemanticSearchHit;
import com.opsmind.knowledge.document.domain.SemanticSearchResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RetrievalEvaluationServiceTests {

    @Test
    void shouldCalculateRecallMrrHitRateAndNearestRankLatencyPercentiles() {
        UUID documentId = UUID.randomUUID();
        RetrievalEvaluationDatasetLoader loader = mock(RetrievalEvaluationDatasetLoader.class);
        SemanticSearchService searchService = mock(SemanticSearchService.class);
        HybridSearchService hybridSearchService = mock(HybridSearchService.class);
        var dataset = new RetrievalEvaluationDataset(
                "test-dataset", "deterministic metrics", List.of(1, 3), List.of(
                new RetrievalEvaluationCase("case-1", "query one", List.of("Expected A")),
                new RetrievalEvaluationCase("case-2", "query two", List.of("Expected B"))));
        when(loader.load("test-dataset")).thenReturn(dataset);
        when(searchService.search("query one", 3, documentId)).thenReturn(searchResult(
                documentId, hit("Other", 0.9), hit("Expected A", 0.8), hit("Third", 0.7)));
        when(searchService.search("query two", 3, documentId)).thenReturn(searchResult(
                documentId, hit("Other", 0.9), hit("Third", 0.8), hit("Fourth", 0.7)));
        long[] timestamps = {0, 10_000_000, 20_000_000, 50_000_000};
        AtomicInteger timestampIndex = new AtomicInteger();
        var service = new RetrievalEvaluationService(
                loader, searchService, hybridSearchService, () -> timestamps[timestampIndex.getAndIncrement()]);

        var report = service.run("test-dataset", documentId, RetrievalStrategy.VECTOR);

        assertThat(report.caseCount()).isEqualTo(2);
        assertThat(report.strategy()).isEqualTo(RetrievalStrategy.VECTOR);
        assertThat(report.model()).isEqualTo("test-model");
        assertThat(report.averageCandidateCount()).isEqualTo(3);
        assertThat(report.p50LatencyMs()).isEqualTo(10);
        assertThat(report.p95LatencyMs()).isEqualTo(30);
        assertThat(report.metrics()).hasSize(2);
        assertThat(report.metrics().get(0).k()).isEqualTo(1);
        assertThat(report.metrics().get(0).recall()).isZero();
        assertThat(report.metrics().get(0).meanReciprocalRank()).isZero();
        assertThat(report.metrics().get(0).hitRate()).isZero();
        assertThat(report.metrics().get(1).k()).isEqualTo(3);
        assertThat(report.metrics().get(1).recall()).isEqualTo(0.5);
        assertThat(report.metrics().get(1).meanReciprocalRank()).isEqualTo(0.25);
        assertThat(report.metrics().get(1).hitRate()).isEqualTo(0.5);
        assertThat(report.cases().getFirst().firstRelevantRank()).isEqualTo(2);
        assertThat(report.cases().get(1).firstRelevantRank()).isNull();
    }

    private SemanticSearchResult searchResult(UUID documentId, SemanticSearchHit... hits) {
        return new SemanticSearchResult(
                "query", "test-model", documentId, 3, List.of(hits));
    }

    private SemanticSearchHit hit(String headingPath, double score) {
        return new SemanticSearchHit(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "test.md",
                headingPath, "child", "parent", score);
    }
}
