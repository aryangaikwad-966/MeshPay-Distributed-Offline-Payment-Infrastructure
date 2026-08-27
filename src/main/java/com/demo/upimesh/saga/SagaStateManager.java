package com.demo.upimesh.saga;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Saga state for distributed transactions
 * In production, this would use a persistent store (database)
 */
@Service
@Slf4j
public class SagaStateManager {

    private final Map<String, SagaState> sagaStates = new ConcurrentHashMap<>();

    /**
     * Create a new saga state
     */
    public void createSaga(String sagaId, String aggregateId, String initialState) {
        SagaState state = SagaState.builder()
                .sagaId(sagaId)
                .aggregateId(aggregateId)
                .state(initialState)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .currentStep(initialState)
                .build();
        
        sagaStates.put(sagaId, state);
        log.info("Created saga state: sagaId={}, state={}", sagaId, initialState);
    }

    /**
     * Update saga state
     */
    public void updateSagaState(String sagaId, String newState) {
        SagaState state = sagaStates.get(sagaId);
        if (state != null) {
            state.setState(newState);
            state.setUpdatedAt(Instant.now());
            state.setCurrentStep(newState);
            sagaStates.put(sagaId, state);
            log.info("Updated saga state: sagaId={}, newState={}", sagaId, newState);
        } else {
            log.warn("Saga state not found for update: sagaId={}", sagaId);
        }
    }

    /**
     * Get saga state
     */
    public SagaState getSagaState(String sagaId) {
        return sagaStates.get(sagaId);
    }

    /**
     * Delete saga state
     */
    public void deleteSaga(String sagaId) {
        sagaStates.remove(sagaId);
        log.info("Deleted saga state: sagaId={}", sagaId);
    }

    /**
     * Get all active sagas
     */
    public Map<String, SagaState> getAllSagas() {
        return new ConcurrentHashMap<>(sagaStates);
    }
}
