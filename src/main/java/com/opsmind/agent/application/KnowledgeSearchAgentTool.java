package com.opsmind.agent.application; // 包作用：应用层，编排业务步骤，但不绑定具体数据库或模型厂商。

import com.opsmind.agent.domain.AgentToolRisk;
import com.opsmind.rag.application.RagAnswerResult;
import com.opsmind.rag.application.RagAnswerService;
import org.springframework.stereotype.Component;

@Component // 注解作用：把普通组件交给 Spring 容器管理。
public class KnowledgeSearchAgentTool implements AgentTool<AgentToolInput, RagAnswerResult> { // 类型作用：Agent 工具，把一项能力封装成统一名称、风险级别和执行入口。

    private final RagAnswerService answerService;

    public KnowledgeSearchAgentTool(RagAnswerService answerService) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this.answerService = answerService;
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public String name() { // 方法作用：完成“name”对应的内部步骤，供主流程复用。
        return "knowledge_search";
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public AgentToolRisk risk() { // 方法作用：完成“risk”对应的内部步骤，供主流程复用。
        return AgentToolRisk.READ_ONLY;
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public RagAnswerResult execute(AgentToolInput input) { // 方法作用：执行当前工具封装的具体能力并返回结果。
        return answerService.answer(input.query(), null, input.documentId());
    }
}
