package com.opsmind.knowledge.document.api; // 包作用：接口层，接收 HTTP/JSON 数据并转换成应用层调用。

import com.opsmind.knowledge.document.application.DocumentUploadResult;
import com.opsmind.knowledge.document.application.DocumentIngestionResult;
import com.opsmind.knowledge.document.domain.DocumentStatus;
import com.opsmind.knowledge.document.domain.IngestionTaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Uploaded document and ingestion task status")
public record DocumentResponse(
        @Schema(format = "uuid") UUID documentId /* 字段作用：对应对象的唯一标识。 */,
        @Schema(format = "uuid") UUID taskId /* 字段作用：对应对象的唯一标识。 */,
        @Schema(example = "incident-runbook.pdf") String filename /* 字段作用：用户上传时的原始文件名。 */,
        @Schema(example = "application/pdf") String mediaType /* 字段作用：文件或响应的 MIME 类型。 */,
        @Schema(description = "File size in bytes", example = "204800") long size /* 字段作用：该不可变对象携带的 size 数据。 */,
        @Schema(description = "SHA-256 checksum", example = "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9") String sha256 /* 字段作用：该不可变对象携带的 sha256 数据。 */,
        DocumentStatus documentStatus /* 字段作用：当前业务状态。 */,
        IngestionTaskStatus taskStatus /* 字段作用：当前业务状态。 */,
        @Schema(description = "Processing error, present only when the task failed") String taskError /* 字段作用：该不可变对象携带的 taskError 数据。 */,
        @Schema(description = "Whether an existing document with identical content was returned") boolean duplicate /* 字段作用：该不可变对象携带的 duplicate 数据。 */,
        Instant createdAt /* 字段作用：对象创建时间。 */) { // 类型作用：响应 DTO，规定接口返回给前端的 JSON 字段。

    public static DocumentResponse from(DocumentUploadResult result) { // 方法作用：把内部领域对象转换为对外响应 DTO。
        return new DocumentResponse(
                result.document().id(),
                result.task().id(),
                result.document().originalFilename(),
                result.document().mediaType(),
                result.document().size(),
                result.document().sha256(),
                result.document().status(),
                result.task().status(),
                result.task().errorMessage(),
                result.duplicate(),
                result.document().createdAt());
    }

    public static DocumentResponse from(DocumentIngestionResult result) { // 方法作用：把内部领域对象转换为对外响应 DTO。
        return new DocumentResponse(
                result.document().id(),
                result.task().id(),
                result.document().originalFilename(),
                result.document().mediaType(),
                result.document().size(),
                result.document().sha256(),
                result.document().status(),
                result.task().status(),
                result.task().errorMessage(),
                false,
                result.document().createdAt());
    }
}

