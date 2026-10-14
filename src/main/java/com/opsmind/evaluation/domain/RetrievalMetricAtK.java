package com.opsmind.evaluation.domain; // 包作用：领域层，定义核心业务对象、状态和仓储契约。

public record RetrievalMetricAtK(
        int k /* 字段作用：该不可变对象携带的 k 数据。 */,
        double recall /* 字段作用：该不可变对象携带的 recall 数据。 */,
        double meanReciprocalRank /* 字段作用：该不可变对象携带的 meanReciprocalRank 数据。 */,
        double hitRate /* 字段作用：该不可变对象携带的 hitRate 数据。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。
}
