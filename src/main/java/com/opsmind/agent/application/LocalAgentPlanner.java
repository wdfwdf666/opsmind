package com.opsmind.agent.application; // 包作用：应用层，编排业务步骤，但不绑定具体数据库或模型厂商。

import com.opsmind.agent.config.AgentPlannerProperties;
import com.opsmind.agent.domain.AgentTaskType;
import com.opsmind.agent.domain.AgentTaskTypeSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component // 注解作用：把普通组件交给 Spring 容器管理。
@ConditionalOnProperty(prefix = "opsmind.agent.planner", name = "provider", havingValue = "local") // 注解作用：只有配置值满足条件时才启用当前实现。
public class LocalAgentPlanner implements AgentPlanner { // 类型作用：任务规划器，只判断任务类型，不直接执行写操作。

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

    public LocalAgentPlanner(AgentPlannerProperties properties) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this.properties = properties;
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public AgentPlan plan(String query) { // 方法作用：根据用户问题选择知识问答或创建工单任务。
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

    private boolean isNegatedWrite(String query) { // 方法作用：判断当前数据或状态是否满足指定条件。
        return NEGATED_WRITE.matcher(query).find();
    }

    private boolean isWorkflowQuestion(String query) { // 方法作用：判断当前数据或状态是否满足指定条件。
        return HOW_TO_CREATE.matcher(query).find()
                || WORKFLOW_QUESTION.matcher(query).find()
                || TICKET_FIRST_WORKFLOW.matcher(query).find();
    }

    private String normalize(String query) { // 方法作用：完成“normalize”对应的内部步骤，供主流程复用。
        return Normalizer.normalize(query, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "");
    }

    private AgentPlan result(AgentTaskType taskType, String reason) { // 方法作用：完成“result”对应的内部步骤，供主流程复用。
        return new AgentPlan(taskType, AgentTaskTypeSource.PLANNER, properties.provider(), properties.model(), reason);
    }
}
