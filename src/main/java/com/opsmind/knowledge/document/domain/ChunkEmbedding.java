package com.opsmind.knowledge.document.domain;

import java.time.Instant;
import java.util.UUID;

public record ChunkEmbedding(
        UUID chunkId,
        UUID documentId,
        String model,
        int dimension,
        double[] vector,
        String contentHash,
        Instant createdAt) {

    public ChunkEmbedding {
        vector = vector.clone();
    }

    @Override
    public double[] vector() {
        return vector.clone();
    }
}
