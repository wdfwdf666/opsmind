package com.opsmind.knowledge.document.api;

import com.opsmind.knowledge.document.application.DocumentEmbeddingResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record DocumentEmbeddingResponse(
        @Schema(format = "uuid") UUID documentId,
        String model,
        int dimension,
        int embeddedChunkCount) {

    public static DocumentEmbeddingResponse from(DocumentEmbeddingResult result) {
        return new DocumentEmbeddingResponse(
                result.documentId(), result.model(), result.dimension(), result.embeddedChunkCount());
    }
}
