package com.demo.meshpay.saga.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Background processor for Saga Service
 * Periodically publishes saga state events to Kafka for monitoring and compensation
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SagaOutboxProcessor {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Publish saga state updates for monitoring
     * Runs every 10 seconds
     */
    @Scheduled(fixedDelay = 10000)
    public void publishSagaStateUpdates() {
        try {
            // In a real implementation, this would query a saga outbox table
            // For now, this is a placeholder for the pattern
            
            log.debug("Saga outbox processor running");
            
        } catch (Exception e) {
            log.error("Error in saga outbox processor", e);
        }
    }

    /**
     * Publish saga completion events
     */
    public void publishSagaCompletion(String sagaId, String packetHash, String status) {
        try {
            Map<String, Object> event = Map.of(
                "sagaId", sagaId,
                "packetHash", packetHash,
                "status", status,
                "timestamp", Instant.now().toString()
            );
            
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("saga-completed", sagaId, eventJson);
            
            log.info("Published saga completion event: sagaId={}, status={}", sagaId, status);
            
        } catch (Exception e) {
            log.error("Failed to publish saga completion event: sagaId={}", sagaId, e);
        }
    }

    /**
     * Publish saga failure events for compensation
     */
    public void publishSagaFailure(String sagaId, String packetHash, String failureReason) {
        try {
            Map<String, Object> event = Map.of(
                "sagaId", sagaId,
                "packetHash", packetHash,
                "failureReason", failureReason,
                "timestamp", Instant.now().toString()
            );
            
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("saga-failed", sagaId, eventJson);
            
            log.info("Published saga failure event: sagaId={}, reason={}", sagaId, failureReason);
            
        } catch (Exception e) {
            log.error("Failed to publish saga failure event: sagaId={}", sagaId, e);
        }
    }
}
