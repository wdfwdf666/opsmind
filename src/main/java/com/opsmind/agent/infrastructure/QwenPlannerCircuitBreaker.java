package com.opsmind.agent.infrastructure; // 包作用：基础设施层，实现数据库、文件系统和外部模型调用。

import java.time.Clock;

/**
 * 一个小型线程安全熔断器。
 * <p>CLOSED 正常放行；连续失败达到阈值后进入 OPEN；等待窗口结束后进入 HALF_OPEN，
 * 只用一次试探请求判断远程服务是否恢复。</p>
 */
final class QwenPlannerCircuitBreaker { // 类型作用：Qwen 适配器，负责请求组装、远程调用和响应校验。

    enum State { // 类型作用：枚举 State 集中列出允许的有限状态，避免随意使用字符串。
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    private final int failureThreshold;
    private final long openDurationMs;
    private final Clock clock;

    private State state = State.CLOSED;
    private int consecutiveFailures;
    private long openUntilEpochMs;

    QwenPlannerCircuitBreaker(int failureThreshold, long openDurationMs, Clock clock) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this.failureThreshold = failureThreshold;
        this.openDurationMs = openDurationMs;
        this.clock = clock;
    }

    // synchronized 保证多个 Web 请求线程不会同时破坏状态和失败计数。
    synchronized boolean tryAcquirePermission() { // 方法作用：根据熔断状态决定本次远程请求是否放行。
        if (state == State.CLOSED) {
            return true;
        }
        // 冷却时间到后放行一个探测请求；其成功/失败决定回到 CLOSED 或 OPEN。
        if (state == State.OPEN && clock.millis() >= openUntilEpochMs) {
            state = State.HALF_OPEN;
            return true;
        }
        return false;
    }

    synchronized void onSuccess() { // 方法作用：远程调用成功后清零失败次数并关闭熔断。
        consecutiveFailures = 0;
        openUntilEpochMs = 0;
        state = State.CLOSED;
    }

    synchronized void onFailure() { // 方法作用：记录连续失败，达到阈值后打开熔断。
        consecutiveFailures++;
        if (state == State.HALF_OPEN || consecutiveFailures >= failureThreshold) {
            state = State.OPEN;
            openUntilEpochMs = clock.millis() + openDurationMs;
        }
    }

    synchronized State state() { // 方法作用：完成“state”对应的内部步骤，供主流程复用。
        return state;
    }

    synchronized double stateValue() { // 方法作用：完成“stateValue”对应的内部步骤，供主流程复用。
        // Prometheus Gauge 只能记录数字，因此把三种状态编码为 0、0.5、1。
        return switch (state) {
            case CLOSED -> 0;
            case HALF_OPEN -> 0.5;
            case OPEN -> 1;
        };
    }
}
