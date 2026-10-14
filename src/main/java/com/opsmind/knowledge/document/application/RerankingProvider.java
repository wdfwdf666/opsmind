package com.opsmind.knowledge.document.application; // 包作用：应用层，编排业务步骤，但不绑定具体数据库或模型厂商。

import com.opsmind.knowledge.document.domain.SemanticSearchHit;

import java.util.List;

public interface RerankingProvider { // 类型作用：能力接口，用于隔离业务代码与具体模型厂商。

    String model(); // 接口约定：返回当前实现使用的模型名称，供响应和审计展示。

    List<SemanticSearchHit> rerank(String query, List<SemanticSearchHit> candidates, int topK); // 接口约定：规定实现类必须执行检索或相关性排序并返回有序结果。
}
