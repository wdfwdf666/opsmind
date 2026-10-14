package com.opsmind.rag.application; // 包作用：应用层，编排业务步骤，但不绑定具体数据库或模型厂商。

import java.util.List;
import java.util.UUID;

public record RagAnswerResult(
        String query /* 字段作用：用户输入的问题或检索文本。 */,
        UUID documentId /* 字段作用：对应对象的唯一标识。 */,
        String retrievalModel /* 字段作用：该不可变对象携带的 retrievalModel 数据。 */,
        String generationModel /* 字段作用：该不可变对象携带的 generationModel 数据。 */,
        boolean refused /* 字段作用：该不可变对象携带的 refused 数据。 */,
        RagRefusalReason refusalReason /* 字段作用：产生当前状态或决策的原因。 */,
        Double topEvidenceScore /* 字段作用：相关性或质量分数，含义由当前算法定义。 */,
        String answer /* 字段作用：该不可变对象携带的 answer 数据。 */,
        List<RagCitation> citations /* 字段作用：该不可变对象携带的 citations 数据。 */,
        Integer promptTokens /* 字段作用：模型输入或输出的 Token 数量。 */,
        Integer completionTokens /* 字段作用：模型输入或输出的 Token 数量。 */,
        Integer totalTokens /* 字段作用：模型输入或输出的 Token 数量。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。
}
