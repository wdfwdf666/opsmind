package com.opsmind.evaluation.domain; // 包作用：领域层，定义核心业务对象、状态和仓储契约。

public enum RetrievalStrategy { // 类型作用：枚举 RetrievalStrategy 集中列出允许的有限状态，避免随意使用字符串。
    VECTOR,
    HYBRID,
    RERANKED
}
