package com.opsmind.knowledge.document.domain;

import java.time.Instant;
import java.util.UUID;

public record KnowledgeDocument(
        UUID id,
        String originalFilename,
        String mediaType,
        long size,
        String sha256,
        String storageKey,
        DocumentStatus status,
        Instant createdAt) {
}

