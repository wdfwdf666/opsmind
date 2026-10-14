package com.opsmind.common.api;

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
        String code,
        String message,
        T data,
        Instant timestamp) {

    private static final String SUCCESS_CODE = "OK";
    private static final String SUCCESS_MESSAGE = "success";

    /** 工厂方法：调用方只提供数据，公共成功码与时间由这里统一生成。 */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(SUCCESS_CODE, SUCCESS_MESSAGE, data, Instant.now());
    }

    /** 工厂方法：统一构造失败响应，避免 Controller 重复拼 JSON。 */
    public static <T> ApiResponse<T> failure(String code, String message) {
        return new ApiResponse<>(code, message, null, Instant.now());
    }
}

