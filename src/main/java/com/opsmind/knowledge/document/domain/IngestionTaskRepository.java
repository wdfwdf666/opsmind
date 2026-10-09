package com.opsmind.knowledge.document.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IngestionTaskRepository {

    IngestionTask getOrCreate(UUID documentId, Instant now);

    Optional<IngestionTask> findByDocumentId(UUID documentId);

    boolean tryMarkRunning(UUID documentId, Instant now, Instant staleBefore);

    void markSucceeded(UUID documentId, Instant now);

    void markFailed(UUID documentId, Instant now, String errorMessage);
}

