package com.opsmind.system; // 包作用：系统模块，提供应用基本信息与运行状态接口。

import com.opsmind.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 最简单的 Spring MVC 示例，可从这里开始学习调用链。
 * <p>浏览器请求 → SystemController → SystemInfoService → ApiResponse → Jackson JSON。</p>
 */
@RestController // 注解作用：把类注册为 REST 控制器，返回对象会自动转换成 JSON。
@RequestMapping("/api/v1/system") // 注解作用：声明该控制器所有接口共用的 URL 前缀。
@Tag(name = "System", description = "Application status and runtime information")
public class SystemController { // 类型作用：HTTP 控制器，只负责收参、调用 Service 和返回 JSON。

    private final SystemInfoService systemInfoService;

    public SystemController(SystemInfoService systemInfoService) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this.systemInfoService = systemInfoService;
    }

    @GetMapping("/info") // 注解作用：把 HTTP GET 请求映射到下面的方法。
    @Operation(summary = "Get application information", description = "Returns the application name, status and Java version.")
    public ApiResponse<SystemInfo> getSystemInfo() { // 方法作用：按条件读取目标数据，不存在时按约定返回空值或异常。
        return ApiResponse.success(systemInfoService.getSystemInfo());
    }
}

