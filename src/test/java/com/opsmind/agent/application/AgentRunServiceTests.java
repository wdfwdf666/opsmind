package com.opsmind.agent.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsmind.agent.domain.AgentAuditRepository;
import com.opsmind.agent.domain.AgentException;
import com.opsmind.agent.domain.AgentRun;
import com.opsmind.agent.domain.AgentRunStatus;
import com.opsmind.agent.domain.AgentStep;
import com.opsmind.agent.domain.AgentStepType;
import com.opsmind.agent.domain.AgentToolRisk;
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
        FakeAuditRepository repository = new FakeAuditRepository();
        AgentTool tool = new StubTool(AgentToolRisk.READ_ONLY, new RagAnswerResult(
                "检查什么", null, "retrieval", "generation", false, null, 0.8,
                "检查连接池 [S1]", List.of(), 10, 4, 14), null);
        AgentRunService service = service(repository, tool);

        var view = service.start("  检查什么  ", null);

        assertThat(view.run().status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(view.run().query()).isEqualTo("检查什么");
        assertThat(view.run().answer()).isEqualTo("检查连接池 [S1]");
        assertThat(view.steps()).extracting(AgentStep::sequence).containsExactly(1, 2, 3, 4);
        assertThat(view.steps()).extracting(AgentStep::type).containsExactly(
                AgentStepType.RECEIVED, AgentStepType.PLANNED, AgentStepType.TOOL_CALL, AgentStepType.RESPONDED);
        assertThat(view.steps().get(2).toolRisk()).isEqualTo(AgentToolRisk.READ_ONLY);
        assertThat(view.steps().get(2).detailJson()).contains("topEvidenceScore", "citationCount");
    }

    @Test
    void shouldBlockWriteToolAndPersistFailedAuditWithoutExecutingIt() {
        FakeAuditRepository repository = new FakeAuditRepository();
        StubTool tool = new StubTool(AgentToolRisk.WRITE_REQUIRES_APPROVAL, null, null);
        AgentRunService service = service(repository, tool);

        assertThatThrownBy(() -> service.start("创建工单", null))
                .isInstanceOf(AgentException.class)
                .satisfies(exception -> assertThat(((AgentException) exception).code()).isEqualTo("AGENT_RUN_FAILED"));

        assertThat(tool.executed).isFalse();
        assertThat(repository.runs.values()).singleElement()
                .satisfies(run -> {
                    assertThat(run.status()).isEqualTo(AgentRunStatus.FAILED);
                    assertThat(run.errorCode()).isEqualTo("AGENT_TOOL_APPROVAL_REQUIRED");
                });
        assertThat(repository.steps.values()).singleElement()
                .satisfies(steps -> assertThat(steps).extracting(AgentStep::success)
                        .containsExactly(true, true, false));
    }

    @Test
    void shouldRejectInvalidQueryBeforeCreatingAuditRun() {
        FakeAuditRepository repository = new FakeAuditRepository();
        AgentRunService service = service(repository, new StubTool(AgentToolRisk.READ_ONLY, null, null));

        assertThatThrownBy(() -> service.start("   ", null))
                .isInstanceOf(AgentException.class)
                .satisfies(exception -> assertThat(((AgentException) exception).code())
                        .isEqualTo("AGENT_QUERY_REQUIRED"));
        assertThat(repository.runs).isEmpty();
    }

    private AgentRunService service(FakeAuditRepository repository, AgentTool tool) {
        return new AgentRunService(
                repository, tool, new AgentToolPolicy(), Clock.fixed(NOW, ZoneOffset.UTC),
                UUID::randomUUID, new ObjectMapper());
    }

    private static final class StubTool implements AgentTool {

        private final AgentToolRisk risk;
        private final RagAnswerResult result;
        private final RuntimeException failure;
        private boolean executed;

        private StubTool(AgentToolRisk risk, RagAnswerResult result, RuntimeException failure) {
            this.risk = risk;
            this.result = result;
            this.failure = failure;
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
            if (failure != null) {
                throw failure;
            }
            return result;
        }
    }

    private static final class FakeAuditRepository implements AgentAuditRepository {

        private final Map<UUID, AgentRun> runs = new LinkedHashMap<>();
        private final Map<UUID, List<AgentStep>> steps = new LinkedHashMap<>();

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
            runs.put(runId, new AgentRun(
                    old.id(), old.query(), old.documentId(), AgentRunStatus.COMPLETED,
                    answer.answer(), answer.refused(), answer.refusalReason(), answer.topEvidenceScore(),
                    answer.citations(), null, null, old.createdAt(), completedAt, completedAt));
        }

        @Override
        public void fail(UUID runId, String errorCode, String errorMessage, Instant completedAt) {
            AgentRun old = runs.get(runId);
            runs.put(runId, new AgentRun(
                    old.id(), old.query(), old.documentId(), AgentRunStatus.FAILED,
                    null, null, null, null, List.of(), errorCode, errorMessage,
                    old.createdAt(), completedAt, completedAt));
        }

        @Override
        public Optional<AgentRun> findRun(UUID runId) {
            return Optional.ofNullable(runs.get(runId));
        }

        @Override
        public List<AgentStep> findSteps(UUID runId) {
            return List.copyOf(steps.getOrDefault(runId, List.of()));
        }
    }
}
