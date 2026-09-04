package com.demo.meshpay.saga.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Custom metrics for Saga operations
 * Tracks business metrics for monitoring and alerting
 */
@Component
public class SagaMetrics {

    private final MeterRegistry meterRegistry;
    private final AtomicLong activeSagas = new AtomicLong(0);

    // Counters
    private final Counter sagaStartedCounter;
    private final Counter sagaCompletedCounter;
    private final Counter sagaCompensatedCounter;

    // Timers
    private final Timer sagaExecutionTimer;

    public SagaMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Initialize counters
        this.sagaStartedCounter = Counter.builder("saga.started.total")
                .description("Total number of sagas started")
                .register(meterRegistry);

        this.sagaCompletedCounter = Counter.builder("saga.completed.total")
                .description("Total number of sagas completed successfully")
                .register(meterRegistry);

        this.sagaCompensatedCounter = Counter.builder("saga.compensated.total")
                .description("Total number of sagas compensated")
                .register(meterRegistry);

        // Initialize timers
        this.sagaExecutionTimer = Timer.builder("saga.execution.duration")
                .description("Time taken to execute saga")
                .register(meterRegistry);

        // Initialize gauges
        Gauge.builder("saga.active.count", activeSagas, AtomicLong::get)
                .description("Number of active sagas")
                .register(meterRegistry);
    }

    // Counter increment methods
    public void incrementSagaStarted() {
        sagaStartedCounter.increment();
    }

    public void incrementSagaCompleted() {
        sagaCompletedCounter.increment();
    }

    public void incrementSagaCompensated() {
        sagaCompensatedCounter.increment();
    }

    // Timer recording methods
    public Timer.Sample startSagaExecutionTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopSagaExecutionTimer(Timer.Sample sample) {
        sample.stop(sagaExecutionTimer);
    }

    // Gauge update methods
    public void incrementActiveSagas() {
        activeSagas.incrementAndGet();
    }

    public void decrementActiveSagas() {
        activeSagas.decrementAndGet();
    }

    public long getActiveSagas() {
        return activeSagas.get();
    }
}
