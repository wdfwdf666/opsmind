package com.opsmind.knowledge.document.domain; // 包作用：领域层，定义核心业务对象、状态和仓储契约。

import java.time.Instant;
import java.util.UUID;

public record IngestionTask(
        UUID id /* 字段作用：对应对象的唯一标识。 */,
        UUID documentId /* 字段作用：对应对象的唯一标识。 */,
        IngestionTaskStatus status /* 字段作用：当前业务状态。 */,
        Instant createdAt /* 字段作用：对象创建时间。 */,
        Instant updatedAt /* 字段作用：最近更新或完成时间。 */,
        String errorMessage /* 字段作用：该不可变对象携带的 errorMessage 数据。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。

    public static IngestionTask pending(UUID documentId, Instant now) { // 方法作用：完成“pending”对应的内部步骤，供主流程复用。
        return new IngestionTask(UUID.randomUUID(), documentId, IngestionTaskStatus.PENDING, now, now, null);
    }
}

