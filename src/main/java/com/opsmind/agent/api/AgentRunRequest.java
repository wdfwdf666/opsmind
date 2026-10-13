package com.opsmind.agent.api;

import com.opsmind.agent.domain.AgentTaskType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record AgentRunRequest(
        @Schema(example = "数据库连接池告警应该检查什么？") String query,
        @Schema(
                format = "uuid",
                nullable = true,
                description = "Optional debug scope. Omit it to let RAG select documents automatically.")
        UUID documentId,
        @Schema(
                nullable = true,
                description = "Optional debug override. Omit it to let the configured Planner select an allowed task type.",
                example = "KNOWLEDGE_QA")
        AgentTaskType taskType) {
}
