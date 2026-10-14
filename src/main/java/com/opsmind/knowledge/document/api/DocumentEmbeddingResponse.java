package com.opsmind.knowledge.document.api; // 包作用：接口层，接收 HTTP/JSON 数据并转换成应用层调用。

import com.opsmind.knowledge.document.application.DocumentEmbeddingResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record DocumentEmbeddingResponse(
        @Schema(format = "uuid") UUID documentId /* 字段作用：对应对象的唯一标识。 */,
        String model /* 字段作用：实际使用的模型名称，用于追踪和审计。 */,
        int dimension /* 字段作用：该不可变对象携带的 dimension 数据。 */,
        int embeddedChunkCount /* 字段作用：对应项目的数量统计。 */) { // 类型作用：响应 DTO，规定接口返回给前端的 JSON 字段。

    public static DocumentEmbeddingResponse from(DocumentEmbeddingResult result) { // 方法作用：把内部领域对象转换为对外响应 DTO。
        return new DocumentEmbeddingResponse(
                result.documentId(), result.model(), result.dimension(), result.embeddedChunkCount());
    }
}
