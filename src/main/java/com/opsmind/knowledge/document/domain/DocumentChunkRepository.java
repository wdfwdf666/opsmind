package com.opsmind.knowledge.document.domain;

import java.util.List;
import java.util.UUID;

public interface DocumentChunkRepository {

    void replaceAll(UUID documentId, List<DocumentChunk> chunks);

    DocumentChunkPage findPage(UUID documentId, ChunkType type, int page, int size);

    List<DocumentChunk> findAll(UUID documentId, ChunkType type);
}
