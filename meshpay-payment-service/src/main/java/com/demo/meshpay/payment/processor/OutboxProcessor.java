package com.demo.meshpay.payment.processor;

import com.demo.meshpay.payment.model.Outbox;
import com.demo.meshpay.payment.repository.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Background processor for Transactional Outbox pattern
 * Periodically processes unprocessed outbox events and publishes them to Kafka
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Process outbox events every 5 seconds
     * Uses pessimistic locking to prevent concurrent processing
     */
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processOutboxEvents() {
        try {
            // Fetch unprocessed events with pessimistic locking
            List<Outbox> unprocessedEvents = outboxRepository.findUnprocessedEventsForProcessing(50);

            if (unprocessedEvents.isEmpty()) {
                return;
            }

            log.info("Processing {} outbox events", unprocessedEvents.size());

            for (Outbox outbox : unprocessedEvents) {
                try {
                    // Publish event to Kafka
                    String payloadJson = objectMapper.writeValueAsString(outbox.getPayload());
                    kafkaTemplate.send(outbox.getTopic(), outbox.getPartitionKey(), payloadJson);

                    // Mark as processed
                    outbox.setProcessed(true);
                    outbox.setProcessedAt(Instant.now());
                    outboxRepository.save(outbox);

                    log.debug("Processed outbox event: eventId={}, topic={}", 
                        outbox.getEventId(), outbox.getTopic());

                } catch (Exception e) {
                    log.error("Failed to process outbox event: eventId={}", outbox.getEventId(), e);
                    
                    // Increment retry count
                    outbox.setRetryCount(outbox.getRetryCount() + 1);
                    outbox.setErrorMessage(e.getMessage());
                    outboxRepository.save(outbox);
                    
                    // If max retries exceeded, mark as failed
                    if (outbox.getRetryCount() >= 3) {
                        outbox.setProcessed(true);
                        log.error("Outbox event failed after max retries: eventId={}", outbox.getEventId());
                    }
                    
                    outboxRepository.save(outbox);
                }
            }

            log.info("Completed processing outbox events");

        } catch (Exception e) {
            log.error("Error in outbox processor", e);
        }
    }

    /**
     * Clean up processed events older than 24 hours
     * Runs every hour
     */
    @Scheduled(fixedDelay = 3600000)
    @Transactional
    public void cleanupProcessedEvents() {
        try {
            Instant cutoffTime = Instant.now().minusSeconds(86400); // 24 hours
            int deletedCount = outboxRepository.deleteProcessedEventsBefore(cutoffTime);
            
            if (deletedCount > 0) {
                log.info("Cleaned up {} processed outbox events older than 24 hours", deletedCount);
            }

        } catch (Exception e) {
            log.error("Error cleaning up processed events", e);
        }
    }
}
