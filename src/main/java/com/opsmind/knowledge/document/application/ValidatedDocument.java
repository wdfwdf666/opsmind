package com.opsmind.knowledge.document.application; // 包作用：应用层，编排业务步骤，但不绑定具体数据库或模型厂商。

public record ValidatedDocument(
        String filename /* 字段作用：用户上传时的原始文件名。 */,
        String extension /* 字段作用：该不可变对象携带的 extension 数据。 */,
        String mediaType /* 字段作用：文件或响应的 MIME 类型。 */,
        byte[] content /* 字段作用：本次流程使用的文本内容。 */,
        String sha256 /* 字段作用：该不可变对象携带的 sha256 数据。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。

    public long size() { // 方法作用：完成“size”对应的内部步骤，供主流程复用。
        return content.length;
    }
}

