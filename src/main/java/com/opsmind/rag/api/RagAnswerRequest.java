package com.opsmind.rag.api; // 包作用：接口层，接收 HTTP/JSON 数据并转换成应用层调用。

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record RagAnswerRequest(
        @Schema(example = "数据库连接池告警应该如何处置？") String query /* 字段作用：用户输入的问题或检索文本。 */,
        @Schema(example = "5", nullable = true, description = "Number of reranked evidence chunks") Integer topK /* 字段作用：最终希望返回的最相关结果数量。 */,
        @Schema(
                format = "uuid",
                nullable = true,
                description = "Optional debug scope. Omit it to select documents automatically.") UUID documentId /* 字段作用：对应对象的唯一标识。 */) { // 类型作用：请求 DTO，规定调用接口时可以提交哪些 JSON 字段。
}
