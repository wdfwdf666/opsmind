package com.opsmind.rag.api;

import com.opsmind.common.api.ApiResponse;
import com.opsmind.rag.application.RagAnswerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rag")
@Tag(name = "RAG Answers", description = "Generate grounded answers from reranked internal evidence")
public class RagAnswerController {

    private final RagAnswerService answerService;

    public RagAnswerController(RagAnswerService answerService) {
        this.answerService = answerService;
    }

    @PostMapping("/answer")
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
    public ApiResponse<RagAnswerResponse> answer(@RequestBody RagAnswerRequest request) {
        return ApiResponse.success(RagAnswerResponse.from(
                answerService.answer(request.query(), request.topK(), request.documentId())));
    }
}
