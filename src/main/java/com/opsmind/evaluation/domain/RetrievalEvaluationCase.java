package com.opsmind.evaluation.domain; // 包作用：领域层，定义核心业务对象、状态和仓储契约。

import java.util.List;

public record RetrievalEvaluationCase(
        String id /* 字段作用：对应对象的唯一标识。 */,
        String query /* 字段作用：用户输入的问题或检索文本。 */,
        List<String> expectedHeadingPaths /* 字段作用：该不可变对象携带的 expectedHeadingPaths 数据。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。
}
