package com.opsmind.knowledge.document.api; // 包作用：接口层，接收 HTTP/JSON 数据并转换成应用层调用。

import com.opsmind.knowledge.document.domain.ChunkType;
import com.opsmind.knowledge.document.domain.DocumentChunk;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "A parent context chunk or child retrieval chunk")
public record DocumentChunkResponse(
        @Schema(format = "uuid") UUID chunkId /* 字段作用：对应对象的唯一标识。 */,
        @Schema(format = "uuid") UUID documentId /* 字段作用：对应对象的唯一标识。 */,
        @Schema(format = "uuid", nullable = true) UUID parentId /* 字段作用：对应对象的唯一标识。 */,
        ChunkType type /* 字段作用：该不可变对象携带的 type 数据。 */,
        int ordinal /* 字段作用：该不可变对象携带的 ordinal 数据。 */,
        String headingPath /* 字段作用：该不可变对象携带的 headingPath 数据。 */,
        String text /* 字段作用：本次流程使用的文本内容。 */,
        String contentHash /* 字段作用：本次流程使用的文本内容。 */,
        int characterCount /* 字段作用：对应项目的数量统计。 */,
        @Schema(description = "Approximation based on four characters per token") int estimatedTokenCount /* 字段作用：对应项目的数量统计。 */,
        Instant createdAt /* 字段作用：对象创建时间。 */) { // 类型作用：响应 DTO，规定接口返回给前端的 JSON 字段。

    public static DocumentChunkResponse from(DocumentChunk chunk) { // 方法作用：把内部领域对象转换为对外响应 DTO。
        return new DocumentChunkResponse(
                chunk.id(), chunk.documentId(), chunk.parentId(), chunk.type(), chunk.ordinal(),
                chunk.headingPath(), chunk.text(), chunk.contentHash(), chunk.characterCount(),
                chunk.estimatedTokenCount(), chunk.createdAt());
    }
}
