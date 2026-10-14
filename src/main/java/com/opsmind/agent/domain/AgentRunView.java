package com.opsmind.agent.domain; // 包作用：领域层，定义核心业务对象、状态和仓储契约。

import java.util.List;

public record AgentRunView(
        AgentRun run /* 字段作用：该不可变对象携带的 run 数据。 */,
        List<AgentStep> steps /* 字段作用：该不可变对象携带的 steps 数据。 */,
        AgentApproval approval /* 字段作用：该不可变对象携带的 approval 数据。 */,
        SupportTicket ticket /* 字段作用：该不可变对象携带的 ticket 数据。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。
}
