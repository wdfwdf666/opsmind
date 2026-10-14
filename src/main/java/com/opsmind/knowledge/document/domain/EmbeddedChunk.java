package com.opsmind.knowledge.document.domain; // 包作用：领域层，定义核心业务对象、状态和仓储契约。

import java.util.UUID;

public record EmbeddedChunk(
        UUID chunkId /* 字段作用：对应对象的唯一标识。 */,
        UUID parentId /* 字段作用：对应对象的唯一标识。 */,
        UUID documentId /* 字段作用：对应对象的唯一标识。 */,
        String filename /* 字段作用：用户上传时的原始文件名。 */,
        String headingPath /* 字段作用：该不可变对象携带的 headingPath 数据。 */,
        String childText /* 字段作用：本次流程使用的文本内容。 */,
        String parentText /* 字段作用：本次流程使用的文本内容。 */,
        double[] vector /* 字段作用：该不可变对象携带的 vector 数据。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。

    public EmbeddedChunk { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        vector = vector.clone();
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public double[] vector() { // 方法作用：完成“vector”对应的内部步骤，供主流程复用。
        return vector.clone();
    }
}
