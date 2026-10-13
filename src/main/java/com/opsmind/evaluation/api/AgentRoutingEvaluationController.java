package com.opsmind.evaluation.api;

import com.opsmind.common.api.ApiResponse;
import com.opsmind.evaluation.application.AgentRoutingEvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/evaluations/agent-routing")
@Tag(name = "Agent Evaluation", description = "Run versioned Agent routing regression datasets")
public class AgentRoutingEvaluationController {

    private final AgentRoutingEvaluationService evaluationService;

    public AgentRoutingEvaluationController(AgentRoutingEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping("/run")
    @Operation(
            summary = "Run an Agent Planner routing evaluation",
            description = "Calls only the Planner and never executes tools. Remote Planner providers may incur cost.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Routing quality and latency report"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Unknown evaluation dataset", content = @Content)
    })
    public ApiResponse<AgentRoutingEvaluationResponse> run(
            @RequestBody(required = false) AgentRoutingEvaluationRequest request) {
        String dataset = request == null ? null : request.dataset();
        return ApiResponse.success(AgentRoutingEvaluationResponse.from(evaluationService.run(dataset)));
    }
}
