package com.opsmind.knowledge.document.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IngestionTaskRepository {

    IngestionTask getOrCreate(UUID documentId, Instant now);

    Optional<IngestionTask> findByDocumentId(UUID documentId);
}

