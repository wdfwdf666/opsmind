package com.opsmind.evaluation.api;

import com.opsmind.common.api.ApiResponse;
import com.opsmind.evaluation.application.RetrievalEvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/evaluations/retrieval")
@Tag(name = "RAG Evaluation", description = "Run versioned retrieval datasets and calculate regression metrics")
public class RetrievalEvaluationController {

    private final RetrievalEvaluationService evaluationService;

    public RetrievalEvaluationController(RetrievalEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping("/run")
    @Operation(
            summary = "Run a fixed retrieval evaluation dataset",
            description = "Runs every query in the versioned dataset. Remote embedding providers may incur cost.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Retrieval quality and latency report"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Unknown evaluation dataset", content = @Content)
    })
    public ApiResponse<RetrievalEvaluationResponse> run(@RequestBody RetrievalEvaluationRequest request) {
        return ApiResponse.success(RetrievalEvaluationResponse.from(
                evaluationService.run(request.dataset(), request.documentId(), request.strategy())));
    }
}
