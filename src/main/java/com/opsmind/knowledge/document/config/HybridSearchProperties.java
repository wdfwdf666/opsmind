package com.opsmind.knowledge.document.config; // 包作用：配置层，把外部配置转换成 Spring 可管理的对象。

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opsmind.search.hybrid") // 注解作用：把指定前缀的 YAML 或环境变量绑定到当前对象。
public record HybridSearchProperties(
        int rrfK /* 字段作用：该不可变对象携带的 rrfK 数据。 */,
        int candidateMultiplier /* 字段作用：该不可变对象携带的 candidateMultiplier 数据。 */,
        int minimumCandidates /* 字段作用：该不可变对象携带的 minimumCandidates 数据。 */) { // 类型作用：配置对象，字段值来自 application.yml 或 .env。

    public HybridSearchProperties { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        if (rrfK < 1 || candidateMultiplier < 1 || minimumCandidates < 1) {
            throw new IllegalArgumentException("Hybrid search properties must be positive");
        }
    }
}
