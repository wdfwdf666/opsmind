package com.opsmind.evaluation.domain;

import java.util.List;
import java.util.UUID;

public record AnswerEvaluationReport(
        String dataset,
        String description,
        String retrievalModel,
        String generationModel,
        UUID documentId,
        int topK,
        int caseCount,
        int answerableCaseCount,
        int unanswerableCaseCount,
        double answerCorrectness,
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
        List<AnswerEvaluationCaseResult> cases) {
}
