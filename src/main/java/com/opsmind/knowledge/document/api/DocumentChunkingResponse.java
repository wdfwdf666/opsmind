package com.opsmind.knowledge.document.api; // 包作用：接口层，接收 HTTP/JSON 数据并转换成应用层调用。

import com.opsmind.knowledge.document.application.DocumentChunkingResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Result of rebuilding parent and child chunks")
public record DocumentChunkingResponse(
        @Schema(format = "uuid") UUID documentId /* 字段作用：对应对象的唯一标识。 */,
        int parentCount /* 字段作用：对应项目的数量统计。 */,
        int childCount /* 字段作用：对应项目的数量统计。 */,
        int totalCount /* 字段作用：对应项目的数量统计。 */) { // 类型作用：响应 DTO，规定接口返回给前端的 JSON 字段。

    public static DocumentChunkingResponse from(DocumentChunkingResult result) { // 方法作用：把内部领域对象转换为对外响应 DTO。
        return new DocumentChunkingResponse(
                result.documentId(), result.parentCount(), result.childCount(), result.totalCount());
    }
}
