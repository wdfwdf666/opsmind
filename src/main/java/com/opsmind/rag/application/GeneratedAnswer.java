package com.opsmind.rag.application; // 包作用：应用层，编排业务步骤，但不绑定具体数据库或模型厂商。

public record GeneratedAnswer(
        String text /* 字段作用：本次流程使用的文本内容。 */,
        Integer promptTokens /* 字段作用：模型输入或输出的 Token 数量。 */,
        Integer completionTokens /* 字段作用：模型输入或输出的 Token 数量。 */,
        Integer totalTokens /* 字段作用：模型输入或输出的 Token 数量。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。
}
