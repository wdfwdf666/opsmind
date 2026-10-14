package com.opsmind.knowledge.document.domain; // 包作用：领域层，定义核心业务对象、状态和仓储契约。

import java.util.List;

public record NearestChunkSearch(int candidateCount /* 字段作用：对应项目的数量统计。 */, List<ScoredEmbeddedChunk> hits /* 字段作用：该不可变对象携带的 hits 数据。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。
}
