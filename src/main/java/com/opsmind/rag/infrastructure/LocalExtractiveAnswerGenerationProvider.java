package com.opsmind.rag.infrastructure; // 包作用：基础设施层，实现数据库、文件系统和外部模型调用。

import com.opsmind.rag.application.AnswerGenerationProvider;
import com.opsmind.rag.application.GeneratedAnswer;
import com.opsmind.rag.application.RagEvidence;
import com.opsmind.rag.config.GenerationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component // 注解作用：把普通组件交给 Spring 容器管理。
@ConditionalOnProperty(prefix = "opsmind.generation", name = "provider", havingValue = "local") // 注解作用：只有配置值满足条件时才启用当前实现。
public class LocalExtractiveAnswerGenerationProvider implements AnswerGenerationProvider { // 类型作用：封装 LocalExtractiveAnswerGenerationProvider 对应的单一职责，供当前模块协作使用。

    private final GenerationProperties properties;

    public LocalExtractiveAnswerGenerationProvider(GenerationProperties properties) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this.properties = properties;
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public String model() { // 方法作用：返回当前实现使用的模型名称，供响应和审计展示。
        return properties.model();
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public GeneratedAnswer generate(String query, List<RagEvidence> evidence) { // 方法作用：根据输入和证据生成受约束的输出结果。
        RagEvidence first = evidence.getFirst();
        return new GeneratedAnswer(first.excerpt() + " [" + first.sourceId() + "]", 0, 0, 0);
    }
}
