package com.opsmind.agent.api; // 包作用：接口层，接收 HTTP/JSON 数据并转换成应用层调用。

public enum AgentApprovalDecision { // 类型作用：枚举 AgentApprovalDecision 集中列出允许的有限状态，避免随意使用字符串。
    APPROVE,
    REJECT
}
