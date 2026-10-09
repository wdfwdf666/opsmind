package com.opsmind.knowledge.document.domain;

import java.util.UUID;

public record ScoredEmbeddedChunk(
        UUID chunkId,
        UUID parentId,
        UUID documentId,
        String filename,
        String headingPath,
        String childText,
        String parentText,
        double score) {
}
