package com.opsmind.rag.api;

import com.opsmind.rag.application.RagCitation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record RagCitationResponse(
        String sourceId,
        @Schema(format = "uuid") UUID chunkId,
        @Schema(format = "uuid", nullable = true) UUID parentId,
        @Schema(format = "uuid") UUID documentId,
        String filename,
        String headingPath,
        String excerpt,
        double score) {

    static RagCitationResponse from(RagCitation citation) {
        return new RagCitationResponse(
                citation.sourceId(), citation.chunkId(), citation.parentId(), citation.documentId(),
                citation.filename(), citation.headingPath(), citation.excerpt(), citation.score());
    }
}
