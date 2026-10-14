package com.opsmind.system; // 包作用：系统模块，提供应用基本信息与运行状态接口。

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service // 注解作用：把类注册为业务服务，由 Spring 负责创建和注入。
public class SystemInfoService { // 类型作用：应用服务，负责组织一个完整业务流程。

    private final String applicationName;

    public SystemInfoService(@Value("${spring.application.name}") String applicationName) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this.applicationName = applicationName;
    }

    public SystemInfo getSystemInfo() { // 方法作用：按条件读取目标数据，不存在时按约定返回空值或异常。
        return new SystemInfo(applicationName, "UP", Integer.toString(Runtime.version().feature()));
    }
}

