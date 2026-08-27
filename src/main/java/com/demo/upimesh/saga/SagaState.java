package com.demo.upimesh.saga;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Represents the state of a Saga transaction
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaState {
    
    private String sagaId;
    private String aggregateId;
    private String state;
    private Instant createdAt;
    private Instant updatedAt;
    private String currentStep;
    private String failureReason;
}
