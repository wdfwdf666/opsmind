package com.opsmind.evaluation.api;

import com.opsmind.common.api.ApiResponse;
import com.opsmind.evaluation.application.AnswerEvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/evaluations/answers")
@Tag(name = "RAG Evaluation", description = "Run versioned retrieval and answer regression datasets")
public class AnswerEvaluationController {

    private final AnswerEvaluationService evaluationService;

    public AnswerEvaluationController(AnswerEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping("/run")
    @Operation(
            summary = "Run a fixed grounded-answer evaluation dataset",
            description = "Measures expected-fact coverage, citation precision/recall, refusal quality, latency, tokens, "
                    + "and a deterministic lexical faithfulness proxy. Remote providers may incur cost.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Grounded-answer quality and latency report"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid topK", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Unknown evaluation dataset", content = @Content)
    })
    public ApiResponse<AnswerEvaluationResponse> run(@RequestBody AnswerEvaluationRequest request) {
        return ApiResponse.success(AnswerEvaluationResponse.from(
                evaluationService.run(request.dataset(), request.documentId(), request.topK())));
    }
}
