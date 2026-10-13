package com.opsmind.agent.api;

import com.opsmind.agent.application.AgentRunService;
import com.opsmind.agent.domain.AgentApprovalStatus;
import com.opsmind.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agent/runs")
@Tag(name = "Agent Runs", description = "Execute and inspect auditable Agent state-machine runs")
public class AgentRunController {

    private final AgentRunService agentRunService;

    public AgentRunController(AgentRunService agentRunService) {
        this.agentRunService = agentRunService;
    }

    @PostMapping
    @Operation(
            summary = "Start an Agent run",
            description = "Omit taskType for Planner routing. KNOWLEDGE_QA completes automatically; CREATE_TICKET waits for approval.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = {
                    @ExampleObject(
                            name = "Automatic knowledge answer",
                            value = "{\"query\":\"数据库连接池告警应该检查什么？\"}"),
                    @ExampleObject(
                            name = "Create ticket draft",
                            value = "{\"query\":\"数据库连接池持续告警，请创建 P2 工单\"}")
            }))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Completed or waiting-approval run"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid query", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "Tool execution failed", content = @Content)
    })
    public ApiResponse<AgentRunResponse> start(@RequestBody AgentRunRequest request) {
        return ApiResponse.success(AgentRunResponse.from(
                agentRunService.start(request.query(), request.documentId(), request.taskType())));
    }

    @PostMapping("/{runId}/approval")
    @Operation(
            summary = "Approve or reject a pending write tool",
            description = "APPROVE creates exactly one support ticket; repeating the same decision is idempotent.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Decision applied"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid decision", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Run not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Decision conflicts with run state", content = @Content)
    })
    public ApiResponse<AgentRunResponse> decide(
            @PathVariable UUID runId,
            @RequestBody AgentApprovalRequest request) {
        AgentApprovalStatus decision = request.decision() == null
                ? null
                : request.decision() == AgentApprovalDecision.APPROVE
                ? AgentApprovalStatus.APPROVED
                : AgentApprovalStatus.REJECTED;
        return ApiResponse.success(AgentRunResponse.from(agentRunService.decide(runId, decision, request.reason())));
    }

    @GetMapping("/{runId}")
    @Operation(summary = "Get an Agent run and its ordered audit steps")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Agent run"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Run not found", content = @Content)
    })
    public ApiResponse<AgentRunResponse> get(@PathVariable UUID runId) {
        return ApiResponse.success(AgentRunResponse.from(agentRunService.get(runId)));
    }
}
