package com.demo.upimesh.repository;

import com.demo.upimesh.model.EventProcessed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * Repository for EventProcessed entity
 * Provides methods for checking and tracking processed events
 */
@Repository
public interface EventProcessedRepository extends JpaRepository<EventProcessed, Long> {

    /**
     * Find event processed record by event ID
     */
    Optional<EventProcessed> findByEventId(String eventId);

    /**
     * Find event processed record by aggregate ID
     */
    Optional<EventProcessed> findByAggregateId(String aggregateId);

    /**
     * Check if event has been processed
     */
    boolean existsByEventId(String eventId);

    /**
     * Delete processed event records older than specified time
     * Used for cleanup
     */
    @Modifying
    @Query("DELETE FROM EventProcessed e WHERE e.processedAt < :cutoffTime")
    void deleteProcessedOlderThan(Instant cutoffTime);

    /**
     * Count total processed events
     */
    long count();
}
