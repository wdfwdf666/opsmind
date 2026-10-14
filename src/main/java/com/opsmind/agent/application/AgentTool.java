package com.opsmind.agent.application; // 包作用：应用层，编排业务步骤，但不绑定具体数据库或模型厂商。

import com.opsmind.agent.domain.AgentToolRisk;
public interface AgentTool<I, O> { // 类型作用：Agent 工具，把一项能力封装成统一名称、风险级别和执行入口。

    String name(); // 接口约定：规定实现类必须完成“name”对应的内部步骤，供主流程复用。

    AgentToolRisk risk(); // 接口约定：规定实现类必须完成“risk”对应的内部步骤，供主流程复用。

    O execute(I input); // 接口约定：规定实现类必须执行当前工具封装的具体能力并返回结果。
}
