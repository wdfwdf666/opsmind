package com.opsmind.agent.application;

import com.opsmind.agent.config.AgentPlannerProperties;
import com.opsmind.agent.domain.AgentTaskType;
import com.opsmind.agent.domain.AgentTaskTypeSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(prefix = "opsmind.agent.planner", name = "provider", havingValue = "local")
public class LocalAgentPlanner implements AgentPlanner {

    private static final List<String> CREATE_VERBS = List.of("创建", "新建", "生成", "提交", "登记", "建立");
    private static final Pattern NEGATED_WRITE = Pattern.compile(
            "(?:不要|无需|不需要|别|暂不|先不).{0,10}(?:创建|新建|生成|提交|登记|建立|开).{0,10}工单");
    private static final Pattern HOW_TO_CREATE = Pattern.compile(
            "(?:如何|怎么|怎样).{0,10}(?:创建|新建|生成|提交|登记|建立|开).{0,10}工单");
    private static final Pattern WORKFLOW_QUESTION = Pattern.compile(
            "(?:创建|新建|生成|提交|登记|建立|开).{0,6}工单.{0,16}(?:流程|步骤|条件|标准|规定|审批|需要哪些|有什么要求)");
    private static final Pattern TICKET_FIRST_WORKFLOW = Pattern.compile(
            "工单.{0,6}(?:创建|新建|登记).{0,10}(?:流程|步骤|条件|标准|规定|审批)");
    private final AgentPlannerProperties properties;

    public LocalAgentPlanner(AgentPlannerProperties properties) {
        this.properties = properties;
    }

    @Override
    public AgentPlan plan(String query) {
        String normalized = normalize(query);
        if (isNegatedWrite(normalized)) {
            return result(AgentTaskType.KNOWLEDGE_QA, "检测到对工单写操作的否定表达");
        }
        if (isWorkflowQuestion(normalized)) {
            return result(AgentTaskType.KNOWLEDGE_QA, "检测到工单流程咨询而非执行命令");
        }
        boolean createTicket = normalized.contains("工单")
                && (CREATE_VERBS.stream().anyMatch(normalized::contains)
                || normalized.contains("开个工单")
                || normalized.contains("开一张工单")
                || normalized.contains("建个工单")
                || normalized.contains("建一张工单"));
        return createTicket
                ? result(AgentTaskType.CREATE_TICKET, "检测到明确的工单创建意图")
                : result(AgentTaskType.KNOWLEDGE_QA, "未检测到写操作意图，按知识问答处理");
    }

    private boolean isNegatedWrite(String query) {
        return NEGATED_WRITE.matcher(query).find();
    }

    private boolean isWorkflowQuestion(String query) {
        return HOW_TO_CREATE.matcher(query).find()
                || WORKFLOW_QUESTION.matcher(query).find()
                || TICKET_FIRST_WORKFLOW.matcher(query).find();
    }

    private String normalize(String query) {
        return Normalizer.normalize(query, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "");
    }

    private AgentPlan result(AgentTaskType taskType, String reason) {
        return new AgentPlan(taskType, AgentTaskTypeSource.PLANNER, properties.provider(), properties.model(), reason);
    }
}
