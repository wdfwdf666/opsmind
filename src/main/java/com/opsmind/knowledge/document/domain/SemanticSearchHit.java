package com.opsmind.knowledge.document.domain; // 包作用：领域层，定义核心业务对象、状态和仓储契约。

import java.util.UUID;

public record SemanticSearchHit(
        UUID chunkId /* 字段作用：对应对象的唯一标识。 */,
        UUID parentId /* 字段作用：对应对象的唯一标识。 */,
        UUID documentId /* 字段作用：对应对象的唯一标识。 */,
        String filename /* 字段作用：用户上传时的原始文件名。 */,
        String headingPath /* 字段作用：该不可变对象携带的 headingPath 数据。 */,
        String childText /* 字段作用：本次流程使用的文本内容。 */,
        String parentText /* 字段作用：本次流程使用的文本内容。 */,
        double score /* 字段作用：相关性或质量分数，含义由当前算法定义。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。
}
