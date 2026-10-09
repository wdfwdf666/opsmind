package com.opsmind.evaluation.domain;

import java.util.List;

public record RetrievalEvaluationCaseResult(
        String caseId,
        String query,
        List<String> expectedHeadingPaths,
        List<String> retrievedHeadingPaths,
        Integer firstRelevantRank,
        int candidateCount,
        double latencyMs) {
}
