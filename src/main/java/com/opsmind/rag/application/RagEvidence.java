package com.opsmind.rag.application;

import java.util.UUID;

public record RagEvidence(
        String sourceId,
        UUID chunkId,
        UUID parentId,
        UUID documentId,
        String filename,
        String headingPath,
        String passage,
        String excerpt,
        double score) {
}
