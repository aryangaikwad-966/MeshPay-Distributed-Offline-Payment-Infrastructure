package com.demo.upimesh.consumer;

import com.demo.upimesh.events.EventEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumer for Dead Letter Queue (DLQ) topics
 * Handles failed messages from main topics for manual inspection and retry
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DeadLetterQueueConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = "${kafka.dlq.topic:payment-events-dlq}",
        groupId = "${kafka.consumer.group:meshpay-consumer-group}-dlq",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleDeadLetterMessage(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_TIMESTAMP) long timestamp,
            Acknowledgment acknowledgment
    ) {
        try {
            log.warn("Processing DLQ message: topic={}, partition={}, offset={}", topic, partition, offset);
            
            // Parse the original message
            EventEnvelope eventEnvelope = objectMapper.readValue(message, EventEnvelope.class);
            
            log.info("DLQ Event: eventId={}, eventType={}, aggregateId={}", 
                    eventEnvelope.getEventId(), 
                    eventEnvelope.getEventType(), 
                    eventEnvelope.getAggregateId());
            
            // In a production system, you would:
            // 1. Store the failed event in a DLQ table for manual review
            // 2. Send alert notifications
            // 3. Provide a mechanism for manual retry
            
            // For now, we just log and acknowledge
            log.warn("DLQ message logged for manual review. Event: {}", eventEnvelope);
            
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
            
        } catch (Exception e) {
            log.error("Error processing DLQ message: topic={}, partition={}, offset={}", 
                    topic, partition, offset, e);
            
            // Even if we can't process the DLQ message, we should acknowledge it
            // to prevent it from being stuck in the DLQ forever
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
        }
    }
}
