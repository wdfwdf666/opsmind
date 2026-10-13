package com.opsmind.agent.application;

import com.opsmind.agent.domain.AgentToolRisk;
import com.opsmind.rag.application.RagAnswerResult;

public interface AgentTool {

    String name();

    AgentToolRisk risk();

    RagAnswerResult execute(AgentToolInput input);
}
