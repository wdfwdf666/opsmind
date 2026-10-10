package com.opsmind.evaluation.api;

import com.opsmind.evaluation.domain.RetrievalEvaluationReport;
import com.opsmind.evaluation.domain.RetrievalMetricAtK;
import com.opsmind.evaluation.domain.RetrievalStrategy;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

public record RetrievalEvaluationResponse(
        String dataset,
        String description,
        RetrievalStrategy strategy,
        String model,
        @Schema(format = "uuid", nullable = true) UUID documentId,
        int caseCount,
        int maxK,
        double averageCandidateCount,
        double p50LatencyMs,
        double p95LatencyMs,
        List<RetrievalMetricAtK> metrics,
        List<RetrievalEvaluationCaseResponse> cases) {

    static RetrievalEvaluationResponse from(RetrievalEvaluationReport report) {
        return new RetrievalEvaluationResponse(
                report.dataset(), report.description(), report.strategy(), report.model(), report.documentId(), report.caseCount(),
                report.maxK(), report.averageCandidateCount(), report.p50LatencyMs(), report.p95LatencyMs(),
                report.metrics(), report.cases().stream().map(RetrievalEvaluationCaseResponse::from).toList());
    }
}
