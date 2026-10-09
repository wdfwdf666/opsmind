package com.opsmind.knowledge.document.infrastructure;

import com.opsmind.knowledge.document.domain.ChunkType;
import com.opsmind.knowledge.document.domain.DocumentChunk;
import com.opsmind.knowledge.document.domain.DocumentChunkPage;
import com.opsmind.knowledge.document.domain.DocumentChunkRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class InMemoryDocumentChunkRepository implements DocumentChunkRepository {

    private final List<DocumentChunk> chunks = new ArrayList<>();

    @Override
    public synchronized void replaceAll(UUID documentId, List<DocumentChunk> replacements) {
        chunks.removeIf(chunk -> chunk.documentId().equals(documentId));
        chunks.addAll(replacements);
    }

    @Override
    public synchronized DocumentChunkPage findPage(UUID documentId, ChunkType type, int page, int size) {
        List<DocumentChunk> matching = chunks.stream()
                .filter(chunk -> chunk.documentId().equals(documentId) && chunk.type() == type)
                .sorted(Comparator.comparingInt(DocumentChunk::ordinal))
                .toList();
        int fromIndex = (int) Math.min((long) page * size, matching.size());
        int toIndex = Math.min(fromIndex + size, matching.size());
        return new DocumentChunkPage(
                List.copyOf(matching.subList(fromIndex, toIndex)), type, page, size, matching.size());
    }
}
