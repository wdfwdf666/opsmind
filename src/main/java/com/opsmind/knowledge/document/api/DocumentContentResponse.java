package com.opsmind.knowledge.document.api; // 包作用：接口层，接收 HTTP/JSON 数据并转换成应用层调用。

import com.opsmind.knowledge.document.domain.DocumentContent;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Parsed and cleaned document content")
public record DocumentContentResponse(
        @Schema(format = "uuid") UUID documentId /* 字段作用：对应对象的唯一标识。 */,
        String title /* 字段作用：该不可变对象携带的 title 数据。 */,
        @Schema(example = "text/plain; charset=UTF-8") String detectedMediaType /* 字段作用：文件或响应的 MIME 类型。 */,
        String text /* 字段作用：本次流程使用的文本内容。 */,
        int characterCount /* 字段作用：对应项目的数量统计。 */,
        Instant parsedAt /* 字段作用：该不可变对象携带的 parsedAt 数据。 */) { // 类型作用：响应 DTO，规定接口返回给前端的 JSON 字段。

    public static DocumentContentResponse from(DocumentContent content) { // 方法作用：把内部领域对象转换为对外响应 DTO。
        return new DocumentContentResponse(
                content.documentId(), content.title(), content.detectedMediaType(), content.text(),
                content.characterCount(), content.parsedAt());
    }
}
