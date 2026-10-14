package com.opsmind.evaluation.domain; // 包作用：领域层，定义核心业务对象、状态和仓储契约。

import java.util.List;

public record AnswerEvaluationDataset(
        String name /* 字段作用：该不可变对象携带的 name 数据。 */,
        String description /* 字段作用：该不可变对象携带的 description 数据。 */,
        int topK /* 字段作用：最终希望返回的最相关结果数量。 */,
        List<AnswerEvaluationCase> cases /* 字段作用：该不可变对象携带的 cases 数据。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。
}
