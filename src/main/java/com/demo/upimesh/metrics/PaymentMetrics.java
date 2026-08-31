package com.demo.upimesh.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Custom metrics for Payment Settlement operations
 * Tracks business metrics for monitoring and alerting
 */
@Component
public class PaymentMetrics {

    private final MeterRegistry meterRegistry;
    private final AtomicLong pendingSettlements = new AtomicLong(0);
    private final AtomicLong activeSagas = new AtomicLong(0);

    // Counters
    private final Counter paymentReceivedCounter;
    private final Counter paymentValidatedCounter;
    private final Counter settlementRequestedCounter;
    private final Counter paymentSettledCounter;
    private final Counter paymentFailedCounter;
    private final Counter sagaStartedCounter;
    private final Counter sagaCompletedCounter;
    private final Counter sagaCompensatedCounter;
    private final Counter dlqMessageCounter;

    // Timers
    private final Timer settlementTimer;
    private final Timer validationTimer;
    private final Timer sagaExecutionTimer;

    public PaymentMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Initialize counters
        this.paymentReceivedCounter = Counter.builder("payment.received.total")
                .description("Total number of payment received events")
                .register(meterRegistry);

        this.paymentValidatedCounter = Counter.builder("payment.validated.total")
                .description("Total number of payment validated events")
                .register(meterRegistry);

        this.settlementRequestedCounter = Counter.builder("settlement.requested.total")
                .description("Total number of settlement requested events")
                .register(meterRegistry);

        this.paymentSettledCounter = Counter.builder("payment.settled.total")
                .description("Total number of successfully settled payments")
                .register(meterRegistry);

        this.paymentFailedCounter = Counter.builder("payment.failed.total")
                .description("Total number of failed payments")
                .register(meterRegistry);

        this.sagaStartedCounter = Counter.builder("saga.started.total")
                .description("Total number of sagas started")
                .register(meterRegistry);

        this.sagaCompletedCounter = Counter.builder("saga.completed.total")
                .description("Total number of sagas completed successfully")
                .register(meterRegistry);

        this.sagaCompensatedCounter = Counter.builder("saga.compensated.total")
                .description("Total number of sagas compensated")
                .register(meterRegistry);

        this.dlqMessageCounter = Counter.builder("kafka.dlq.messages.total")
                .description("Total number of messages sent to DLQ")
                .register(meterRegistry);

        // Initialize timers
        this.settlementTimer = Timer.builder("settlement.duration")
                .description("Time taken to process payment settlement")
                .register(meterRegistry);

        this.validationTimer = Timer.builder("validation.duration")
                .description("Time taken to validate payment")
                .register(meterRegistry);

        this.sagaExecutionTimer = Timer.builder("saga.execution.duration")
                .description("Time taken to execute saga")
                .register(meterRegistry);

        // Initialize gauges
        Gauge.builder("payment.pending.settlements", pendingSettlements, AtomicLong::get)
                .description("Number of pending settlements")
                .register(meterRegistry);

        Gauge.builder("saga.active.count", activeSagas, AtomicLong::get)
                .description("Number of active sagas")
                .register(meterRegistry);
    }

    // Counter increment methods
    public void incrementPaymentReceived() {
        paymentReceivedCounter.increment();
    }

    public void incrementPaymentValidated() {
        paymentValidatedCounter.increment();
    }

    public void incrementSettlementRequested() {
        settlementRequestedCounter.increment();
    }

    public void incrementPaymentSettled() {
        paymentSettledCounter.increment();
    }

    public void incrementPaymentFailed() {
        paymentFailedCounter.increment();
    }

    public void incrementSagaStarted() {
        sagaStartedCounter.increment();
    }

    public void incrementSagaCompleted() {
        sagaCompletedCounter.increment();
    }

    public void incrementSagaCompensated() {
        sagaCompensatedCounter.increment();
    }

    public void incrementDlqMessage() {
        dlqMessageCounter.increment();
    }

    // Timer recording methods
    public Timer.Sample startSettlementTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopSettlementTimer(Timer.Sample sample) {
        sample.stop(settlementTimer);
    }

    public Timer.Sample startValidationTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopValidationTimer(Timer.Sample sample) {
        sample.stop(validationTimer);
    }

    public Timer.Sample startSagaExecutionTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopSagaExecutionTimer(Timer.Sample sample) {
        sample.stop(sagaExecutionTimer);
    }

    // Gauge update methods
    public void incrementPendingSettlements() {
        pendingSettlements.incrementAndGet();
    }

    public void decrementPendingSettlements() {
        pendingSettlements.decrementAndGet();
    }

    public void incrementActiveSagas() {
        activeSagas.incrementAndGet();
    }

    public void decrementActiveSagas() {
        activeSagas.decrementAndGet();
    }

    public long getPendingSettlements() {
        return pendingSettlements.get();
    }

    public long getActiveSagas() {
        return activeSagas.get();
    }
}
