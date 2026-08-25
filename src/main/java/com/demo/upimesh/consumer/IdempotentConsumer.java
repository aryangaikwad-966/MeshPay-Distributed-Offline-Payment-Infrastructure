package com.demo.upimesh.consumer;

import com.demo.upimesh.events.EventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Base class for idempotent Kafka consumers
 * Provides common functionality for processing events with idempotency guarantees
 */
@Component
@RequiredArgsConstructor
@Slf4j
public abstract class IdempotentConsumer {

    /**
     * Process an event envelope with idempotency guarantees
     * Subclasses should implement the handleEvent method
     */
    @KafkaListener(topics = "#{@kafkaTopics}", groupId = "meshpay-consumer-group", 
                   containerFactory = "kafkaListenerContainerFactory")
    public void consumeEvent(@Payload EventEnvelope eventEnvelope,
                            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                            @Header(KafkaHeaders.OFFSET) long offset,
                            Acknowledgment acknowledgment) {
        
        String eventId = eventEnvelope.getEventId();
        String eventType = eventEnvelope.getEventType();
        String aggregateId = eventEnvelope.getAggregateId();
        
        log.info("Received event: eventId={}, eventType={}, aggregateId={}, topic={}, partition={}, offset={}", 
                eventId, eventType, aggregateId, topic, partition, offset);
        
        try {
            // Check if event has already been processed (idempotency check)
            if (isEventProcessed(eventId)) {
                log.warn("Event already processed, skipping: eventId={}", eventId);
                acknowledgment.acknowledge();
                return;
            }
            
            // Process the event
            handleEvent(eventEnvelope, topic);
            
            // Mark event as processed
            markEventAsProcessed(eventId);
            
            // Acknowledge the message
            acknowledgment.acknowledge();
            
            log.info("Successfully processed event: eventId={}, eventType={}", eventId, eventType);
            
        } catch (Exception e) {
            log.error("Error processing event: eventId={}, eventType={}", eventId, eventType, e);
            // Don't acknowledge - let Kafka retry
            throw new RuntimeException("Failed to process event: " + eventId, e);
        }
    }

    /**
     * Handle the specific event logic
     * Subclasses must implement this method
     */
    protected abstract void handleEvent(EventEnvelope eventEnvelope, String topic) throws Exception;

    /**
     * Check if event has already been processed
     * Subclasses must implement this method with their idempotency strategy
     */
    protected abstract boolean isEventProcessed(String eventId);

    /**
     * Mark event as processed
     * Subclasses must implement this method with their idempotency strategy
     */
    protected abstract void markEventAsProcessed(String eventId);

    /**
     * Generate a unique event ID if not provided
     */
    protected String generateEventId() {
        return UUID.randomUUID().toString();
    }
}
