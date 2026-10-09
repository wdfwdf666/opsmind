package com.opsmind.knowledge.document.api;

import com.opsmind.knowledge.document.domain.SemanticSearchHit;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record SemanticSearchHitResponse(
        @Schema(format = "uuid") UUID chunkId,
        @Schema(format = "uuid") UUID parentId,
        @Schema(format = "uuid") UUID documentId,
        String filename,
        String headingPath,
        String childText,
        String parentText,
        double score) {

    public static SemanticSearchHitResponse from(SemanticSearchHit hit) {
        return new SemanticSearchHitResponse(
                hit.chunkId(), hit.parentId(), hit.documentId(), hit.filename(), hit.headingPath(),
                hit.childText(), hit.parentText(), hit.score());
    }
}
