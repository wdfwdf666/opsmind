package com.opsmind.knowledge.document.api;

import com.opsmind.knowledge.document.domain.DocumentContent;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Parsed and cleaned document content")
public record DocumentContentResponse(
        @Schema(format = "uuid") UUID documentId,
        String title,
        @Schema(example = "text/plain; charset=UTF-8") String detectedMediaType,
        String text,
        int characterCount,
        Instant parsedAt) {

    public static DocumentContentResponse from(DocumentContent content) {
        return new DocumentContentResponse(
                content.documentId(), content.title(), content.detectedMediaType(), content.text(),
                content.characterCount(), content.parsedAt());
    }
}
