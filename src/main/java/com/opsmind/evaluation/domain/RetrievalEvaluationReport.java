package com.opsmind.evaluation.domain;

import java.util.List;
import java.util.UUID;

public record RetrievalEvaluationReport(
        String dataset,
        String description,
        RetrievalStrategy strategy,
        String model,
        UUID documentId,
        int caseCount,
        int maxK,
        double averageCandidateCount,
        double p50LatencyMs,
        double p95LatencyMs,
        List<RetrievalMetricAtK> metrics,
        List<RetrievalEvaluationCaseResult> cases) {
}
