package com.opsmind.agent.infrastructure;

import java.time.Clock;

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

    synchronized boolean tryAcquirePermission() {
        if (state == State.CLOSED) {
            return true;
        }
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
        return switch (state) {
            case CLOSED -> 0;
            case HALF_OPEN -> 0.5;
            case OPEN -> 1;
        };
    }
}
