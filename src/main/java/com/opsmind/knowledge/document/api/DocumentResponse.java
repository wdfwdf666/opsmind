package com.opsmind.knowledge.document.api;

import com.opsmind.knowledge.document.application.DocumentUploadResult;
import com.opsmind.knowledge.document.domain.DocumentStatus;
import com.opsmind.knowledge.document.domain.IngestionTaskStatus;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID documentId,
        UUID taskId,
        String filename,
        String mediaType,
        long size,
        String sha256,
        DocumentStatus documentStatus,
        IngestionTaskStatus taskStatus,
        boolean duplicate,
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
                result.duplicate(),
                result.document().createdAt());
    }
}

