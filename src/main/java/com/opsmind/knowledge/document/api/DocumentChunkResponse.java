package com.opsmind.knowledge.document.api;

import com.opsmind.knowledge.document.domain.ChunkType;
import com.opsmind.knowledge.document.domain.DocumentChunk;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "A parent context chunk or child retrieval chunk")
public record DocumentChunkResponse(
        @Schema(format = "uuid") UUID chunkId,
        @Schema(format = "uuid") UUID documentId,
        @Schema(format = "uuid", nullable = true) UUID parentId,
        ChunkType type,
        int ordinal,
        String headingPath,
        String text,
        String contentHash,
        int characterCount,
        @Schema(description = "Approximation based on four characters per token") int estimatedTokenCount,
        Instant createdAt) {

    public static DocumentChunkResponse from(DocumentChunk chunk) {
        return new DocumentChunkResponse(
                chunk.id(), chunk.documentId(), chunk.parentId(), chunk.type(), chunk.ordinal(),
                chunk.headingPath(), chunk.text(), chunk.contentHash(), chunk.characterCount(),
                chunk.estimatedTokenCount(), chunk.createdAt());
    }
}
