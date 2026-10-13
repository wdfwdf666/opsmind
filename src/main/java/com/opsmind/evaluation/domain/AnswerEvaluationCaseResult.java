package com.opsmind.evaluation.domain;

import com.opsmind.rag.application.RagRefusalReason;

import java.util.List;

public record AnswerEvaluationCaseResult(
        String id,
        String query,
        boolean answerable,
        boolean refused,
        RagRefusalReason refusalReason,
        Double topEvidenceScore,
        Double answerCorrectness,
        Double faithfulnessProxy,
        List<String> expectedFacts,
        List<String> matchedFacts,
        List<String> expectedHeadingPaths,
        List<String> citedHeadingPaths,
        double citationPrecision,
        double citationRecall,
        double latencyMs,
        int promptTokens,
        int completionTokens,
        int totalTokens,
        String answer) {
}
