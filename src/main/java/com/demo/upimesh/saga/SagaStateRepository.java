package com.demo.upimesh.saga;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Saga State persistence
 * Enables microservices architecture with durable saga state
 */
@Repository
public interface SagaStateRepository extends JpaRepository<SagaState, Long> {

    Optional<SagaState> findBySagaId(String sagaId);

    Optional<SagaState> findByPacketHash(String packetHash);

    void deleteBySagaId(String sagaId);
}
