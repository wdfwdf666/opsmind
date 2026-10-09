package com.opsmind.knowledge.document.api;

import com.opsmind.knowledge.document.domain.DocumentStatus;
import com.opsmind.knowledge.document.domain.DocumentSummary;
import com.opsmind.knowledge.document.domain.IngestionTaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Document list item with ingestion status")
public record DocumentListItemResponse(
        @Schema(format = "uuid") UUID documentId,
        @Schema(format = "uuid", nullable = true) UUID taskId,
        String filename,
        String mediaType,
        long size,
        String sha256,
        DocumentStatus documentStatus,
        IngestionTaskStatus taskStatus,
        String taskError,
        Instant createdAt) {

    public static DocumentListItemResponse from(DocumentSummary summary) {
        var document = summary.document();
        return new DocumentListItemResponse(
                document.id(), summary.taskId(), document.originalFilename(), document.mediaType(), document.size(),
                document.sha256(), document.status(), summary.taskStatus(), summary.taskError(), document.createdAt());
    }
}
