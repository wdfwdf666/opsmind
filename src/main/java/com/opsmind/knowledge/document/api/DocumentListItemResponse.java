package com.opsmind.knowledge.document.api; // 包作用：接口层，接收 HTTP/JSON 数据并转换成应用层调用。

import com.opsmind.knowledge.document.domain.DocumentStatus;
import com.opsmind.knowledge.document.domain.DocumentSummary;
import com.opsmind.knowledge.document.domain.IngestionTaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Document list item with ingestion status")
public record DocumentListItemResponse(
        @Schema(format = "uuid") UUID documentId /* 字段作用：对应对象的唯一标识。 */,
        @Schema(format = "uuid", nullable = true) UUID taskId /* 字段作用：对应对象的唯一标识。 */,
        String filename /* 字段作用：用户上传时的原始文件名。 */,
        String mediaType /* 字段作用：文件或响应的 MIME 类型。 */,
        long size /* 字段作用：该不可变对象携带的 size 数据。 */,
        String sha256 /* 字段作用：该不可变对象携带的 sha256 数据。 */,
        DocumentStatus documentStatus /* 字段作用：当前业务状态。 */,
        IngestionTaskStatus taskStatus /* 字段作用：当前业务状态。 */,
        String taskError /* 字段作用：该不可变对象携带的 taskError 数据。 */,
        Instant createdAt /* 字段作用：对象创建时间。 */) { // 类型作用：响应 DTO，规定接口返回给前端的 JSON 字段。

    public static DocumentListItemResponse from(DocumentSummary summary) { // 方法作用：把内部领域对象转换为对外响应 DTO。
        var document = summary.document();
        return new DocumentListItemResponse(
                document.id(), summary.taskId(), document.originalFilename(), document.mediaType(), document.size(),
                document.sha256(), document.status(), summary.taskStatus(), summary.taskError(), document.createdAt());
    }
}
