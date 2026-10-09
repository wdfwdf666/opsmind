package com.opsmind.knowledge.document.domain;

import java.util.List;
import java.util.UUID;

public record SemanticSearchResult(
        String query,
        String model,
        UUID documentId,
        int candidateCount,
        List<SemanticSearchHit> hits) {
}
