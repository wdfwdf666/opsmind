package com.opsmind.knowledge.document.config; // 包作用：配置层，把外部配置转换成 Spring 可管理的对象。

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opsmind.chunking") // 注解作用：把指定前缀的 YAML 或环境变量绑定到当前对象。
public record ChunkingProperties(
        int parentMaxCharacters /* 字段作用：该不可变对象携带的 parentMaxCharacters 数据。 */,
        int childMaxCharacters /* 字段作用：该不可变对象携带的 childMaxCharacters 数据。 */,
        int childOverlapCharacters /* 字段作用：该不可变对象携带的 childOverlapCharacters 数据。 */) { // 类型作用：配置对象，字段值来自 application.yml 或 .env。

    public ChunkingProperties { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        if (parentMaxCharacters < 1 || childMaxCharacters < 1) {
            throw new IllegalArgumentException("Chunk sizes must be positive");
        }
        if (parentMaxCharacters < childMaxCharacters) {
            throw new IllegalArgumentException("Parent chunk size must not be smaller than child chunk size");
        }
        if (childOverlapCharacters < 0 || childOverlapCharacters >= childMaxCharacters) {
            throw new IllegalArgumentException("Child overlap must be between 0 and child chunk size - 1");
        }
    }
}
