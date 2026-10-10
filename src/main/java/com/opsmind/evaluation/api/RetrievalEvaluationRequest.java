package com.opsmind.evaluation.api;

import com.opsmind.evaluation.domain.RetrievalStrategy;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record RetrievalEvaluationRequest(
        @Schema(example = "opsmind-retrieval-v1", nullable = true) String dataset,
        @Schema(format = "uuid", nullable = true, description = "Optional document scope for reproducible evaluation")
        UUID documentId,
        @Schema(nullable = true, description = "Retrieval strategy; defaults to VECTOR")
        RetrievalStrategy strategy) {
}
