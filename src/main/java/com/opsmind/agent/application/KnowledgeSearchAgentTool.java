package com.opsmind.agent.application;

import com.opsmind.agent.domain.AgentToolRisk;
import com.opsmind.rag.application.RagAnswerResult;
import com.opsmind.rag.application.RagAnswerService;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeSearchAgentTool implements AgentTool {

    private final RagAnswerService answerService;

    public KnowledgeSearchAgentTool(RagAnswerService answerService) {
        this.answerService = answerService;
    }

    @Override
    public String name() {
        return "knowledge_search";
    }

    @Override
    public AgentToolRisk risk() {
        return AgentToolRisk.READ_ONLY;
    }

    @Override
    public RagAnswerResult execute(AgentToolInput input) {
        return answerService.answer(input.query(), null, input.documentId());
    }
}
