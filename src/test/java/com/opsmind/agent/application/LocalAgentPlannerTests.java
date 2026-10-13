package com.opsmind.agent.application;

import com.opsmind.agent.config.AgentPlannerProperties;
import com.opsmind.agent.domain.AgentTaskType;
import com.opsmind.agent.domain.AgentTaskTypeSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocalAgentPlannerTests {

    private final LocalAgentPlanner planner = new LocalAgentPlanner(
            new AgentPlannerProperties("local", "local-intent-router-v1", "", "", 256));

    @Test
    void shouldSelectTicketOnlyForExplicitCreateIntent() {
        assertThat(planner.plan("数据库告警，请创建 P2 工单").taskType()).isEqualTo(AgentTaskType.CREATE_TICKET);
        assertThat(planner.plan("已有工单应该如何排查？").taskType()).isEqualTo(AgentTaskType.KNOWLEDGE_QA);
    }

    @Test
    void shouldReturnAuditablePlannerMetadata() {
        AgentPlan plan = planner.plan("数据库连接池告警怎么处理？");

        assertThat(plan.source()).isEqualTo(AgentTaskTypeSource.PLANNER);
        assertThat(plan.provider()).isEqualTo("local");
        assertThat(plan.model()).isEqualTo("local-intent-router-v1");
        assertThat(plan.reason()).isNotBlank();
    }
}
