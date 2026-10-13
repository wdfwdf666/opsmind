package com.opsmind.evaluation.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record AnswerEvaluationRequest(
        @Schema(example = "opsmind-answer-v1", nullable = true) String dataset,
        @Schema(format = "uuid", nullable = true, description = "Optional document scope for reproducible evaluation")
        UUID documentId,
        @Schema(example = "3", nullable = true, description = "Evidence count; dataset default is used when omitted")
        Integer topK) {
}
