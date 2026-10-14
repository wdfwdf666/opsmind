package com.opsmind.rag.application; // 包作用：应用层，编排业务步骤，但不绑定具体数据库或模型厂商。

import java.util.List;

public interface AnswerGenerationProvider { // 类型作用：能力接口，用于隔离业务代码与具体模型厂商。

    String model(); // 接口约定：返回当前实现使用的模型名称，供响应和审计展示。

    GeneratedAnswer generate(String query, List<RagEvidence> evidence); // 接口约定：规定实现类必须根据输入和证据生成受约束的输出结果。
}
