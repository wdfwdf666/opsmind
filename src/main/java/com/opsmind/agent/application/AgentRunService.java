package com.opsmind.agent.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsmind.agent.domain.AgentAuditRepository;
import com.opsmind.agent.domain.AgentException;
import com.opsmind.agent.domain.AgentRun;
import com.opsmind.agent.domain.AgentRunStatus;
import com.opsmind.agent.domain.AgentRunView;
import com.opsmind.agent.domain.AgentStep;
import com.opsmind.agent.domain.AgentStepType;
import com.opsmind.rag.application.RagAnswerResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class AgentRunService {

    private static final int MAX_QUERY_CHARACTERS = 1000;

    private final AgentAuditRepository auditRepository;
    private final AgentTool knowledgeSearchTool;
    private final AgentToolPolicy toolPolicy;
    private final Clock clock;
    private final Supplier<UUID> idSupplier;
    private final ObjectMapper objectMapper;

    @Autowired
    public AgentRunService(
            AgentAuditRepository auditRepository,
            KnowledgeSearchAgentTool knowledgeSearchTool,
            AgentToolPolicy toolPolicy) {
        this(auditRepository, knowledgeSearchTool, toolPolicy, Clock.systemUTC(), UUID::randomUUID, new ObjectMapper());
    }

    AgentRunService(
            AgentAuditRepository auditRepository,
            AgentTool knowledgeSearchTool,
            AgentToolPolicy toolPolicy,
            Clock clock,
            Supplier<UUID> idSupplier,
            ObjectMapper objectMapper) {
        this.auditRepository = auditRepository;
        this.knowledgeSearchTool = knowledgeSearchTool;
        this.toolPolicy = toolPolicy;
        this.clock = clock;
        this.idSupplier = idSupplier;
        this.objectMapper = objectMapper;
    }

    public AgentRunView start(String rawQuery, UUID documentId) {
        String query = validateQuery(rawQuery);
        UUID runId = idSupplier.get();
        Instant now = clock.instant();
        auditRepository.create(new AgentRun(
                runId, query, documentId, AgentRunStatus.RUNNING, null, null, null, null, List.of(),
                null, null, now, now, null));
        append(runId, 1, AgentStepType.RECEIVED, null, true,
                Map.of("queryCharacters", query.length(), "scope", documentId == null ? "ALL_DOCUMENTS" : "DOCUMENT"));
        append(runId, 2, AgentStepType.PLANNED, knowledgeSearchTool, true,
                Map.of("strategy", "DETERMINISTIC_SINGLE_TOOL", "tool", knowledgeSearchTool.name()));

        RagAnswerResult answer;
        try {
            toolPolicy.requireAutoExecutable(knowledgeSearchTool);
            answer = knowledgeSearchTool.execute(new AgentToolInput(query, documentId));
        } catch (RuntimeException exception) {
            String code = exception instanceof AgentException agentException
                    ? agentException.code()
                    : "AGENT_TOOL_EXECUTION_FAILED";
            append(runId, 3, AgentStepType.TOOL_CALL, knowledgeSearchTool, false,
                    Map.of("errorCode", code));
            auditRepository.fail(runId, code, "Agent execution failed", clock.instant());
            throw new AgentException(
                    "AGENT_RUN_FAILED", "Agent run failed; runId=" + runId, HttpStatus.BAD_GATEWAY);
        }
        append(runId, 3, AgentStepType.TOOL_CALL, knowledgeSearchTool, true,
                Map.of(
                        "refused", answer.refused(),
                        "refusalReason", answer.refusalReason() == null ? "" : answer.refusalReason().name(),
                        "citationCount", answer.citations().size(),
                        "topEvidenceScore", answer.topEvidenceScore() == null ? -1 : answer.topEvidenceScore()));
        append(runId, 4, AgentStepType.RESPONDED, null, true,
                Map.of("answerCharacters", answer.answer().length(), "totalTokens", value(answer.totalTokens())));
        auditRepository.complete(runId, answer, clock.instant());
        return get(runId);
    }

    public AgentRunView get(UUID runId) {
        AgentRun run = auditRepository.findRun(runId)
                .orElseThrow(() -> new AgentException(
                        "AGENT_RUN_NOT_FOUND", "Agent run not found: " + runId, HttpStatus.NOT_FOUND));
        return new AgentRunView(run, auditRepository.findSteps(runId));
    }

    private void append(
            UUID runId,
            int sequence,
            AgentStepType type,
            AgentTool tool,
            boolean success,
            Map<String, ?> detail) {
        auditRepository.appendStep(new AgentStep(
                idSupplier.get(), runId, sequence, type,
                tool == null ? null : tool.name(), tool == null ? null : tool.risk(),
                success, json(detail), clock.instant()));
    }

    private String validateQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            throw new AgentException("AGENT_QUERY_REQUIRED", "Agent query must not be blank", HttpStatus.BAD_REQUEST);
        }
        String query = rawQuery.strip();
        if (query.length() > MAX_QUERY_CHARACTERS) {
            throw new AgentException(
                    "AGENT_QUERY_TOO_LONG",
                    "Agent query must not exceed " + MAX_QUERY_CHARACTERS + " characters",
                    HttpStatus.BAD_REQUEST);
        }
        return query;
    }

    private String json(Map<String, ?> detail) {
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize agent audit detail", exception);
        }
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }
}
