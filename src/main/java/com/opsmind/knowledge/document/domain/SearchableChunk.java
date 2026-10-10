package com.opsmind.knowledge.document.domain;

import java.util.UUID;

public record SearchableChunk(
        UUID chunkId,
        UUID parentId,
        UUID documentId,
        String filename,
        String headingPath,
        String childText,
        String parentText) {
}
