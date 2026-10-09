package com.opsmind.knowledge.document.domain;

import java.time.Instant;
import java.util.UUID;

public record IngestionTask(
        UUID id,
        UUID documentId,
        IngestionTaskStatus status,
        Instant createdAt,
        Instant updatedAt,
        String errorMessage) {

    public static IngestionTask pending(UUID documentId, Instant now) {
        return new IngestionTask(UUID.randomUUID(), documentId, IngestionTaskStatus.PENDING, now, now, null);
    }
}

