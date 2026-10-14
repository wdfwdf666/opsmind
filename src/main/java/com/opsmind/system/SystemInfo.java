package com.opsmind.system; // 包作用：系统模块，提供应用基本信息与运行状态接口。

public record SystemInfo(
        String application /* 字段作用：该不可变对象携带的 application 数据。 */,
        String status /* 字段作用：当前业务状态。 */,
        String javaVersion /* 字段作用：该不可变对象携带的 javaVersion 数据。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。
}

