package com.opsmind.rag.application; // 包作用：应用层，编排业务步骤，但不绑定具体数据库或模型厂商。

import java.util.UUID;

public record RagEvidence(
        String sourceId /* 字段作用：对应对象的唯一标识。 */,
        UUID chunkId /* 字段作用：对应对象的唯一标识。 */,
        UUID parentId /* 字段作用：对应对象的唯一标识。 */,
        UUID documentId /* 字段作用：对应对象的唯一标识。 */,
        String filename /* 字段作用：用户上传时的原始文件名。 */,
        String headingPath /* 字段作用：该不可变对象携带的 headingPath 数据。 */,
        String passage /* 字段作用：本次流程使用的文本内容。 */,
        String excerpt /* 字段作用：该不可变对象携带的 excerpt 数据。 */,
        double score /* 字段作用：相关性或质量分数，含义由当前算法定义。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。
}
