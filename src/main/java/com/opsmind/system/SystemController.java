package com.opsmind.system;

import com.opsmind.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

