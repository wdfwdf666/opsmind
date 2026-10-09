package com.opsmind.evaluation.api;

import com.opsmind.evaluation.domain.RetrievalEvaluationCaseResult;

import java.util.List;

public record RetrievalEvaluationCaseResponse(
        String caseId,
        String query,
        List<String> expectedHeadingPaths,
        List<String> retrievedHeadingPaths,
        Integer firstRelevantRank,
        int candidateCount,
        double latencyMs) {

    static RetrievalEvaluationCaseResponse from(RetrievalEvaluationCaseResult result) {
        return new RetrievalEvaluationCaseResponse(
                result.caseId(), result.query(), result.expectedHeadingPaths(), result.retrievedHeadingPaths(),
                result.firstRelevantRank(), result.candidateCount(), result.latencyMs());
    }
}
