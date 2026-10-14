package com.opsmind.evaluation.domain; // 包作用：领域层，定义核心业务对象、状态和仓储契约。

import com.opsmind.agent.domain.AgentTaskType;

public record AgentRoutingClassMetric(
        AgentTaskType taskType /* 字段作用：该不可变对象携带的 taskType 数据。 */,
        int support /* 字段作用：该不可变对象携带的 support 数据。 */,
        int predicted /* 字段作用：该不可变对象携带的 predicted 数据。 */,
        int truePositive /* 字段作用：该不可变对象携带的 truePositive 数据。 */,
        double precision /* 字段作用：该不可变对象携带的 precision 数据。 */,
        double recall /* 字段作用：该不可变对象携带的 recall 数据。 */,
        double f1 /* 字段作用：该不可变对象携带的 f1 数据。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。
}
