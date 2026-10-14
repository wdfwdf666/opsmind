package com.opsmind.agent.application; // 包作用：应用层，编排业务步骤，但不绑定具体数据库或模型厂商。

import com.opsmind.agent.domain.AgentTaskType;
import com.opsmind.agent.domain.AgentTaskTypeSource;

public record AgentPlan(
        AgentTaskType taskType /* 字段作用：该不可变对象携带的 taskType 数据。 */,
        AgentTaskTypeSource source /* 字段作用：该不可变对象携带的 source 数据。 */,
        String provider /* 字段作用：当前选择的能力提供方式，例如 qwen 或 local。 */,
        String model /* 字段作用：实际使用的模型名称，用于追踪和审计。 */,
        String reason /* 字段作用：产生当前状态或决策的原因。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。

    public static AgentPlan explicit(AgentTaskType taskType) { // 方法作用：完成“explicit”对应的内部步骤，供主流程复用。
        return new AgentPlan(taskType, AgentTaskTypeSource.EXPLICIT, "request", "explicit-override",
                "调用方显式指定任务类型");
    }
}
