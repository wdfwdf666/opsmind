package com.opsmind.knowledge.document.application; // 包作用：应用层，编排业务步骤，但不绑定具体数据库或模型厂商。

import java.util.List;

public interface EmbeddingProvider { // 类型作用：能力接口，用于隔离业务代码与具体模型厂商。

    String model(); // 接口约定：返回当前实现使用的模型名称，供响应和审计展示。

    int dimension(); // 接口约定：规定实现类必须完成“dimension”对应的内部步骤，供主流程复用。

    List<double[]> embed(List<String> texts); // 接口约定：规定实现类必须完成“embed”对应的内部步骤，供主流程复用。
}
