package com.opsmind.agent.infrastructure;

import com.opsmind.agent.domain.AgentAuditRepository;
import com.opsmind.agent.domain.AgentApproval;
import com.opsmind.agent.domain.AgentApprovalStatus;
import com.opsmind.agent.domain.AgentRun;
import com.opsmind.agent.domain.AgentRunStatus;
import com.opsmind.agent.domain.AgentStep;
import com.opsmind.agent.domain.AgentStepType;
import com.opsmind.agent.domain.AgentTaskType;
import com.opsmind.agent.domain.AgentTaskTypeSource;
import com.opsmind.agent.domain.AgentToolRisk;
import com.opsmind.agent.domain.TicketDraft;
import com.opsmind.agent.domain.TicketPriority;
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
                runId, "自动选择文档", null, AgentTaskType.KNOWLEDGE_QA,
                AgentTaskTypeSource.PLANNER, "local", "local-intent-router-v1", "知识问答",
                AgentRunStatus.RUNNING,
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
        assertThat(stored.taskTypeSource()).isEqualTo(AgentTaskTypeSource.PLANNER);
        assertThat(stored.plannerModel()).isEqualTo("local-intent-router-v1");
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
                runId, "失败请求", null, AgentTaskType.KNOWLEDGE_QA,
                AgentTaskTypeSource.EXPLICIT, "request", "explicit-override", "显式指定",
                AgentRunStatus.RUNNING,
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

    @Test
    void shouldPersistPendingApprovalAndDecision() {
        UUID runId = UUID.randomUUID();
        Instant now = Instant.parse("2026-10-13T05:20:00Z");
        repository.create(new AgentRun(
                runId, "创建告警工单", null, AgentTaskType.CREATE_TICKET,
                AgentTaskTypeSource.PLANNER, "qwen", "qwen3.7-plus", "创建工单意图",
                AgentRunStatus.RUNNING,
                null, null, null, null, List.of(), null, null, now, now, null));
        RagAnswerResult evidence = new RagAnswerResult(
                "创建告警工单", null, "retrieval", "generation", false,
                null, 0.7, "先检查连接池 [S1]", List.of(), 1, 1, 2);
        AgentApproval approval = new AgentApproval(
                UUID.randomUUID(), runId, "create_ticket", AgentToolRisk.WRITE_REQUIRES_APPROVAL,
                AgentApprovalStatus.PENDING,
                new TicketDraft("连接池告警", "用户描述与知识库建议", TicketPriority.P2),
                null, now, null);

        AgentStep approvalStep = new AgentStep(
                UUID.randomUUID(), runId, 1, AgentStepType.APPROVAL_REQUESTED,
                "create_ticket", AgentToolRisk.WRITE_REQUIRES_APPROVAL, true,
                "{\"approval\":\"requested\"}", now.plusSeconds(1));
        repository.requestApproval(runId, evidence, approval, approvalStep, now.plusSeconds(1));
        boolean decided = repository.decideApproval(
                runId, AgentApprovalStatus.APPROVED, "确认创建", now.plusSeconds(2));

        assertThat(decided).isTrue();
        assertThat(repository.findRun(runId).orElseThrow().status()).isEqualTo(AgentRunStatus.WAITING_APPROVAL);
        assertThat(repository.findApproval(runId).orElseThrow())
                .satisfies(stored -> {
                    assertThat(stored.status()).isEqualTo(AgentApprovalStatus.APPROVED);
                    assertThat(stored.draft()).isEqualTo(approval.draft());
                    assertThat(stored.decisionReason()).isEqualTo("确认创建");
                });
    }
}
