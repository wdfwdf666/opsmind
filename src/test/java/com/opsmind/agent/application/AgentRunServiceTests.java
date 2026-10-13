package com.opsmind.agent.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsmind.agent.domain.AgentApproval;
import com.opsmind.agent.domain.AgentApprovalStatus;
import com.opsmind.agent.domain.AgentAuditRepository;
import com.opsmind.agent.domain.AgentException;
import com.opsmind.agent.domain.AgentRun;
import com.opsmind.agent.domain.AgentRunStatus;
import com.opsmind.agent.domain.AgentStep;
import com.opsmind.agent.domain.AgentStepType;
import com.opsmind.agent.domain.AgentTaskType;
import com.opsmind.agent.domain.AgentToolRisk;
import com.opsmind.agent.domain.SupportTicket;
import com.opsmind.agent.domain.SupportTicketRepository;
import com.opsmind.agent.domain.TicketStatus;
import com.opsmind.rag.application.RagAnswerResult;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentRunServiceTests {

    private static final Instant NOW = Instant.parse("2026-10-13T04:00:00Z");

    @Test
    void shouldExecuteReadOnlyKnowledgeToolAndPersistOrderedAuditTrail() {
        Fixture fixture = fixture(AgentToolRisk.READ_ONLY);

        var view = fixture.service.start("  检查什么  ", null);

        assertThat(view.run().taskType()).isEqualTo(AgentTaskType.KNOWLEDGE_QA);
        assertThat(view.run().status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(view.run().query()).isEqualTo("检查什么");
        assertThat(view.run().answer()).isEqualTo("检查连接池 [S1]");
        assertThat(view.steps()).extracting(AgentStep::sequence).containsExactly(1, 2, 3, 4);
        assertThat(view.steps()).extracting(AgentStep::type).containsExactly(
                AgentStepType.RECEIVED, AgentStepType.PLANNED,
                AgentStepType.TOOL_CALL, AgentStepType.RESPONDED);
        assertThat(view.approval()).isNull();
        assertThat(view.ticket()).isNull();
    }

    @Test
    void shouldCreateTicketDraftAndWaitWithoutExecutingWriteTool() {
        Fixture fixture = fixture(AgentToolRisk.READ_ONLY);

        var view = fixture.service.start("数据库连接池持续告警", null, AgentTaskType.CREATE_TICKET);

        assertThat(view.run().status()).isEqualTo(AgentRunStatus.WAITING_APPROVAL);
        assertThat(view.approval().status()).isEqualTo(AgentApprovalStatus.PENDING);
        assertThat(view.approval().draft().title()).isEqualTo("数据库连接池持续告警");
        assertThat(view.approval().draft().description()).contains("用户描述", "知识库建议", "检查连接池");
        assertThat(view.steps()).extracting(AgentStep::type).containsExactly(
                AgentStepType.RECEIVED, AgentStepType.PLANNED,
                AgentStepType.TOOL_CALL, AgentStepType.APPROVAL_REQUESTED);
        assertThat(fixture.ticketTool.executions).isZero();
        assertThat(view.ticket()).isNull();
    }

    @Test
    void shouldCreateExactlyOneTicketAfterApprovalAndMakeRetryIdempotent() {
        Fixture fixture = fixture(AgentToolRisk.READ_ONLY);
        var waiting = fixture.service.start("数据库连接池持续告警", null, AgentTaskType.CREATE_TICKET);

        var completed = fixture.service.decide(waiting.run().id(), AgentApprovalStatus.APPROVED, "同意创建");
        var retried = fixture.service.decide(waiting.run().id(), AgentApprovalStatus.APPROVED, "重复请求");

        assertThat(completed.run().status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(completed.approval().status()).isEqualTo(AgentApprovalStatus.APPROVED);
        assertThat(completed.ticket()).isNotNull();
        assertThat(completed.ticket().status()).isEqualTo(TicketStatus.OPEN);
        assertThat(completed.steps()).extracting(AgentStep::sequence).containsExactly(1, 2, 3, 4, 5, 6, 7);
        assertThat(fixture.ticketTool.executions).isOne();
        assertThat(retried.ticket().id()).isEqualTo(completed.ticket().id());
        assertThat(fixture.ticketRepository.tickets).hasSize(1);
        assertThatThrownBy(() -> fixture.service.decide(
                waiting.run().id(), AgentApprovalStatus.REJECTED, "改变决定"))
                .isInstanceOf(AgentException.class)
                .satisfies(exception -> assertThat(((AgentException) exception).code())
                        .isEqualTo("AGENT_APPROVAL_ALREADY_DECIDED"));
    }

    @Test
    void shouldRejectTicketCreationAndMakeRepeatedRejectionIdempotent() {
        Fixture fixture = fixture(AgentToolRisk.READ_ONLY);
        var waiting = fixture.service.start("数据库连接池持续告警", null, AgentTaskType.CREATE_TICKET);

        var rejected = fixture.service.decide(waiting.run().id(), AgentApprovalStatus.REJECTED, "影响范围不足");
        var retried = fixture.service.decide(waiting.run().id(), AgentApprovalStatus.REJECTED, null);

        assertThat(rejected.run().status()).isEqualTo(AgentRunStatus.REJECTED);
        assertThat(rejected.approval().status()).isEqualTo(AgentApprovalStatus.REJECTED);
        assertThat(rejected.approval().decisionReason()).isEqualTo("影响范围不足");
        assertThat(rejected.steps()).extracting(AgentStep::type).endsWith(AgentStepType.REJECTED);
        assertThat(retried.run().status()).isEqualTo(AgentRunStatus.REJECTED);
        assertThat(fixture.ticketTool.executions).isZero();
        assertThat(fixture.ticketRepository.tickets).isEmpty();
    }

    @Test
    void shouldBlockNonReadKnowledgeToolAndPersistFailedAuditWithoutExecutingIt() {
        Fixture fixture = fixture(AgentToolRisk.WRITE_REQUIRES_APPROVAL);

        assertThatThrownBy(() -> fixture.service.start("创建工单", null))
                .isInstanceOf(AgentException.class)
                .satisfies(exception -> assertThat(((AgentException) exception).code()).isEqualTo("AGENT_RUN_FAILED"));

        assertThat(fixture.knowledgeTool.executed).isFalse();
        assertThat(fixture.auditRepository.runs.values()).singleElement()
                .satisfies(run -> {
                    assertThat(run.status()).isEqualTo(AgentRunStatus.FAILED);
                    assertThat(run.errorCode()).isEqualTo("AGENT_TOOL_APPROVAL_REQUIRED");
                });
    }

    @Test
    void shouldRejectInvalidQueryBeforeCreatingAuditRun() {
        Fixture fixture = fixture(AgentToolRisk.READ_ONLY);

        assertThatThrownBy(() -> fixture.service.start("   ", null))
                .isInstanceOf(AgentException.class)
                .satisfies(exception -> assertThat(((AgentException) exception).code())
                        .isEqualTo("AGENT_QUERY_REQUIRED"));
        assertThat(fixture.auditRepository.runs).isEmpty();
    }

    private Fixture fixture(AgentToolRisk knowledgeRisk) {
        FakeAuditRepository auditRepository = new FakeAuditRepository();
        FakeTicketRepository ticketRepository = new FakeTicketRepository();
        StubKnowledgeTool knowledgeTool = new StubKnowledgeTool(knowledgeRisk);
        StubTicketTool ticketTool = new StubTicketTool(ticketRepository);
        AgentRunService service = new AgentRunService(
                auditRepository, ticketRepository, knowledgeTool, ticketTool, new AgentToolPolicy(),
                Clock.fixed(NOW, ZoneOffset.UTC), UUID::randomUUID, new ObjectMapper());
        return new Fixture(service, auditRepository, ticketRepository, knowledgeTool, ticketTool);
    }

    private record Fixture(
            AgentRunService service,
            FakeAuditRepository auditRepository,
            FakeTicketRepository ticketRepository,
            StubKnowledgeTool knowledgeTool,
            StubTicketTool ticketTool) {
    }

    private static final class StubKnowledgeTool implements AgentTool<AgentToolInput, RagAnswerResult> {

        private final AgentToolRisk risk;
        private boolean executed;

        private StubKnowledgeTool(AgentToolRisk risk) {
            this.risk = risk;
        }

        @Override
        public String name() {
            return "knowledge_search";
        }

        @Override
        public AgentToolRisk risk() {
            return risk;
        }

        @Override
        public RagAnswerResult execute(AgentToolInput input) {
            executed = true;
            return new RagAnswerResult(
                    input.query(), input.documentId(), "retrieval", "generation", false,
                    null, 0.8, "检查连接池 [S1]", List.of(), 10, 4, 14);
        }
    }

    private static final class StubTicketTool implements AgentTool<CreateTicketInput, SupportTicket> {

        private final FakeTicketRepository repository;
        private int executions;

        private StubTicketTool(FakeTicketRepository repository) {
            this.repository = repository;
        }

        @Override
        public String name() {
            return "create_ticket";
        }

        @Override
        public AgentToolRisk risk() {
            return AgentToolRisk.WRITE_REQUIRES_APPROVAL;
        }

        @Override
        public SupportTicket execute(CreateTicketInput input) {
            executions++;
            return repository.create(new SupportTicket(
                    UUID.randomUUID(), input.runId(), input.draft().title(), input.draft().description(),
                    input.draft().priority(), TicketStatus.OPEN, NOW));
        }
    }

    private static final class FakeTicketRepository implements SupportTicketRepository {

        private final Map<UUID, SupportTicket> tickets = new LinkedHashMap<>();

        @Override
        public SupportTicket create(SupportTicket ticket) {
            tickets.putIfAbsent(ticket.runId(), ticket);
            return tickets.get(ticket.runId());
        }

        @Override
        public Optional<SupportTicket> findByRunId(UUID runId) {
            return Optional.ofNullable(tickets.get(runId));
        }
    }

    private static final class FakeAuditRepository implements AgentAuditRepository {

        private final Map<UUID, AgentRun> runs = new LinkedHashMap<>();
        private final Map<UUID, List<AgentStep>> steps = new LinkedHashMap<>();
        private final Map<UUID, AgentApproval> approvals = new LinkedHashMap<>();

        @Override
        public void create(AgentRun run) {
            runs.put(run.id(), run);
            steps.put(run.id(), new ArrayList<>());
        }

        @Override
        public void appendStep(AgentStep step) {
            steps.get(step.runId()).add(step);
        }

        @Override
        public void complete(UUID runId, RagAnswerResult answer, Instant completedAt) {
            AgentRun old = runs.get(runId);
            runs.put(runId, copy(
                    old, AgentRunStatus.COMPLETED, answer.answer(), answer.refused(),
                    answer.refusalReason(), answer.topEvidenceScore(), answer.citations(),
                    null, null, completedAt, completedAt));
        }

        @Override
        public void requestApproval(
                UUID runId,
                RagAnswerResult evidence,
                AgentApproval approval,
                AgentStep approvalStep,
                Instant updatedAt) {
            approvals.put(runId, approval);
            appendStep(approvalStep);
            AgentRun old = runs.get(runId);
            runs.put(runId, copy(
                    old, AgentRunStatus.WAITING_APPROVAL, evidence.answer(), evidence.refused(),
                    evidence.refusalReason(), evidence.topEvidenceScore(), evidence.citations(),
                    null, null, updatedAt, null));
        }

        @Override
        public boolean decideApproval(
                UUID runId, AgentApprovalStatus decision, String decisionReason, Instant decidedAt) {
            AgentApproval old = approvals.get(runId);
            if (old == null || old.status() != AgentApprovalStatus.PENDING) {
                return false;
            }
            approvals.put(runId, new AgentApproval(
                    old.id(), old.runId(), old.toolName(), old.toolRisk(), decision,
                    old.draft(), decisionReason, old.createdAt(), decidedAt));
            return true;
        }

        @Override
        public void completeWrite(UUID runId, String answer, Instant completedAt) {
            AgentRun old = runs.get(runId);
            runs.put(runId, copy(
                    old, AgentRunStatus.COMPLETED, answer, false, null,
                    old.topEvidenceScore(), old.citations(), null, null, completedAt, completedAt));
        }

        @Override
        public void reject(UUID runId, String answer, Instant completedAt) {
            AgentRun old = runs.get(runId);
            runs.put(runId, copy(
                    old, AgentRunStatus.REJECTED, answer, false, null,
                    old.topEvidenceScore(), old.citations(), null, null, completedAt, completedAt));
        }

        @Override
        public void fail(UUID runId, String errorCode, String errorMessage, Instant completedAt) {
            AgentRun old = runs.get(runId);
            runs.put(runId, copy(
                    old, AgentRunStatus.FAILED, null, null, null, null, List.of(),
                    errorCode, errorMessage, completedAt, completedAt));
        }

        @Override
        public Optional<AgentRun> findRun(UUID runId) {
            return Optional.ofNullable(runs.get(runId));
        }

        @Override
        public Optional<AgentApproval> findApproval(UUID runId) {
            return Optional.ofNullable(approvals.get(runId));
        }

        @Override
        public List<AgentStep> findSteps(UUID runId) {
            return List.copyOf(steps.getOrDefault(runId, List.of()));
        }

        private AgentRun copy(
                AgentRun old,
                AgentRunStatus status,
                String answer,
                Boolean refused,
                com.opsmind.rag.application.RagRefusalReason refusalReason,
                Double score,
                List<com.opsmind.rag.application.RagCitation> citations,
                String errorCode,
                String errorMessage,
                Instant updatedAt,
                Instant completedAt) {
            return new AgentRun(
                    old.id(), old.query(), old.documentId(), old.taskType(), status,
                    answer, refused, refusalReason, score, citations, errorCode, errorMessage,
                    old.createdAt(), updatedAt, completedAt);
        }
    }
}
