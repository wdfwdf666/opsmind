package com.opsmind.knowledge.document.domain; // 包作用：领域层，定义核心业务对象、状态和仓储契约。

import java.util.UUID;

public record DocumentSummary(
        KnowledgeDocument document /* 字段作用：该不可变对象携带的 document 数据。 */,
        UUID taskId /* 字段作用：对应对象的唯一标识。 */,
        IngestionTaskStatus taskStatus /* 字段作用：当前业务状态。 */,
        String taskError /* 字段作用：该不可变对象携带的 taskError 数据。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。
}
