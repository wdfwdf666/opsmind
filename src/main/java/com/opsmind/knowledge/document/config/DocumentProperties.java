package com.opsmind.knowledge.document.config; // 包作用：配置层，把外部配置转换成 Spring 可管理的对象。

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "opsmind.document") // 注解作用：把指定前缀的 YAML 或环境变量绑定到当前对象。
public record DocumentProperties(
        DataSize maxSize /* 字段作用：该不可变对象携带的 maxSize 数据。 */,
        Path storageLocation /* 字段作用：该不可变对象携带的 storageLocation 数据。 */,
        int maxExtractedCharacters /* 字段作用：该不可变对象携带的 maxExtractedCharacters 数据。 */) { // 类型作用：配置对象，字段值来自 application.yml 或 .env。

    @ConstructorBinding
    public DocumentProperties { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
    }

    public DocumentProperties(DataSize maxSize, Path storageLocation) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this(maxSize, storageLocation, 2_000_000);
    }
}

