package com.opsmind.evaluation.application;

import com.opsmind.agent.application.AgentPlan;
import com.opsmind.agent.application.AgentPlanner;
import com.opsmind.agent.domain.AgentTaskType;
import com.opsmind.agent.domain.AgentTaskTypeSource;
import com.opsmind.evaluation.domain.AgentRoutingEvaluationCase;
import com.opsmind.evaluation.domain.AgentRoutingEvaluationDataset;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentRoutingEvaluationServiceTests {

    @Test
    void shouldCalculateConfusionMacroDangerAndLatencyMetrics() {
        AgentRoutingEvaluationDatasetLoader loader = mock(AgentRoutingEvaluationDatasetLoader.class);
        when(loader.load(null)).thenReturn(new AgentRoutingEvaluationDataset(
                "routing-v1", "test", List.of(
                new AgentRoutingEvaluationCase("qa-ok", "QA", "qa-ok", AgentTaskType.KNOWLEDGE_QA),
                new AgentRoutingEvaluationCase("qa-danger", "NEGATION", "qa-danger", AgentTaskType.KNOWLEDGE_QA),
                new AgentRoutingEvaluationCase("ticket-ok", "WRITE", "ticket-ok", AgentTaskType.CREATE_TICKET),
                new AgentRoutingEvaluationCase("ticket-miss", "INDIRECT", "ticket-miss", AgentTaskType.CREATE_TICKET))));
        AgentPlanner planner = query -> plan(switch (query) {
            case "qa-ok", "ticket-miss" -> AgentTaskType.KNOWLEDGE_QA;
            default -> AgentTaskType.CREATE_TICKET;
        });
        long[] times = {0, 1_000_000, 2_000_000, 4_000_000, 5_000_000, 8_000_000, 9_000_000, 13_000_000};
        AtomicInteger timeIndex = new AtomicInteger();
        var service = new AgentRoutingEvaluationService(loader, planner, () -> times[timeIndex.getAndIncrement()]);

        var report = service.run(null);

        assertThat(report.caseCount()).isEqualTo(4);
        assertThat(report.correctCount()).isEqualTo(2);
        assertThat(report.accuracy()).isEqualTo(0.5);
        assertThat(report.macroPrecision()).isEqualTo(0.5);
        assertThat(report.macroRecall()).isEqualTo(0.5);
        assertThat(report.macroF1()).isEqualTo(0.5);
        assertThat(report.dangerousWriteMisrouteCount()).isOne();
        assertThat(report.dangerousWriteMisrouteRate()).isEqualTo(0.5);
        assertThat(report.p50LatencyMs()).isEqualTo(2.0);
        assertThat(report.p95LatencyMs()).isEqualTo(4.0);
        assertThat(report.classMetrics()).hasSize(2);
        assertThat(report.cases()).filteredOn(item -> !item.correct()).hasSize(2);
    }

    @Test
    void shouldExposePlannerProviderModelAndReason() {
        AgentRoutingEvaluationDatasetLoader loader = mock(AgentRoutingEvaluationDatasetLoader.class);
        when(loader.load("routing-v1")).thenReturn(new AgentRoutingEvaluationDataset(
                "routing-v1", "test", List.of(
                new AgentRoutingEvaluationCase("one", "QA", "query", AgentTaskType.KNOWLEDGE_QA))));
        AgentPlanner planner = query -> plan(AgentTaskType.KNOWLEDGE_QA);
        var service = new AgentRoutingEvaluationService(loader, planner, () -> 1);

        var report = service.run("routing-v1");

        assertThat(report.plannerProvider()).isEqualTo("test");
        assertThat(report.plannerModel()).isEqualTo("test-planner-v1");
        assertThat(report.cases().getFirst().plannerReason()).isEqualTo("test reason");
    }

    private AgentPlan plan(AgentTaskType taskType) {
        return new AgentPlan(
                taskType, AgentTaskTypeSource.PLANNER, "test", "test-planner-v1", "test reason");
    }
}
