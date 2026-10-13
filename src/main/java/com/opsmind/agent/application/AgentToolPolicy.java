package com.opsmind.agent.application;

import com.opsmind.agent.domain.AgentException;
import com.opsmind.agent.domain.AgentToolRisk;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AgentToolPolicy {

    public void requireAutoExecutable(AgentTool tool) {
        if (tool.risk() != AgentToolRisk.READ_ONLY) {
            throw new AgentException(
                    "AGENT_TOOL_APPROVAL_REQUIRED",
                    "Tool requires approval before execution: " + tool.name(),
                    HttpStatus.CONFLICT);
        }
    }
}
