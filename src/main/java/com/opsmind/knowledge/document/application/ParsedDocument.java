package com.opsmind.knowledge.document.application; // 包作用：应用层，编排业务步骤，但不绑定具体数据库或模型厂商。

public record ParsedDocument(
        String title /* 字段作用：该不可变对象携带的 title 数据。 */,
        String detectedMediaType /* 字段作用：文件或响应的 MIME 类型。 */,
        String text /* 字段作用：本次流程使用的文本内容。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。
}
