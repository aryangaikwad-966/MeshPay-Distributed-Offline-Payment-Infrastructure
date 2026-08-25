package com.demo.upimesh.service;

import com.demo.upimesh.events.EventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka Producer Service for publishing events
 * Handles event publication with error handling and logging
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Topic names
    private static final String PAYMENT_RECEIVED_TOPIC = "payment-received";
    private static final String PAYMENT_VALIDATED_TOPIC = "payment-validated";
    private static final String PAYMENT_SETTLEMENT_REQUESTED_TOPIC = "payment-settlement-requested";
    private static final String PAYMENT_SETTLED_TOPIC = "payment-settled";
    private static final String PAYMENT_FAILED_TOPIC = "payment-failed";
    private static final String PAYMENT_RETRY_TOPIC = "payment-retry";
    private static final String PAYMENT_DLQ_TOPIC = "payment-dlq";

    /**
     * Publish payment received event
     */
    public CompletableFuture<SendResult<String, Object>> publishPaymentReceived(EventEnvelope event) {
        return publishEvent(PAYMENT_RECEIVED_TOPIC, event.getPacketHash(), event);
    }

    /**
     * Publish payment validated event
     */
    public CompletableFuture<SendResult<String, Object>> publishPaymentValidated(EventEnvelope event) {
        return publishEvent(PAYMENT_VALIDATED_TOPIC, event.getPacketHash(), event);
    }

    /**
     * Publish payment settlement requested event
     */
    public CompletableFuture<SendResult<String, Object>> publishPaymentSettlementRequested(EventEnvelope event) {
        return publishEvent(PAYMENT_SETTLEMENT_REQUESTED_TOPIC, event.getPacketHash(), event);
    }

    /**
     * Publish payment settled event
     */
    public CompletableFuture<SendResult<String, Object>> publishPaymentSettled(EventEnvelope event) {
        return publishEvent(PAYMENT_SETTLED_TOPIC, event.getPacketHash(), event);
    }

    /**
     * Publish payment failed event
     */
    public CompletableFuture<SendResult<String, Object>> publishPaymentFailed(EventEnvelope event) {
        return publishEvent(PAYMENT_FAILED_TOPIC, event.getPacketHash(), event);
    }

    /**
     * Publish to retry topic
     */
    public CompletableFuture<SendResult<String, Object>> publishRetry(EventEnvelope event) {
        return publishEvent(PAYMENT_RETRY_TOPIC, event.getPacketHash(), event);
    }

    /**
     * Publish to dead letter queue
     */
    public CompletableFuture<SendResult<String, Object>> publishDeadLetter(EventEnvelope event) {
        return publishEvent(PAYMENT_DLQ_TOPIC, event.getPacketHash(), event);
    }

    /**
     * Generic event publishing method
     */
    private CompletableFuture<SendResult<String, Object>> publishEvent(String topic, String key, EventEnvelope event) {
        log.info("Publishing event to topic: {}, eventType: {}, packetHash: {}", 
                topic, event.getEventType(), event.getPacketHash());
        
        return kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Event published successfully to topic: {}, partition: {}, offset: {}", 
                                topic, result.getRecordMetadata().partition(), 
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to publish event to topic: {}, packetHash: {}", 
                                topic, event.getPacketHash(), ex);
                    }
                });
    }
}
