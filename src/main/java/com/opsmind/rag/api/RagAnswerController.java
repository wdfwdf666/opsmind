package com.opsmind.rag.api; // 包作用：接口层，接收 HTTP/JSON 数据并转换成应用层调用。

import com.opsmind.common.api.ApiResponse;
import com.opsmind.rag.application.RagAnswerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RAG 问答入口。默认在全部文档中自动检索；只有调用方传入 documentId 时才限定单文档。
 */
@RestController // 注解作用：把类注册为 REST 控制器，返回对象会自动转换成 JSON。
@RequestMapping("/api/v1/rag") // 注解作用：声明该控制器所有接口共用的 URL 前缀。
@Tag(name = "RAG Answers", description = "Generate grounded answers from reranked internal evidence")
public class RagAnswerController { // 类型作用：HTTP 控制器，只负责收参、调用 Service 和返回 JSON。

    private final RagAnswerService answerService;

    public RagAnswerController(RagAnswerService answerService) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this.answerService = answerService;
    }

    @PostMapping("/answer") // 注解作用：把 HTTP POST 请求映射到下面的方法。
    @Operation(
            summary = "Generate a grounded answer with citations",
            description = "Runs reranked retrieval, applies a context budget, calls the configured Qwen chat model, "
                    + "and returns only citations actually referenced by the answer.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Grounded answer or deterministic no-evidence refusal"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid query or topK", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502", description = "Model failed or returned invalid citations", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "503", description = "Model provider is not configured", content = @Content)
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(
                    name = "Automatic document selection",
                    value = "{\"query\":\"数据库连接池告警应该检查什么？\",\"topK\":5}")))
    public ApiResponse<RagAnswerResponse> answer(
            // @RequestBody 让 Jackson 把请求 JSON 反序列化为 RagAnswerRequest record。
            @RequestBody RagAnswerRequest request) { // 方法作用：执行检索、证据门禁、上下文组装、模型生成和引用校验。
        return ApiResponse.success(RagAnswerResponse.from(
                answerService.answer(request.query(), request.topK(), request.documentId())));
    }
}
