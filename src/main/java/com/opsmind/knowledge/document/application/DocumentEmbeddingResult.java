package com.opsmind.knowledge.document.application; // 包作用：应用层，编排业务步骤，但不绑定具体数据库或模型厂商。

import java.util.UUID;

public record DocumentEmbeddingResult(
        UUID documentId /* 字段作用：对应对象的唯一标识。 */,
        String model /* 字段作用：实际使用的模型名称，用于追踪和审计。 */,
        int dimension /* 字段作用：该不可变对象携带的 dimension 数据。 */,
        int embeddedChunkCount /* 字段作用：对应项目的数量统计。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。
}
