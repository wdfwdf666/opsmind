package com.opsmind.knowledge.document.api; // 包作用：接口层，接收 HTTP/JSON 数据并转换成应用层调用。

import com.opsmind.knowledge.document.domain.SemanticSearchHit;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record SemanticSearchHitResponse(
        @Schema(format = "uuid") UUID chunkId /* 字段作用：对应对象的唯一标识。 */,
        @Schema(format = "uuid") UUID parentId /* 字段作用：对应对象的唯一标识。 */,
        @Schema(format = "uuid") UUID documentId /* 字段作用：对应对象的唯一标识。 */,
        String filename /* 字段作用：用户上传时的原始文件名。 */,
        String headingPath /* 字段作用：该不可变对象携带的 headingPath 数据。 */,
        String childText /* 字段作用：本次流程使用的文本内容。 */,
        String parentText /* 字段作用：本次流程使用的文本内容。 */,
        double score /* 字段作用：相关性或质量分数，含义由当前算法定义。 */) { // 类型作用：响应 DTO，规定接口返回给前端的 JSON 字段。

    public static SemanticSearchHitResponse from(SemanticSearchHit hit) { // 方法作用：把内部领域对象转换为对外响应 DTO。
        return new SemanticSearchHitResponse(
                hit.chunkId(), hit.parentId(), hit.documentId(), hit.filename(), hit.headingPath(),
                hit.childText(), hit.parentText(), hit.score());
    }
}
