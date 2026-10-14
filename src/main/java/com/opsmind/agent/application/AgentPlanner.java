package com.opsmind.agent.application; // 包作用：应用层，编排业务步骤，但不绑定具体数据库或模型厂商。

public interface AgentPlanner { // 类型作用：任务规划器，只判断任务类型，不直接执行写操作。

    AgentPlan plan(String query); // 接口约定：根据用户问题选择知识问答或创建工单任务。
}
