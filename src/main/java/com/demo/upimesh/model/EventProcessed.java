package com.demo.upimesh.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entity to track processed events for idempotency
 * Ensures that events are not processed multiple times
 */
@Entity
@Table(name = "event_processed", indexes = {
    @Index(name = "idx_event_processed_event_id", columnList = "eventId", unique = true),
    @Index(name = "idx_event_processed_aggregate_id", columnList = "aggregateId"),
    @Index(name = "idx_event_processed_processed_at", columnList = "processedAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventProcessed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique event ID from the event envelope
     */
    @Column(nullable = false, unique = true, length = 255)
    private String eventId;

    /**
     * Event type (e.g., PAYMENT_RECEIVED, PAYMENT_SETTLED)
     */
    @Column(nullable = false, length = 100)
    private String eventType;

    /**
     * Aggregate ID (e.g., packetHash for payment events)
     */
    @Column(nullable = false, length = 255)
    private String aggregateId;

    /**
     * Packet hash for payment-related events
     */
    @Column(length = 255)
    private String packetHash;

    /**
     * Topic the event was consumed from
     */
    @Column(length = 255)
    private String topic;

    /**
     * Partition the event was consumed from
     */
    @Column
    private Integer partition;

    /**
     * Offset the event was consumed from
     */
    @Column
    private Long offset;

    /**
     * Consumer group that processed the event
     */
    @Column(length = 255)
    private String consumerGroup;

    /**
     * When the event was processed
     */
    @Column(nullable = false)
    private Instant processedAt = Instant.now();

    /**
     * Processing status
     */
    @Column(nullable = false, length = 50)
    private String status = "SUCCESS";

    /**
     * Error message if processing failed
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
}
