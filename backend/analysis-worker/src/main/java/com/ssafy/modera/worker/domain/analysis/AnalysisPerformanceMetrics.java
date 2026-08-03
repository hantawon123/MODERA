package com.ssafy.modera.worker.domain.analysis;

import com.ssafy.modera.worker.domain.analysis.entity.AnalysisJob;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;

/** Records end-to-end and AI-stage timings for asynchronous image analysis. */
@Component
@RequiredArgsConstructor
public class AnalysisPerformanceMetrics {

    private final MeterRegistry meterRegistry;

    public void recordAiRequest(long elapsedNanos, String outcome) {
        Timer.builder("modera.analysis.ai.request.duration")
                .description("Time required for the worker to submit an analysis request")
                .tag("outcome", normalize(outcome))
                .publishPercentileHistogram()
                .register(meterRegistry)
                .record(elapsedNanos, TimeUnit.NANOSECONDS);
        Counter.builder("modera.analysis.ai.requests")
                .tag("outcome", normalize(outcome))
                .register(meterRegistry)
                .increment();
    }

    public void recordCallback(AnalysisJob job, String resultStatus) {
        OffsetDateTime completedAt = OffsetDateTime.now();
        String status = normalize(resultStatus);
        recordDuration("modera.analysis.pipeline.duration", job.getQueuedAt(), completedAt, status);
        recordDuration("modera.analysis.ai.duration", job.getStartedAt(), completedAt, status);
        Counter.builder("modera.analysis.results")
                .description("Analysis callbacks grouped by semantic result status")
                .tag("status", status)
                .register(meterRegistry)
                .increment();
    }

    public void recordCallbackPersistenceFailure() {
        Counter.builder("modera.analysis.callback.persistence.failures")
                .description("Analysis callbacks that the worker could not persist")
                .register(meterRegistry)
                .increment();
    }

    private void recordDuration(String name, OffsetDateTime start, OffsetDateTime end, String status) {
        if (start == null || end.isBefore(start)) {
            return;
        }
        Timer.builder(name)
                .tag("status", status)
                .publishPercentileHistogram()
                .register(meterRegistry)
                .record(Duration.between(start, end));
    }

    private String normalize(String status) {
        return status == null || status.isBlank() ? "UNKNOWN" : status.trim().toUpperCase();
    }
}
