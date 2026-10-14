package com.opsmind.rag.application; // 包作用：应用层，编排业务步骤，但不绑定具体数据库或模型厂商。

public enum RagRefusalReason { // 类型作用：枚举 RagRefusalReason 集中列出允许的有限状态，避免随意使用字符串。
    NO_EVIDENCE,
    LOW_RELEVANCE
}
