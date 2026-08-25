package com.demo.upimesh.repository;

import com.demo.upimesh.model.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Outbox Pattern
 * Provides methods for querying and updating outbox entries with locking support
 */
@Repository
public interface OutboxRepository extends JpaRepository<Outbox, Long> {

    /**
     * Find unprocessed outbox entries ordered by creation time
     * Uses pessimistic locking to prevent concurrent processing
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Outbox o WHERE o.processed = false ORDER BY o.createdAt ASC")
    List<Outbox> findUnprocessedEntriesWithLock();

    /**
     * Find unprocessed outbox entries without locking (for monitoring)
     */
    @Query("SELECT o FROM Outbox o WHERE o.processed = false ORDER BY o.createdAt ASC")
    List<Outbox> findUnprocessedEntries();

    /**
     * Find outbox entry by event ID
     */
    Optional<Outbox> findByEventId(String eventId);

    /**
     * Find outbox entry by packet hash (for idempotency checks)
     */
    Optional<Outbox> findByPacketHashAndProcessed(String packetHash, Boolean processed);

    /**
     * Mark outbox entry as processed
     */
    @Modifying
    @Query("UPDATE Outbox o SET o.processed = true, o.processedAt = :processedAt WHERE o.id = :id")
    void markAsProcessed(Long id, Instant processedAt);

    /**
     * Increment retry count and set error message
     */
    @Modifying
    @Query("UPDATE Outbox o SET o.retryCount = o.retryCount + 1, o.errorMessage = :errorMessage WHERE o.id = :id")
    void incrementRetryCount(Long id, String errorMessage);

    /**
     * Delete processed outbox entries older than specified time
     */
    @Modifying
    @Query("DELETE FROM Outbox o WHERE o.processed = true AND o.processedAt < :cutoffTime")
    void deleteProcessedOlderThan(Instant cutoffTime);

    /**
     * Count unprocessed entries
     */
    long countByProcessedFalse();
}
