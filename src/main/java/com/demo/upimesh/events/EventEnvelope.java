package com.demo.upimesh.events;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Event Envelope for Kafka Messages
 * Provides consistent structure for all domain events with metadata for tracing and schema evolution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventEnvelope {

    /**
     * Unique identifier for this event instance
     */
    private String eventId;

    /**
     * Type of event (e.g., PAYMENT_RECEIVED, PAYMENT_SETTLED)
     */
    private String eventType;

    /**
     * Aggregate root identifier (e.g., packetHash for payment events)
     */
    private String aggregateId;

    /**
     * Packet hash for payment-related events (used for idempotency)
     */
    private String packetHash;

    /**
     * Event creation timestamp
     */
    private Instant timestamp;

    /**
     * Service that produced this event
     */
    private String producer;

    /**
     * Schema version for event evolution
     */
    private String schemaVersion;

    /**
     * Correlation ID for distributed tracing
     */
    private String correlationId;

    /**
     * The actual event payload
     */
    private Object payload;

    /**
     * Create a new event envelope with generated IDs
     */
    public static EventEnvelope create(String eventType, String aggregateId, String packetHash, 
                                       String producer, Object payload, String correlationId) {
        return EventEnvelope.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .aggregateId(aggregateId)
                .packetHash(packetHash)
                .timestamp(Instant.now())
                .producer(producer)
                .schemaVersion("1.0")
                .correlationId(correlationId != null ? correlationId : UUID.randomUUID().toString())
                .payload(payload)
                .build();
    }
}
