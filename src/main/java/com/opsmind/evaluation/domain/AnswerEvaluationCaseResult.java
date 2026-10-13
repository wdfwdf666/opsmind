package com.opsmind.evaluation.domain;

import java.util.List;

public record AnswerEvaluationCaseResult(
        String id,
        String query,
        boolean answerable,
        boolean refused,
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
