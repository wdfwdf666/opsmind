package com.opsmind.agent.application;

import com.opsmind.agent.domain.AgentException;
import com.opsmind.agent.domain.AgentToolRisk;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AgentToolPolicy {

    public void requireAutoExecutable(AgentTool<?, ?> tool) {
        if (tool.risk() != AgentToolRisk.READ_ONLY) {
            throw new AgentException(
                    "AGENT_TOOL_APPROVAL_REQUIRED",
                    "Tool requires approval before execution: " + tool.name(),
                    HttpStatus.CONFLICT);
        }
    }

    public void requireApprovedExecutable(AgentTool<?, ?> tool) {
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
