package com.opsmind.evaluation.api; // 包作用：接口层，接收 HTTP/JSON 数据并转换成应用层调用。

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

@RestController // 注解作用：把类注册为 REST 控制器，返回对象会自动转换成 JSON。
@RequestMapping("/api/v1/evaluations/agent-routing") // 注解作用：声明该控制器所有接口共用的 URL 前缀。
@Tag(name = "Agent Evaluation", description = "Run versioned Agent routing regression datasets")
public class AgentRoutingEvaluationController { // 类型作用：HTTP 控制器，只负责收参、调用 Service 和返回 JSON。

    private final AgentRoutingEvaluationService evaluationService;

    public AgentRoutingEvaluationController(AgentRoutingEvaluationService evaluationService) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this.evaluationService = evaluationService;
    }

    @PostMapping("/run") // 注解作用：把 HTTP POST 请求映射到下面的方法。
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
            @RequestBody(required = false) AgentRoutingEvaluationRequest request) { // 方法作用：加载固定评测集，逐条执行并汇总质量与延迟指标。
        String dataset = request == null ? null : request.dataset();
        return ApiResponse.success(AgentRoutingEvaluationResponse.from(evaluationService.run(dataset)));
    }
}
