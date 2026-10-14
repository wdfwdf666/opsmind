package com.opsmind.knowledge.document.domain; // 包作用：领域层，定义核心业务对象、状态和仓储契约。

import java.util.List;

public record DocumentChunkPage(
        List<DocumentChunk> items /* 字段作用：该不可变对象携带的 items 数据。 */,
        ChunkType type /* 字段作用：该不可变对象携带的 type 数据。 */,
        int page /* 字段作用：分页位置或分页结果。 */,
        int size /* 字段作用：该不可变对象携带的 size 数据。 */,
        long totalElements /* 字段作用：该不可变对象携带的 totalElements 数据。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。

    public long totalPages() { // 方法作用：把外部或数据库数据转换成项目内部对象。
        return totalElements == 0 ? 0 : (totalElements + size - 1) / size;
    }
}
