package com.opsmind.knowledge.document.api;

import com.opsmind.knowledge.document.domain.SemanticSearchResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

public record SemanticSearchResponse(
        String query,
        String model,
        @Schema(format = "uuid", nullable = true) UUID documentId,
        int candidateCount,
        List<SemanticSearchHitResponse> hits) {

    public static SemanticSearchResponse from(SemanticSearchResult result) {
        return new SemanticSearchResponse(
                result.query(), result.model(), result.documentId(), result.candidateCount(),
                result.hits().stream().map(SemanticSearchHitResponse::from).toList());
    }
}
