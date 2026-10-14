package com.opsmind.knowledge.document.domain; // 包作用：领域层，定义核心业务对象、状态和仓储契约。

import java.util.List;
import java.util.UUID;

public record SemanticSearchResult(
        String query /* 字段作用：用户输入的问题或检索文本。 */,
        String model /* 字段作用：实际使用的模型名称，用于追踪和审计。 */,
        UUID documentId /* 字段作用：对应对象的唯一标识。 */,
        int candidateCount /* 字段作用：对应项目的数量统计。 */,
        List<SemanticSearchHit> hits /* 字段作用：该不可变对象携带的 hits 数据。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。
}
