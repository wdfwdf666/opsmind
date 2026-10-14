package com.opsmind.knowledge.document.domain; // 包作用：领域层，定义核心业务对象、状态和仓储契约。

import java.time.Instant;
import java.util.UUID;

public record KnowledgeDocument(
        UUID id /* 字段作用：对应对象的唯一标识。 */,
        String originalFilename /* 字段作用：用户上传时的原始文件名。 */,
        String mediaType /* 字段作用：文件或响应的 MIME 类型。 */,
        long size /* 字段作用：该不可变对象携带的 size 数据。 */,
        String sha256 /* 字段作用：该不可变对象携带的 sha256 数据。 */,
        String storageKey /* 字段作用：该不可变对象携带的 storageKey 数据。 */,
        DocumentStatus status /* 字段作用：当前业务状态。 */,
        Instant createdAt /* 字段作用：对象创建时间。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。
}

