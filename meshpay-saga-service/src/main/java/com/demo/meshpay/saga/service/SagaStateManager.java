package com.demo.meshpay.saga.service;

import com.demo.meshpay.saga.model.SagaState;
import com.demo.meshpay.saga.repository.SagaStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Manages Saga state for distributed transactions
 * Uses persistent database storage for microservices architecture
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SagaStateManager {

    private final SagaStateRepository sagaStateRepository;

    /**
     * Create a new saga state
     */
    @Transactional
    public void createSaga(String sagaId, String aggregateId, String initialState) {
        SagaState state = SagaState.builder()
                .sagaId(sagaId)
                .packetHash(aggregateId)
                .aggregateId(aggregateId)
                .state(initialState)
                .currentStep(initialState)
                .build();
        
        sagaStateRepository.save(state);
        log.info("Created saga state: sagaId={}, state={}", sagaId, initialState);
    }

    /**
     * Update saga state
     */
    @Transactional
    public void updateSagaState(String sagaId, String newState) {
        Optional<SagaState> stateOpt = sagaStateRepository.findBySagaId(sagaId);
        if (stateOpt.isPresent()) {
            SagaState state = stateOpt.get();
            state.setState(newState);
            state.setCurrentStep(newState);
            state.setUpdatedAt(Instant.now());
            sagaStateRepository.save(state);
            log.info("Updated saga state: sagaId={}, newState={}", sagaId, newState);
        } else {
            log.warn("Saga state not found for update: sagaId={}", sagaId);
        }
    }

    /**
     * Get saga state
     */
    public SagaState getSagaState(String sagaId) {
        return sagaStateRepository.findBySagaId(sagaId).orElse(null);
    }

    /**
     * Delete saga state
     */
    @Transactional
    public void deleteSaga(String sagaId) {
        sagaStateRepository.deleteBySagaId(sagaId);
        log.info("Deleted saga state: sagaId={}", sagaId);
    }

    /**
     * Get saga state by packet hash
     */
    public SagaState getSagaStateByPacketHash(String packetHash) {
        return sagaStateRepository.findByPacketHash(packetHash).orElse(null);
    }

    /**
     * Find saga ID by packet hash (for event correlation)
     */
    public String findSagaByPacketHash(String packetHash) {
        Optional<SagaState> state = sagaStateRepository.findByPacketHash(packetHash);
        return state.map(SagaState::getSagaId).orElse(null);
    }
}
