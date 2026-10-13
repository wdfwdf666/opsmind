package com.opsmind.agent.application;

import com.opsmind.agent.config.AgentPlannerProperties;
import com.opsmind.agent.domain.AgentTaskType;
import com.opsmind.agent.domain.AgentTaskTypeSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "opsmind.agent.planner", name = "provider", havingValue = "local")
public class LocalAgentPlanner implements AgentPlanner {

    private static final List<String> CREATE_VERBS = List.of("创建", "新建", "生成", "提交");
    private final AgentPlannerProperties properties;

    public LocalAgentPlanner(AgentPlannerProperties properties) {
        this.properties = properties;
    }

    @Override
    public AgentPlan plan(String query) {
        boolean createTicket = query.contains("工单") && CREATE_VERBS.stream().anyMatch(query::contains);
        AgentTaskType taskType = createTicket ? AgentTaskType.CREATE_TICKET : AgentTaskType.KNOWLEDGE_QA;
        String reason = createTicket ? "检测到明确的工单创建意图" : "未检测到写操作意图，按知识问答处理";
        return new AgentPlan(taskType, AgentTaskTypeSource.PLANNER, properties.provider(), properties.model(), reason);
    }
}
