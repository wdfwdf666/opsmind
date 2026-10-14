package com.opsmind.evaluation.domain; // 包作用：领域层，定义核心业务对象、状态和仓储契约。

import com.opsmind.agent.domain.AgentTaskType;

public record AgentRoutingEvaluationCaseResult(
        String id /* 字段作用：对应对象的唯一标识。 */,
        String category /* 字段作用：该不可变对象携带的 category 数据。 */,
        String query /* 字段作用：用户输入的问题或检索文本。 */,
        AgentTaskType expectedTaskType /* 字段作用：该不可变对象携带的 expectedTaskType 数据。 */,
        AgentTaskType actualTaskType /* 字段作用：该不可变对象携带的 actualTaskType 数据。 */,
        boolean correct /* 字段作用：该不可变对象携带的 correct 数据。 */,
        boolean dangerousWriteMisroute /* 字段作用：该不可变对象携带的 dangerousWriteMisroute 数据。 */,
        String plannerReason /* 字段作用：产生当前状态或决策的原因。 */,
        double latencyMs /* 字段作用：该不可变对象携带的 latencyMs 数据。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。
}
