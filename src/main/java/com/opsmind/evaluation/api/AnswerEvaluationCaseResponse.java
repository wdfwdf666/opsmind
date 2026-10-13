package com.opsmind.evaluation.api;

import com.opsmind.evaluation.domain.AnswerEvaluationCaseResult;
import com.opsmind.rag.application.RagRefusalReason;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record AnswerEvaluationCaseResponse(
        String id,
        String query,
        boolean answerable,
        boolean refused,
        @Schema(nullable = true) RagRefusalReason refusalReason,
        @Schema(nullable = true) Double topEvidenceScore,
        @Schema(nullable = true) Double answerCorrectness,
        @Schema(nullable = true, description = "Deterministic lexical support proxy, not a semantic faithfulness score")
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

    static AnswerEvaluationCaseResponse from(AnswerEvaluationCaseResult result) {
        return new AnswerEvaluationCaseResponse(
                result.id(), result.query(), result.answerable(), result.refused(),
                result.refusalReason(), result.topEvidenceScore(),
                result.answerCorrectness(), result.faithfulnessProxy(), result.expectedFacts(), result.matchedFacts(),
                result.expectedHeadingPaths(), result.citedHeadingPaths(), result.citationPrecision(),
                result.citationRecall(), result.latencyMs(), result.promptTokens(), result.completionTokens(),
                result.totalTokens(), result.answer());
    }
}
