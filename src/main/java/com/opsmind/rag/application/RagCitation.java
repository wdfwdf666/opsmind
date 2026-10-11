package com.opsmind.rag.application;

import java.util.UUID;

public record RagCitation(
        String sourceId,
        UUID chunkId,
        UUID parentId,
        UUID documentId,
        String filename,
        String headingPath,
        String excerpt,
        double score) {

    static RagCitation from(RagEvidence evidence) {
        return new RagCitation(
                evidence.sourceId(), evidence.chunkId(), evidence.parentId(), evidence.documentId(),
                evidence.filename(), evidence.headingPath(), evidence.excerpt(), evidence.score());
    }
}
