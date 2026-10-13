package com.opsmind.agent.domain;

import com.opsmind.rag.application.RagAnswerResult;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentAuditRepository {

    void create(AgentRun run);

    void appendStep(AgentStep step);

    void complete(UUID runId, RagAnswerResult answer, Instant completedAt);

    void fail(UUID runId, String errorCode, String errorMessage, Instant completedAt);

    Optional<AgentRun> findRun(UUID runId);

    List<AgentStep> findSteps(UUID runId);
}
