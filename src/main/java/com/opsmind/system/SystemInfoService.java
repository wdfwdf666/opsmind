package com.opsmind.system;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SystemInfoService {

    private final String applicationName;

    public SystemInfoService(@Value("${spring.application.name}") String applicationName) {
        this.applicationName = applicationName;
    }

    public SystemInfo getSystemInfo() {
        return new SystemInfo(applicationName, "UP", Integer.toString(Runtime.version().feature()));
    }
}

