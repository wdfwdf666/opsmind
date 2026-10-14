package com.opsmind.agent.application; // 包作用：应用层，编排业务步骤，但不绑定具体数据库或模型厂商。

import com.opsmind.agent.domain.AgentException;
import com.opsmind.agent.domain.AgentToolRisk;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 工具安全策略的集中入口。
 * <p>只读工具可自动执行；写工具必须经过审批；禁止工具在任何路径都不执行。
 * 把规则集中在此处，避免新增工具时在多段流程里遗漏检查。</p>
 */
@Component // 注解作用：把普通组件交给 Spring 容器管理。
public class AgentToolPolicy { // 类型作用：封装 AgentToolPolicy 对应的单一职责，供当前模块协作使用。

    public void requireAutoExecutable(AgentTool<?, ?> tool) { // 方法作用：自动执行前校验工具必须是只读风险级别。
        // 自动链路只放行 READ_ONLY，写操作即使模型要求也会停在审批状态。
        if (tool.risk() != AgentToolRisk.READ_ONLY) {
            throw new AgentException(
                    "AGENT_TOOL_APPROVAL_REQUIRED",
                    "Tool requires approval before execution: " + tool.name(),
                    HttpStatus.CONFLICT);
        }
    }

    public void requireApprovedExecutable(AgentTool<?, ?> tool) { // 方法作用：审批执行前阻断禁止工具并确认它属于需审批写操作。
        // 审批不能把明确禁止的工具变成可执行工具。
        if (tool.risk() == AgentToolRisk.PROHIBITED) {
            throw new AgentException(
                    "AGENT_TOOL_PROHIBITED",
                    "Tool execution is prohibited: " + tool.name(),
                    HttpStatus.FORBIDDEN);
        }
        if (tool.risk() != AgentToolRisk.WRITE_REQUIRES_APPROVAL) {
            throw new AgentException(
                    "AGENT_TOOL_APPROVAL_NOT_APPLICABLE",
                    "Tool does not use the write approval path: " + tool.name(),
                    HttpStatus.CONFLICT);
        }
    }
}
