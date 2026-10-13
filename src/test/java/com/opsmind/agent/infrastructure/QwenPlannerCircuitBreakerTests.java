package com.opsmind.agent.infrastructure;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class QwenPlannerCircuitBreakerTests {

    @Test
    void shouldAllowSingleHalfOpenProbeAndCloseAfterSuccess() {
        MutableClock clock = new MutableClock();
        QwenPlannerCircuitBreaker circuitBreaker = new QwenPlannerCircuitBreaker(2, 1000, clock);

        assertThat(circuitBreaker.tryAcquirePermission()).isTrue();
        circuitBreaker.onFailure();
        assertThat(circuitBreaker.tryAcquirePermission()).isTrue();
        circuitBreaker.onFailure();

        assertThat(circuitBreaker.state()).isEqualTo(QwenPlannerCircuitBreaker.State.OPEN);
        assertThat(circuitBreaker.tryAcquirePermission()).isFalse();

        clock.advanceMillis(1000);
        assertThat(circuitBreaker.tryAcquirePermission()).isTrue();
        assertThat(circuitBreaker.state()).isEqualTo(QwenPlannerCircuitBreaker.State.HALF_OPEN);
        assertThat(circuitBreaker.tryAcquirePermission()).isFalse();

        circuitBreaker.onSuccess();
        assertThat(circuitBreaker.state()).isEqualTo(QwenPlannerCircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.tryAcquirePermission()).isTrue();
    }

    private static final class MutableClock extends Clock {

        private Instant instant = Instant.parse("2026-10-13T00:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        void advanceMillis(long milliseconds) {
            instant = instant.plusMillis(milliseconds);
        }
    }
}
