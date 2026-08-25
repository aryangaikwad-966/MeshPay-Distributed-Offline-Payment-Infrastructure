package com.demo.upimesh.processor;

import com.demo.upimesh.model.Outbox;
import com.demo.upimesh.service.OutboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Background processor for publishing events from Outbox to Kafka
 * Runs on a schedule to ensure reliable event delivery
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventProcessor {

    private final OutboxService outboxService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Process unprocessed outbox entries
     * Runs every 5 seconds
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    @Transactional
    public void processOutboxEntries() {
        try {
            List<Outbox> unprocessedEntries = outboxService.findUnprocessedEntriesWithLock();
            
            if (unprocessedEntries.isEmpty()) {
                return;
            }
            
            log.info("Processing {} unprocessed outbox entries", unprocessedEntries.size());
            
            for (Outbox outbox : unprocessedEntries) {
                processSingleEntry(outbox);
            }
            
        } catch (Exception e) {
            log.error("Error processing outbox entries", e);
        }
    }

    /**
     * Process a single outbox entry
     */
    private void processSingleEntry(Outbox outbox) {
        try {
            // Deserialize the event payload
            Object event = objectMapper.readValue(outbox.getPayload(), Object.class);
            
            // Publish to Kafka
            kafkaTemplate
                    .send(outbox.getTopic(), outbox.getPartitionKey(), event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            // Mark as processed on successful publish
                            outboxService.markAsProcessed(outbox.getId());
                            log.debug("Successfully published and marked outbox entry as processed: id={}, eventId={}", 
                                    outbox.getId(), outbox.getEventId());
                        } else {
                            // Increment retry count on failure
                            String errorMessage = ex.getMessage();
                            outboxService.incrementRetryCount(outbox.getId(), errorMessage);
                            log.error("Failed to publish outbox entry: id={}, eventId={}, error={}", 
                                    outbox.getId(), outbox.getEventId(), errorMessage);
                        }
                    });
            
        } catch (Exception e) {
            log.error("Error processing outbox entry: id={}, eventId={}", 
                    outbox.getId(), outbox.getEventId(), e);
            outboxService.incrementRetryCount(outbox.getId(), e.getMessage());
        }
    }

    /**
     * Cleanup processed outbox entries older than 7 days
     * Runs daily
     */
    @Scheduled(cron = "0 0 2 * * ?") // 2 AM daily
    @Transactional
    public void cleanupOldEntries() {
        try {
            java.time.Instant cutoffTime = java.time.Instant.now().minus(java.time.Duration.ofDays(7));
            outboxService.deleteProcessedOlderThan(cutoffTime);
            log.info("Completed cleanup of old outbox entries");
        } catch (Exception e) {
            log.error("Error during outbox cleanup", e);
        }
    }
}
