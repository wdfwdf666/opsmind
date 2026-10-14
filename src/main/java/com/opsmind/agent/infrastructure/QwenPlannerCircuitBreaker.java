package com.opsmind.agent.infrastructure;

import java.time.Clock;

/**
 * 一个小型线程安全熔断器。
 * <p>CLOSED 正常放行；连续失败达到阈值后进入 OPEN；等待窗口结束后进入 HALF_OPEN，
 * 只用一次试探请求判断远程服务是否恢复。</p>
 */
final class QwenPlannerCircuitBreaker {

    enum State {
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

    QwenPlannerCircuitBreaker(int failureThreshold, long openDurationMs, Clock clock) {
        this.failureThreshold = failureThreshold;
        this.openDurationMs = openDurationMs;
        this.clock = clock;
    }

    // synchronized 保证多个 Web 请求线程不会同时破坏状态和失败计数。
    synchronized boolean tryAcquirePermission() {
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

    synchronized void onSuccess() {
        consecutiveFailures = 0;
        openUntilEpochMs = 0;
        state = State.CLOSED;
    }

    synchronized void onFailure() {
        consecutiveFailures++;
        if (state == State.HALF_OPEN || consecutiveFailures >= failureThreshold) {
            state = State.OPEN;
            openUntilEpochMs = clock.millis() + openDurationMs;
        }
    }

    synchronized State state() {
        return state;
    }

    synchronized double stateValue() {
        // Prometheus Gauge 只能记录数字，因此把三种状态编码为 0、0.5、1。
        return switch (state) {
            case CLOSED -> 0;
            case HALF_OPEN -> 0.5;
            case OPEN -> 1;
        };
    }
}
