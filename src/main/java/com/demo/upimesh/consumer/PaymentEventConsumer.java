package com.demo.upimesh.consumer;

import com.demo.upimesh.events.EventEnvelope;
import com.demo.upimesh.model.EventProcessed;
import com.demo.upimesh.repository.EventProcessedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Concrete implementation of IdempotentCounter for payment events
 * Handles idempotency using the EventProcessed table
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer extends IdempotentConsumer {

    private final EventProcessedRepository eventProcessedRepository;

    @Override
    protected void handleEvent(EventEnvelope eventEnvelope, String topic) throws Exception {
        // Extract event type and dispatch to appropriate handler
        String eventType = eventEnvelope.getEventType();
        
        log.info("Handling event: eventType={}, aggregateId={}", eventType, eventEnvelope.getAggregateId());
        
        // Dispatch based on event type
        switch (eventType) {
            case "PAYMENT_RECEIVED":
                handlePaymentReceived(eventEnvelope);
                break;
            case "PAYMENT_VALIDATED":
                handlePaymentValidated(eventEnvelope);
                break;
            case "PAYMENT_SETTLEMENT_REQUESTED":
                handlePaymentSettlementRequested(eventEnvelope);
                break;
            case "PAYMENT_SETTLED":
                handlePaymentSettled(eventEnvelope);
                break;
            case "PAYMENT_FAILED":
                handlePaymentFailed(eventEnvelope);
                break;
            default:
                log.warn("Unknown event type: {}", eventType);
        }
    }

    @Override
    protected boolean isEventProcessed(String eventId) {
        return eventProcessedRepository.existsByEventId(eventId);
    }

    @Override
    protected void markEventAsProcessed(String eventId) {
        // This is handled in the consumeEvent method after successful processing
        // The actual record will be created with full metadata
    }

    /**
     * Record that an event has been processed with full metadata
     */
    public void recordEventProcessed(EventEnvelope eventEnvelope, String topic, Integer partition, Long offset, String consumerGroup) {
        EventProcessed eventProcessed = EventProcessed.builder()
                .eventId(eventEnvelope.getEventId())
                .eventType(eventEnvelope.getEventType())
                .aggregateId(eventEnvelope.getAggregateId())
                .packetHash(eventEnvelope.getPacketHash())
                .topic(topic)
                .partition(partition)
                .offset(offset)
                .consumerGroup(consumerGroup)
                .processedAt(Instant.now())
                .status("SUCCESS")
                .build();
        
        eventProcessedRepository.save(eventProcessed);
        log.debug("Recorded event as processed: eventId={}", eventEnvelope.getEventId());
    }

    /**
     * Handle PAYMENT_RECEIVED event
     */
    private void handlePaymentReceived(EventEnvelope eventEnvelope) {
        log.info("Processing PAYMENT_RECEIVED event: aggregateId={}", eventEnvelope.getAggregateId());
        // TODO: Implement payment received logic
        // This could trigger validation, store payment record, etc.
    }

    /**
     * Handle PAYMENT_VALIDATED event
     */
    private void handlePaymentValidated(EventEnvelope eventEnvelope) {
        log.info("Processing PAYMENT_VALIDATED event: aggregateId={}", eventEnvelope.getAggregateId());
        // TODO: Implement payment validated logic
        // This could trigger settlement request, update payment status, etc.
    }

    /**
     * Handle PAYMENT_SETTLEMENT_REQUESTED event
     */
    private void handlePaymentSettlementRequested(EventEnvelope eventEnvelope) {
        log.info("Processing PAYMENT_SETTLEMENT_REQUESTED event: aggregateId={}", eventEnvelope.getAggregateId());
        // TODO: Implement settlement request logic
        // This could initiate settlement with external systems
    }

    /**
     * Handle PAYMENT_SETTLED event
     */
    private void handlePaymentSettled(EventEnvelope eventEnvelope) {
        log.info("Processing PAYMENT_SETTLED event: aggregateId={}", eventEnvelope.getAggregateId());
        // TODO: Implement payment settled logic
        // This could update payment status, notify users, etc.
    }

    /**
     * Handle PAYMENT_FAILED event
     */
    private void handlePaymentFailed(EventEnvelope eventEnvelope) {
        log.info("Processing PAYMENT_FAILED event: aggregateId={}", eventEnvelope.getAggregateId());
        // TODO: Implement payment failed logic
        // This could trigger retry, notify users, update status, etc.
    }
}
