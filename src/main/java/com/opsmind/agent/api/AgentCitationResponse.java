package com.opsmind.agent.api;

import com.opsmind.rag.application.RagCitation;

import java.util.UUID;

public record AgentCitationResponse(
        String sourceId,
        UUID chunkId,
        UUID parentId,
        UUID documentId,
        String filename,
        String headingPath,
        String excerpt,
        double score) {

    static AgentCitationResponse from(RagCitation citation) {
        return new AgentCitationResponse(
                citation.sourceId(), citation.chunkId(), citation.parentId(), citation.documentId(),
                citation.filename(), citation.headingPath(), citation.excerpt(), citation.score());
    }
}
