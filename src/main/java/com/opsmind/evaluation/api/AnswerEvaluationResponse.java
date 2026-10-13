package com.opsmind.evaluation.api;

import com.opsmind.evaluation.domain.AnswerEvaluationReport;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

public record AnswerEvaluationResponse(
        String dataset,
        String description,
        String retrievalModel,
        String generationModel,
        @Schema(format = "uuid", nullable = true) UUID documentId,
        int topK,
        int caseCount,
        int answerableCaseCount,
        int unanswerableCaseCount,
        double answerCorrectness,
        @Schema(description = "Deterministic character-bigram overlap with cited excerpts; not an LLM faithfulness judge")
        double faithfulnessProxy,
        double citationPrecision,
        double citationRecall,
        double refusalPrecision,
        double refusalRecall,
        double refusalF1,
        double p50LatencyMs,
        double p95LatencyMs,
        int promptTokens,
        int completionTokens,
        int totalTokens,
        List<AnswerEvaluationCaseResponse> cases) {

    static AnswerEvaluationResponse from(AnswerEvaluationReport report) {
        return new AnswerEvaluationResponse(
                report.dataset(), report.description(), report.retrievalModel(), report.generationModel(),
                report.documentId(), report.topK(), report.caseCount(), report.answerableCaseCount(),
                report.unanswerableCaseCount(), report.answerCorrectness(), report.faithfulnessProxy(),
                report.citationPrecision(), report.citationRecall(), report.refusalPrecision(), report.refusalRecall(),
                report.refusalF1(), report.p50LatencyMs(), report.p95LatencyMs(), report.promptTokens(),
                report.completionTokens(), report.totalTokens(),
                report.cases().stream().map(AnswerEvaluationCaseResponse::from).toList());
    }
}
