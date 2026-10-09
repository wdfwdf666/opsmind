package com.opsmind.knowledge.document.api;

import com.opsmind.knowledge.document.application.DocumentUploadResult;
import com.opsmind.knowledge.document.application.DocumentIngestionResult;
import com.opsmind.knowledge.document.domain.DocumentStatus;
import com.opsmind.knowledge.document.domain.IngestionTaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Uploaded document and ingestion task status")
public record DocumentResponse(
        @Schema(format = "uuid") UUID documentId,
        @Schema(format = "uuid") UUID taskId,
        @Schema(example = "incident-runbook.pdf") String filename,
        @Schema(example = "application/pdf") String mediaType,
        @Schema(description = "File size in bytes", example = "204800") long size,
        @Schema(description = "SHA-256 checksum", example = "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9") String sha256,
        DocumentStatus documentStatus,
        IngestionTaskStatus taskStatus,
        @Schema(description = "Processing error, present only when the task failed") String taskError,
        @Schema(description = "Whether an existing document with identical content was returned") boolean duplicate,
        Instant createdAt) {

    public static DocumentResponse from(DocumentUploadResult result) {
        return new DocumentResponse(
                result.document().id(),
                result.task().id(),
                result.document().originalFilename(),
                result.document().mediaType(),
                result.document().size(),
                result.document().sha256(),
                result.document().status(),
                result.task().status(),
                result.task().errorMessage(),
                result.duplicate(),
                result.document().createdAt());
    }

    public static DocumentResponse from(DocumentIngestionResult result) {
        return new DocumentResponse(
                result.document().id(),
                result.task().id(),
                result.document().originalFilename(),
                result.document().mediaType(),
                result.document().size(),
                result.document().sha256(),
                result.document().status(),
                result.task().status(),
                result.task().errorMessage(),
                false,
                result.document().createdAt());
    }
}

