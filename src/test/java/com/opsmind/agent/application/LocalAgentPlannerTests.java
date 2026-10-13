package com.opsmind.agent.application;

import com.opsmind.agent.config.AgentPlannerProperties;
import com.opsmind.agent.domain.AgentTaskType;
import com.opsmind.agent.domain.AgentTaskTypeSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocalAgentPlannerTests {

    private final LocalAgentPlanner planner = new LocalAgentPlanner(
            new AgentPlannerProperties(
                    "local", "local-intent-router-v2", "", "", 256,
                    2000, 5000, 1, false, "local-intent-router-v2"));

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
        assertThat(plan.model()).isEqualTo("local-intent-router-v2");
        assertThat(plan.reason()).isNotBlank();
    }

    @Test
    void shouldKeepNegatedWritesAndWorkflowQuestionsReadOnly() {
        assertThat(planner.plan("不要创建工单，只告诉我连接池告警怎么处理").taskType())
                .isEqualTo(AgentTaskType.KNOWLEDGE_QA);
        assertThat(planner.plan("创建工单前需要哪些审批？").taskType())
                .isEqualTo(AgentTaskType.KNOWLEDGE_QA);
        assertThat(planner.plan("如何创建工单？").taskType())
                .isEqualTo(AgentTaskType.KNOWLEDGE_QA);
    }

    @Test
    void shouldRecognizeIndirectTicketCreationPhrases() {
        assertThat(planner.plan("帮我登记一个工单跟进这个问题").taskType())
                .isEqualTo(AgentTaskType.CREATE_TICKET);
        assertThat(planner.plan("麻烦开一张工单").taskType())
                .isEqualTo(AgentTaskType.CREATE_TICKET);
    }
}
