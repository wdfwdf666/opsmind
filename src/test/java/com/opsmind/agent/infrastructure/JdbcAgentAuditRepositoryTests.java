package com.opsmind.agent.infrastructure;

import com.opsmind.agent.domain.AgentAuditRepository;
import com.opsmind.agent.domain.AgentRun;
import com.opsmind.agent.domain.AgentRunStatus;
import com.opsmind.agent.domain.AgentStep;
import com.opsmind.agent.domain.AgentStepType;
import com.opsmind.agent.domain.AgentToolRisk;
import com.opsmind.rag.application.RagAnswerResult;
import com.opsmind.rag.application.RagCitation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JdbcAgentAuditRepositoryTests {

    @Autowired
    private AgentAuditRepository repository;

    @Test
    void shouldPersistCompletedRunStepsAndCitations() {
        UUID runId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-10-13T05:00:00Z");
        repository.create(new AgentRun(
                runId, "自动选择文档", null, AgentRunStatus.RUNNING,
                null, null, null, null, List.of(), null, null, createdAt, createdAt, null));
        repository.appendStep(new AgentStep(
                UUID.randomUUID(), runId, 1, AgentStepType.RECEIVED,
                null, null, true, "{\"scope\":\"ALL_DOCUMENTS\"}", createdAt));
        RagCitation citation = new RagCitation(
                "S1", UUID.randomUUID(), UUID.randomUUID(), documentId,
                "manual.md", "手册 > 告警", "检查连接池", 0.8);
        RagAnswerResult answer = new RagAnswerResult(
                "自动选择文档", null, "retrieval", "generation", false,
                null, 0.8, "检查连接池 [S1]", List.of(citation), 10, 4, 14);
        Instant completedAt = createdAt.plusSeconds(2);

        repository.complete(runId, answer, completedAt);

        AgentRun stored = repository.findRun(runId).orElseThrow();
        assertThat(stored.status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(stored.documentId()).isNull();
        assertThat(stored.answer()).isEqualTo("检查连接池 [S1]");
        assertThat(stored.citations()).containsExactly(citation);
        assertThat(repository.findSteps(runId)).singleElement()
                .satisfies(step -> {
                    assertThat(step.sequence()).isEqualTo(1);
                    assertThat(step.detailJson()).contains("ALL_DOCUMENTS");
                });
    }

    @Test
    void shouldPersistFailedRunWithoutLeakingProviderDetails() {
        UUID runId = UUID.randomUUID();
        Instant now = Instant.parse("2026-10-13T05:10:00Z");
        repository.create(new AgentRun(
                runId, "失败请求", null, AgentRunStatus.RUNNING,
                null, null, null, null, List.of(), null, null, now, now, null));
        repository.appendStep(new AgentStep(
                UUID.randomUUID(), runId, 1, AgentStepType.TOOL_CALL,
                "knowledge_search", AgentToolRisk.READ_ONLY, false,
                "{\"errorCode\":\"AGENT_TOOL_EXECUTION_FAILED\"}", now));

        repository.fail(runId, "AGENT_TOOL_EXECUTION_FAILED", "Agent execution failed", now.plusSeconds(1));

        AgentRun stored = repository.findRun(runId).orElseThrow();
        assertThat(stored.status()).isEqualTo(AgentRunStatus.FAILED);
        assertThat(stored.errorMessage()).isEqualTo("Agent execution failed");
        assertThat(stored.citations()).isEmpty();
    }
}
