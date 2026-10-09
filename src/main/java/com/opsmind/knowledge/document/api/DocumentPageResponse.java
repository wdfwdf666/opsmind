package com.opsmind.knowledge.document.api;

import com.opsmind.knowledge.document.domain.DocumentPage;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Page of documents ordered by creation time descending")
public record DocumentPageResponse(
        List<DocumentListItemResponse> items,
        int page,
        int size,
        long totalElements,
        long totalPages) {

    public static DocumentPageResponse from(DocumentPage result) {
        return new DocumentPageResponse(
                result.items().stream().map(DocumentListItemResponse::from).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages());
    }
}
