package com.opsmind.knowledge.document.domain;

import java.util.List;
import java.util.UUID;

public interface ChunkEmbeddingRepository {

    void replaceAll(UUID documentId, String model, List<ChunkEmbedding> embeddings);

    List<EmbeddedChunk> findCandidates(String model, UUID documentId);
}
