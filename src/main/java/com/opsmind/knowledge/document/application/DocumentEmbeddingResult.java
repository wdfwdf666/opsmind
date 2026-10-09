package com.opsmind.knowledge.document.application;

import java.util.UUID;

public record DocumentEmbeddingResult(
        UUID documentId,
        String model,
        int dimension,
        int embeddedChunkCount) {
}
