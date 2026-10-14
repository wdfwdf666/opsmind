package com.opsmind.agent.application;

import com.opsmind.agent.domain.AgentException;
import com.opsmind.agent.domain.AgentToolRisk;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 工具安全策略的集中入口。
 * <p>只读工具可自动执行；写工具必须经过审批；禁止工具在任何路径都不执行。
 * 把规则集中在此处，避免新增工具时在多段流程里遗漏检查。</p>
 */
@Component
public class AgentToolPolicy {

    public void requireAutoExecutable(AgentTool<?, ?> tool) {
        // 自动链路只放行 READ_ONLY，写操作即使模型要求也会停在审批状态。
        if (tool.risk() != AgentToolRisk.READ_ONLY) {
            throw new AgentException(
                    "AGENT_TOOL_APPROVAL_REQUIRED",
                    "Tool requires approval before execution: " + tool.name(),
                    HttpStatus.CONFLICT);
        }
    }

    public void requireApprovedExecutable(AgentTool<?, ?> tool) {
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
