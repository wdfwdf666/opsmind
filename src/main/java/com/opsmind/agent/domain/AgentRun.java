package com.opsmind.agent.domain;

import com.opsmind.rag.application.RagCitation;
import com.opsmind.rag.application.RagRefusalReason;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AgentRun(
        UUID id,
        String query,
        UUID documentId,
        AgentRunStatus status,
        String answer,
        Boolean refused,
        RagRefusalReason refusalReason,
        Double topEvidenceScore,
        List<RagCitation> citations,
        String errorCode,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt) {
}
