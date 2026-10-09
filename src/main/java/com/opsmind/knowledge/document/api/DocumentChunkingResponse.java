package com.opsmind.knowledge.document.api;

import com.opsmind.knowledge.document.application.DocumentChunkingResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Result of rebuilding parent and child chunks")
public record DocumentChunkingResponse(
        @Schema(format = "uuid") UUID documentId,
        int parentCount,
        int childCount,
        int totalCount) {

    public static DocumentChunkingResponse from(DocumentChunkingResult result) {
        return new DocumentChunkingResponse(
                result.documentId(), result.parentCount(), result.childCount(), result.totalCount());
    }
}
