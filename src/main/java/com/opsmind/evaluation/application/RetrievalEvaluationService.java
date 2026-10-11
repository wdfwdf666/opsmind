package com.opsmind.evaluation.application;

import com.opsmind.evaluation.domain.RetrievalEvaluationCase;
import com.opsmind.evaluation.domain.RetrievalEvaluationCaseResult;
import com.opsmind.evaluation.domain.RetrievalEvaluationDataset;
import com.opsmind.evaluation.domain.RetrievalEvaluationReport;
import com.opsmind.evaluation.domain.RetrievalMetricAtK;
import com.opsmind.evaluation.domain.RetrievalStrategy;
import com.opsmind.knowledge.document.application.HybridSearchService;
import com.opsmind.knowledge.document.application.RerankedSearchService;
import com.opsmind.knowledge.document.application.SemanticSearchService;
import com.opsmind.knowledge.document.domain.SemanticSearchHit;
import com.opsmind.knowledge.document.domain.SemanticSearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongSupplier;

@Service
public class RetrievalEvaluationService {

    private static final double NANOS_PER_MILLISECOND = 1_000_000.0;

    private final RetrievalEvaluationDatasetLoader datasetLoader;
    private final SemanticSearchService searchService;
    private final HybridSearchService hybridSearchService;
    private final RerankedSearchService rerankedSearchService;
    private final LongSupplier nanoTime;

    @Autowired
    public RetrievalEvaluationService(
            RetrievalEvaluationDatasetLoader datasetLoader,
            SemanticSearchService searchService,
            HybridSearchService hybridSearchService,
            RerankedSearchService rerankedSearchService) {
        this(datasetLoader, searchService, hybridSearchService, rerankedSearchService, System::nanoTime);
    }

    RetrievalEvaluationService(
            RetrievalEvaluationDatasetLoader datasetLoader,
            SemanticSearchService searchService,
            HybridSearchService hybridSearchService,
            RerankedSearchService rerankedSearchService,
            LongSupplier nanoTime) {
        this.datasetLoader = datasetLoader;
        this.searchService = searchService;
        this.hybridSearchService = hybridSearchService;
        this.rerankedSearchService = rerankedSearchService;
        this.nanoTime = nanoTime;
    }

    public RetrievalEvaluationReport run(String datasetName, UUID documentId, RetrievalStrategy requestedStrategy) {
        RetrievalStrategy strategy = requestedStrategy == null ? RetrievalStrategy.VECTOR : requestedStrategy;
        RetrievalEvaluationDataset dataset = datasetLoader.load(datasetName);
        int maxK = dataset.cutoffs().stream().mapToInt(Integer::intValue).max().orElseThrow();
        List<RetrievalEvaluationCaseResult> caseResults = new ArrayList<>(dataset.cases().size());
        String model = null;
        for (RetrievalEvaluationCase evaluationCase : dataset.cases()) {
            long startedAt = nanoTime.getAsLong();
            SemanticSearchResult searchResult = search(strategy, evaluationCase.query(), maxK, documentId);
            double latencyMs = (nanoTime.getAsLong() - startedAt) / NANOS_PER_MILLISECOND;
            if (model == null) {
                model = searchResult.model();
            }
            caseResults.add(toCaseResult(evaluationCase, searchResult, latencyMs));
        }
        List<Double> latencies = caseResults.stream().map(RetrievalEvaluationCaseResult::latencyMs).sorted().toList();
        return new RetrievalEvaluationReport(
                dataset.name(), dataset.description(), strategy, model, documentId, caseResults.size(), maxK,
                caseResults.stream().mapToInt(RetrievalEvaluationCaseResult::candidateCount).average().orElse(0),
                percentile(latencies, 0.50), percentile(latencies, 0.95),
                dataset.cutoffs().stream().sorted().map(k -> metricAtK(caseResults, k)).toList(),
                List.copyOf(caseResults));
    }

    private SemanticSearchResult search(RetrievalStrategy strategy, String query, int topK, UUID documentId) {
        return switch (strategy) {
            case VECTOR -> searchService.search(query, topK, documentId);
            case HYBRID -> hybridSearchService.search(query, topK, documentId);
            case RERANKED -> rerankedSearchService.search(query, topK, documentId);
        };
    }

    private RetrievalEvaluationCaseResult toCaseResult(
            RetrievalEvaluationCase evaluationCase, SemanticSearchResult searchResult, double latencyMs) {
        List<String> headings = searchResult.hits().stream().map(SemanticSearchHit::headingPath).toList();
        Set<String> expected = new HashSet<>(evaluationCase.expectedHeadingPaths());
        Integer firstRelevantRank = null;
        for (int index = 0; index < headings.size(); index++) {
            if (expected.contains(headings.get(index))) {
                firstRelevantRank = index + 1;
                break;
            }
        }
        return new RetrievalEvaluationCaseResult(
                evaluationCase.id(), evaluationCase.query(), List.copyOf(evaluationCase.expectedHeadingPaths()),
                headings, firstRelevantRank, searchResult.candidateCount(), latencyMs);
    }

    private RetrievalMetricAtK metricAtK(List<RetrievalEvaluationCaseResult> cases, int k) {
        double recall = cases.stream().mapToDouble(result -> recallAtK(result, k)).average().orElse(0);
        double mrr = cases.stream().mapToDouble(result -> reciprocalRankAtK(result, k)).average().orElse(0);
        double hitRate = cases.stream().filter(result -> isHitAtK(result, k)).count() / (double) cases.size();
        return new RetrievalMetricAtK(k, recall, mrr, hitRate);
    }

    private double recallAtK(RetrievalEvaluationCaseResult result, int k) {
        Set<String> expected = new HashSet<>(result.expectedHeadingPaths());
        long relevantRetrieved = result.retrievedHeadingPaths().stream().limit(k).distinct()
                .filter(expected::contains).count();
        return relevantRetrieved / (double) expected.size();
    }

    private double reciprocalRankAtK(RetrievalEvaluationCaseResult result, int k) {
        return isHitAtK(result, k) ? 1.0 / result.firstRelevantRank() : 0;
    }

    private boolean isHitAtK(RetrievalEvaluationCaseResult result, int k) {
        return result.firstRelevantRank() != null && result.firstRelevantRank() <= k;
    }

    private double percentile(List<Double> sortedValues, double percentile) {
        int index = Math.max(0, (int) Math.ceil(percentile * sortedValues.size()) - 1);
        return sortedValues.get(index);
    }
}
