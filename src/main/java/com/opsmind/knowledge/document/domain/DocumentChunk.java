package com.opsmind.knowledge.document.domain; // 包作用：领域层，定义核心业务对象、状态和仓储契约。

import java.time.Instant;
import java.util.UUID;

public record DocumentChunk(
        UUID id /* 字段作用：对应对象的唯一标识。 */,
        UUID documentId /* 字段作用：对应对象的唯一标识。 */,
        UUID parentId /* 字段作用：对应对象的唯一标识。 */,
        ChunkType type /* 字段作用：该不可变对象携带的 type 数据。 */,
        int ordinal /* 字段作用：该不可变对象携带的 ordinal 数据。 */,
        String headingPath /* 字段作用：该不可变对象携带的 headingPath 数据。 */,
        String text /* 字段作用：本次流程使用的文本内容。 */,
        String contentHash /* 字段作用：本次流程使用的文本内容。 */,
        int characterCount /* 字段作用：对应项目的数量统计。 */,
        int estimatedTokenCount /* 字段作用：对应项目的数量统计。 */,
        Instant createdAt /* 字段作用：对象创建时间。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。
}
