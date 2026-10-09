package com.opsmind.knowledge.document.domain;

import java.util.UUID;

public record EmbeddedChunk(
        UUID chunkId,
        UUID parentId,
        UUID documentId,
        String filename,
        String headingPath,
        String childText,
        String parentText,
        double[] vector) {

    public EmbeddedChunk {
        vector = vector.clone();
    }

    @Override
    public double[] vector() {
        return vector.clone();
    }
}
