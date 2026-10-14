package com.opsmind.knowledge.document.api; // 包作用：接口层，接收 HTTP/JSON 数据并转换成应用层调用。

import com.opsmind.knowledge.document.domain.DocumentPage;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Page of documents ordered by creation time descending")
public record DocumentPageResponse(
        List<DocumentListItemResponse> items /* 字段作用：该不可变对象携带的 items 数据。 */,
        int page /* 字段作用：分页位置或分页结果。 */,
        int size /* 字段作用：该不可变对象携带的 size 数据。 */,
        long totalElements /* 字段作用：该不可变对象携带的 totalElements 数据。 */,
        long totalPages /* 字段作用：分页位置或分页结果。 */) { // 类型作用：响应 DTO，规定接口返回给前端的 JSON 字段。

    public static DocumentPageResponse from(DocumentPage result) { // 方法作用：把内部领域对象转换为对外响应 DTO。
        return new DocumentPageResponse(
                result.items().stream().map(DocumentListItemResponse::from).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages());
    }
}
