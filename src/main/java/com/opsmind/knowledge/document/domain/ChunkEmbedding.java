package com.opsmind.knowledge.document.domain; // 包作用：领域层，定义核心业务对象、状态和仓储契约。

import java.time.Instant;
import java.util.UUID;

public record ChunkEmbedding(
        UUID chunkId /* 字段作用：对应对象的唯一标识。 */,
        UUID documentId /* 字段作用：对应对象的唯一标识。 */,
        String model /* 字段作用：实际使用的模型名称，用于追踪和审计。 */,
        int dimension /* 字段作用：该不可变对象携带的 dimension 数据。 */,
        double[] vector /* 字段作用：该不可变对象携带的 vector 数据。 */,
        String contentHash /* 字段作用：本次流程使用的文本内容。 */,
        Instant createdAt /* 字段作用：对象创建时间。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。

    public ChunkEmbedding { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        vector = vector.clone();
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public double[] vector() { // 方法作用：完成“vector”对应的内部步骤，供主流程复用。
        return vector.clone();
    }
}
