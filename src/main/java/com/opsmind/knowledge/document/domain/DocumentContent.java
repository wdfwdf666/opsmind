package com.opsmind.knowledge.document.domain; // 包作用：领域层，定义核心业务对象、状态和仓储契约。

import java.time.Instant;
import java.util.UUID;

public record DocumentContent(
        UUID documentId /* 字段作用：对应对象的唯一标识。 */,
        String title /* 字段作用：该不可变对象携带的 title 数据。 */,
        String detectedMediaType /* 字段作用：文件或响应的 MIME 类型。 */,
        String text /* 字段作用：本次流程使用的文本内容。 */,
        int characterCount /* 字段作用：对应项目的数量统计。 */,
        Instant parsedAt /* 字段作用：该不可变对象携带的 parsedAt 数据。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。
}
