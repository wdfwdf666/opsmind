package com.opsmind.rag.api; // 包作用：接口层，接收 HTTP/JSON 数据并转换成应用层调用。

import com.opsmind.rag.application.RagCitation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record RagCitationResponse(
        String sourceId /* 字段作用：对应对象的唯一标识。 */,
        @Schema(format = "uuid") UUID chunkId /* 字段作用：对应对象的唯一标识。 */,
        @Schema(format = "uuid", nullable = true) UUID parentId /* 字段作用：对应对象的唯一标识。 */,
        @Schema(format = "uuid") UUID documentId /* 字段作用：对应对象的唯一标识。 */,
        String filename /* 字段作用：用户上传时的原始文件名。 */,
        String headingPath /* 字段作用：该不可变对象携带的 headingPath 数据。 */,
        String excerpt /* 字段作用：该不可变对象携带的 excerpt 数据。 */,
        double score /* 字段作用：相关性或质量分数，含义由当前算法定义。 */) { // 类型作用：响应 DTO，规定接口返回给前端的 JSON 字段。

    static RagCitationResponse from(RagCitation citation) { // 方法作用：把内部领域对象转换为对外响应 DTO。
        return new RagCitationResponse(
                citation.sourceId(), citation.chunkId(), citation.parentId(), citation.documentId(),
                citation.filename(), citation.headingPath(), citation.excerpt(), citation.score());
    }
}
