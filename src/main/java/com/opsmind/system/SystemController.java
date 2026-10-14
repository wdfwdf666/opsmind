package com.opsmind.system;

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
@RestController
@RequestMapping("/api/v1/system")
@Tag(name = "System", description = "Application status and runtime information")
public class SystemController {

    private final SystemInfoService systemInfoService;

    public SystemController(SystemInfoService systemInfoService) {
        this.systemInfoService = systemInfoService;
    }

    @GetMapping("/info")
    @Operation(summary = "Get application information", description = "Returns the application name, status and Java version.")
    public ApiResponse<SystemInfo> getSystemInfo() {
        return ApiResponse.success(systemInfoService.getSystemInfo());
    }
}

