package com.opsmind.knowledge.document.application;

import java.util.UUID;

public record DocumentChunkingResult(
        UUID documentId,
        int parentCount,
        int childCount) {

    public int totalCount() {
        return parentCount + childCount;
    }
}
