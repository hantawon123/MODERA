package com.ssafy.modera.worker.domain.event;

import com.ssafy.modera.contract.EventEnvelope;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class EventPerformanceMetrics {
    private final MeterRegistry registry;

    public void record(EventEnvelope envelope, long processingNanos, String outcome) {
        String type = envelope.eventType() == null ? "UNKNOWN" : envelope.eventType();
        Timer.builder("modera.worker.event.processing.duration").tag("type", type).tag("outcome", outcome)
                .publishPercentileHistogram().register(registry)
                .record(processingNanos, TimeUnit.NANOSECONDS);
        Counter.builder("modera.worker.events").tag("type", type).tag("outcome", outcome)
                .register(registry).increment();
        try {
            Duration wait = Duration.between(OffsetDateTime.parse(envelope.occurredAt()), OffsetDateTime.now());
            if (!wait.isNegative()) {
                Timer.builder("modera.worker.event.queue.delay").tag("type", type)
                        .publishPercentileHistogram().register(registry).record(wait);
            }
        } catch (RuntimeException ignored) {
            // A malformed timestamp must not affect event processing.
        }
    }

    public void recordAck(String type) {
        Counter.builder("modera.worker.event.acks").tag("type", type == null ? "UNKNOWN" : type)
                .register(registry).increment();
    }
}
