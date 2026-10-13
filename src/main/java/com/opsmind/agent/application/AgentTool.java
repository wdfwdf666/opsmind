package com.opsmind.agent.application;

import com.opsmind.agent.domain.AgentToolRisk;
public interface AgentTool<I, O> {

    String name();

    AgentToolRisk risk();

    O execute(I input);
}
