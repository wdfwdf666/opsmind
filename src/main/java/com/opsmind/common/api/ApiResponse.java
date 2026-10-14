package com.opsmind.common.api; // 包作用：接口层，接收 HTTP/JSON 数据并转换成应用层调用。

import java.time.Instant;

/**
 * 所有业务接口统一使用的 JSON 外壳。
 * <p>{@code record} 是 Java 的不可变数据类语法，编译器会自动生成构造器、访问方法、
 * equals、hashCode 和 toString。泛型 {@code <T>} 表示 data 可以是任意响应类型。</p>
 *
 * @param code 机器可读的结果码，成功固定为 OK
 * @param message 给调用方看的简短说明
 * @param data 真正的业务数据；失败时为 null
 * @param timestamp 服务端创建响应的 UTC 时间
 * @param <T> data 字段的实际 Java 类型
 */
public record ApiResponse<T>(
        String code /* 字段作用：该不可变对象携带的 code 数据。 */,
        String message /* 字段作用：该不可变对象携带的 message 数据。 */,
        T data /* 字段作用：该不可变对象携带的 data 数据。 */,
        Instant timestamp /* 字段作用：该不可变对象携带的 timestamp 数据。 */) { // 类型作用：响应 DTO，规定接口返回给前端的 JSON 字段。

    private static final String SUCCESS_CODE = "OK";
    private static final String SUCCESS_MESSAGE = "success";

    /** 工厂方法：调用方只提供数据，公共成功码与时间由这里统一生成。 */
    public static <T> ApiResponse<T> success(T data) { // 方法作用：完成“success”对应的内部步骤，供主流程复用。
        return new ApiResponse<>(SUCCESS_CODE, SUCCESS_MESSAGE, data, Instant.now());
    }

    /** 工厂方法：统一构造失败响应，避免 Controller 重复拼 JSON。 */
    public static <T> ApiResponse<T> failure(String code, String message) { // 方法作用：完成“failure”对应的内部步骤，供主流程复用。
        return new ApiResponse<>(code, message, null, Instant.now());
    }
}

