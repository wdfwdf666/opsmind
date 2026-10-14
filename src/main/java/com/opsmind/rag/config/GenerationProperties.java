package com.opsmind.rag.config; // 包作用：配置层，把外部配置转换成 Spring 可管理的对象。

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opsmind.generation") // 注解作用：把指定前缀的 YAML 或环境变量绑定到当前对象。
public record GenerationProperties(
        String provider /* 字段作用：当前选择的能力提供方式，例如 qwen 或 local。 */,
        String model /* 字段作用：实际使用的模型名称，用于追踪和审计。 */,
        String baseUrl /* 字段作用：该不可变对象携带的 baseUrl 数据。 */,
        String apiKey /* 字段作用：该不可变对象携带的 apiKey 数据。 */,
        int evidenceTopK /* 字段作用：该不可变对象携带的 evidenceTopK 数据。 */,
        double minimumEvidenceScore /* 字段作用：相关性或质量分数，含义由当前算法定义。 */,
        int maxContextCharacters /* 字段作用：本次流程使用的文本内容。 */,
        int maxOutputTokens /* 字段作用：模型输入或输出的 Token 数量。 */,
        double temperature /* 字段作用：该不可变对象携带的 temperature 数据。 */) { // 类型作用：配置对象，字段值来自 application.yml 或 .env。

    public GenerationProperties { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        if (provider == null || (!provider.equals("qwen") && !provider.equals("local"))) {
            throw new IllegalArgumentException("Generation provider must be qwen or local");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("Generation model must not be blank");
        }
        if (evidenceTopK < 1 || evidenceTopK > 50) {
            throw new IllegalArgumentException("Generation evidence top-k must be between 1 and 50");
        }
        if (!Double.isFinite(minimumEvidenceScore) || minimumEvidenceScore < 0 || minimumEvidenceScore > 1) {
            throw new IllegalArgumentException("Generation minimum evidence score must be between 0 and 1");
        }
        if (maxContextCharacters < 1000 || maxOutputTokens < 1) {
            throw new IllegalArgumentException("Generation context and output budgets are invalid");
        }
        if (temperature < 0 || temperature > 2) {
            throw new IllegalArgumentException("Generation temperature must be between 0 and 2");
        }
    }
}
