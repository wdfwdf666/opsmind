package com.opsmind.knowledge.document.domain;

import java.util.List;

public record DocumentChunkPage(
        List<DocumentChunk> items,
        ChunkType type,
        int page,
        int size,
        long totalElements) {

    public long totalPages() {
        return totalElements == 0 ? 0 : (totalElements + size - 1) / size;
    }
}
