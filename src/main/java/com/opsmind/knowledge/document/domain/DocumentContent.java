package com.opsmind.knowledge.document.domain;

import java.time.Instant;
import java.util.UUID;

public record DocumentContent(
        UUID documentId,
        String title,
        String detectedMediaType,
        String text,
        int characterCount,
        Instant parsedAt) {
}
