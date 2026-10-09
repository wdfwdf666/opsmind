package com.opsmind.knowledge.document.api;

import com.opsmind.knowledge.document.domain.ChunkType;
import com.opsmind.knowledge.document.domain.DocumentChunkPage;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Page of parent or child chunks")
public record DocumentChunkPageResponse(
        List<DocumentChunkResponse> items,
        ChunkType type,
        int page,
        int size,
        long totalElements,
        long totalPages) {

    public static DocumentChunkPageResponse from(DocumentChunkPage result) {
        return new DocumentChunkPageResponse(
                result.items().stream().map(DocumentChunkResponse::from).toList(),
                result.type(), result.page(), result.size(), result.totalElements(), result.totalPages());
    }
}
