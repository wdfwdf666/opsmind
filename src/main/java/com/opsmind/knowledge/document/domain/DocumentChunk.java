package com.opsmind.knowledge.document.domain;

import java.time.Instant;
import java.util.UUID;

public record DocumentChunk(
        UUID id,
        UUID documentId,
        UUID parentId,
        ChunkType type,
        int ordinal,
        String headingPath,
        String text,
        String contentHash,
        int characterCount,
        int estimatedTokenCount,
        Instant createdAt) {
}
