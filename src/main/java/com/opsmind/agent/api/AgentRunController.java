package com.opsmind.agent.api;

import com.opsmind.agent.application.AgentRunService;
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
    @Operation(summary = "Start a knowledge Agent run", description = "Automatically executes the read-only knowledge_search tool.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(
                    name = "Automatic document selection",
                    value = "{\"query\":\"数据库连接池告警应该检查什么？\"}")))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Completed run and audit steps"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid query", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "Tool execution failed", content = @Content)
    })
    public ApiResponse<AgentRunResponse> start(@RequestBody AgentRunRequest request) {
        return ApiResponse.success(AgentRunResponse.from(agentRunService.start(request.query(), request.documentId())));
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
