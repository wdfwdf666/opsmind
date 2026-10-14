package com.opsmind.agent.api; // 包作用：接口层，接收 HTTP/JSON 数据并转换成应用层调用。

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

/**
 * Agent 状态机的 HTTP 入口：启动任务、人工审批、查询完整审计轨迹。
 */
@RestController // 注解作用：把类注册为 REST 控制器，返回对象会自动转换成 JSON。
@RequestMapping("/api/v1/agent/runs") // 注解作用：声明该控制器所有接口共用的 URL 前缀。
@Tag(name = "Agent Runs", description = "Execute and inspect auditable Agent state-machine runs")
public class AgentRunController { // 类型作用：HTTP 控制器，只负责收参、调用 Service 和返回 JSON。

    private final AgentRunService agentRunService;

    public AgentRunController(AgentRunService agentRunService) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this.agentRunService = agentRunService;
    }

    @PostMapping // 注解作用：把 HTTP POST 请求映射到下面的方法。
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
    public ApiResponse<AgentRunResponse> start(@RequestBody AgentRunRequest request) { // 方法作用：创建 Agent Run，并按规划结果进入受控执行流程。
        return ApiResponse.success(AgentRunResponse.from(
                agentRunService.start(request.query(), request.documentId(), request.taskType())));
    }

    @PostMapping("/{runId}/approval") // 注解作用：把 HTTP POST 请求映射到下面的方法。
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
            @RequestBody AgentApprovalRequest request) { // 方法作用：处理人工批准或拒绝；批准后才执行写工具。
        // API 使用 APPROVE/REJECT，领域层使用 APPROVED/REJECTED；在边界处完成协议转换。
        AgentApprovalStatus decision = request.decision() == null
                ? null
                : request.decision() == AgentApprovalDecision.APPROVE
                ? AgentApprovalStatus.APPROVED
                : AgentApprovalStatus.REJECTED;
        return ApiResponse.success(AgentRunResponse.from(agentRunService.decide(runId, decision, request.reason())));
    }

    @GetMapping("/{runId}") // 注解作用：把 HTTP GET 请求映射到下面的方法。
    @Operation(summary = "Get an Agent run and its ordered audit steps")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Agent run"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Run not found", content = @Content)
    })
    public ApiResponse<AgentRunResponse> get(@PathVariable UUID runId) { // 方法作用：按条件读取目标数据，不存在时按约定返回空值或异常。
        return ApiResponse.success(AgentRunResponse.from(agentRunService.get(runId)));
    }
}
