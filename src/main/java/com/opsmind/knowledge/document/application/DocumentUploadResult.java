package com.opsmind.knowledge.document.application; // 包作用：应用层，编排业务步骤，但不绑定具体数据库或模型厂商。

import com.opsmind.knowledge.document.domain.IngestionTask;
import com.opsmind.knowledge.document.domain.KnowledgeDocument;

public record DocumentUploadResult(
        KnowledgeDocument document /* 字段作用：该不可变对象携带的 document 数据。 */,
        IngestionTask task /* 字段作用：该不可变对象携带的 task 数据。 */,
        boolean duplicate /* 字段作用：该不可变对象携带的 duplicate 数据。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。
}

