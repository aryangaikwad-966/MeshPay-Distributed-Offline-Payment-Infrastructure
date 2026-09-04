package com.demo.upimesh.saga;

import com.demo.upimesh.events.CompleteSettlementCommand;
import com.demo.upimesh.events.RequestSettlementCommand;
import com.demo.upimesh.events.ValidatePaymentCommand;
import com.demo.upimesh.metrics.PaymentMetrics;
import com.demo.upimesh.service.PaymentSettlementService;
import com.demo.upimesh.service.SagaCommandProducer;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Saga Orchestrator for Payment Settlement
 * Manages distributed transactions with compensation actions
 * Decoupled from Payment Service via Kafka events
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentSagaOrchestrator {

    private final PaymentSettlementService paymentSettlementService;
    private final SagaStateManager sagaStateManager;
    private final PaymentMetrics paymentMetrics;
    private final SagaCommandProducer sagaCommandProducer;

    /**
     * Start a new payment saga
     */
    @Transactional
    public String startPaymentSaga(String packetId, String packetHash, String bridgeNodeId, String ciphertext) {
        String sagaId = UUID.randomUUID().toString();
        Timer.Sample timer = paymentMetrics.startSagaExecutionTimer();
        
        log.info("Starting payment saga: sagaId={}, packetHash={}", sagaId, packetHash);
        paymentMetrics.incrementSagaStarted();
        paymentMetrics.incrementActiveSagas();
        
        // Initialize saga state
        sagaStateManager.createSaga(sagaId, packetHash, "PAYMENT_RECEIVED");
        
        try {
            // Step 1: Process payment received
            paymentSettlementService.processPaymentReceived(packetId, packetHash, bridgeNodeId, ciphertext);
            
            // Update saga state
            sagaStateManager.updateSagaState(sagaId, "PAYMENT_RECEIVED_COMPLETED");
            paymentMetrics.stopSagaExecutionTimer(timer);
            
            return sagaId;
            
        } catch (Exception e) {
            log.error("Payment saga failed at PAYMENT_RECEIVED: sagaId={}", sagaId, e);
            sagaStateManager.updateSagaState(sagaId, "PAYMENT_RECEIVED_FAILED");
            paymentMetrics.incrementSagaCompensated();
            paymentMetrics.decrementActiveSagas();
            paymentMetrics.stopSagaExecutionTimer(timer);
            compensatePaymentReceived(sagaId, packetHash);
            throw new RuntimeException("Payment saga failed", e);
        }
    }

    /**
     * Continue saga with payment validation
     * Sends Kafka command to Payment Service instead of direct call
     */
    @Transactional
    public void validatePaymentSaga(String sagaId, String packetId, String packetHash, 
                                   String senderVpa, String receiverVpa, java.math.BigDecimal amount, 
                                   String nonce, Long signedAt) {
        log.info("Validating payment in saga: sagaId={}, packetHash={}", sagaId, packetHash);
        
        sagaStateManager.updateSagaState(sagaId, "VALIDATING_PAYMENT");
        
        try {
            // Send Kafka command to Payment Service
            ValidatePaymentCommand command = ValidatePaymentCommand.builder()
                    .sagaId(sagaId)
                    .packetId(packetId)
                    .packetHash(packetHash)
                    .senderVpa(senderVpa)
                    .receiverVpa(receiverVpa)
                    .amount(amount)
                    .nonce(nonce)
                    .signedAt(signedAt)
                    .build();
            sagaCommandProducer.sendValidatePaymentCommand(command);
            sagaStateManager.updateSagaState(sagaId, "PAYMENT_VALIDATION_COMMAND_SENT");
            
        } catch (Exception e) {
            log.error("Payment saga failed at VALIDATION: sagaId={}", sagaId, e);
            sagaStateManager.updateSagaState(sagaId, "PAYMENT_VALIDATION_FAILED");
            compensateValidation(sagaId, packetHash);
            throw new RuntimeException("Payment validation failed", e);
        }
    }

    /**
     * Continue saga with settlement request
     * Sends Kafka command to Payment Service instead of direct call
     */
    @Transactional
    public void requestSettlementSaga(String sagaId, String packetId, String packetHash,
                                      String senderVpa, String receiverVpa, java.math.BigDecimal amount,
                                      String nonce, Long signedAt) {
        log.info("Requesting settlement in saga: sagaId={}, packetHash={}", sagaId, packetHash);
        
        sagaStateManager.updateSagaState(sagaId, "REQUESTING_SETTLEMENT");
        
        try {
            // Send Kafka command to Payment Service
            RequestSettlementCommand command = RequestSettlementCommand.builder()
                    .sagaId(sagaId)
                    .packetId(packetId)
                    .packetHash(packetHash)
                    .senderVpa(senderVpa)
                    .receiverVpa(receiverVpa)
                    .amount(amount)
                    .nonce(nonce)
                    .signedAt(signedAt)
                    .build();
            sagaCommandProducer.sendRequestSettlementCommand(command);
            sagaStateManager.updateSagaState(sagaId, "SETTLEMENT_REQUEST_COMMAND_SENT");
            
        } catch (Exception e) {
            log.error("Payment saga failed at SETTLEMENT_REQUEST: sagaId={}", sagaId, e);
            sagaStateManager.updateSagaState(sagaId, "SETTLEMENT_REQUEST_FAILED");
            compensateSettlementRequest(sagaId, packetHash);
            throw new RuntimeException("Settlement request failed", e);
        }
    }

    /**
     * Complete saga with settlement confirmation
     * Sends Kafka command to Payment Service instead of direct call
     */
    @Transactional
    public void completePaymentSaga(String sagaId, String packetId, String packetHash,
                                    String senderVpa, String receiverVpa, java.math.BigDecimal amount) {
        log.info("Completing payment saga: sagaId={}, packetHash={}", sagaId, packetHash);
        
        sagaStateManager.updateSagaState(sagaId, "COMPLETING_SETTLEMENT");
        
        try {
            // Send Kafka command to Payment Service
            CompleteSettlementCommand command = CompleteSettlementCommand.builder()
                    .sagaId(sagaId)
                    .packetId(packetId)
                    .packetHash(packetHash)
                    .senderVpa(senderVpa)
                    .receiverVpa(receiverVpa)
                    .amount(amount)
                    .build();
            sagaCommandProducer.sendCompleteSettlementCommand(command);
            sagaStateManager.updateSagaState(sagaId, "SETTLEMENT_COMPLETION_COMMAND_SENT");
            
        } catch (Exception e) {
            log.error("Payment saga failed at COMPLETION: sagaId={}", sagaId, e);
            sagaStateManager.updateSagaState(sagaId, "SETTLEMENT_COMPLETION_FAILED");
            paymentMetrics.incrementSagaCompensated();
            paymentMetrics.decrementActiveSagas();
            compensateSettlement(sagaId, packetHash);
            throw new RuntimeException("Settlement completion failed", e);
        }
    }

    /**
     * Compensate payment received step
     */
    private void compensatePaymentReceived(String sagaId, String packetHash) {
        log.info("Compensating payment received: sagaId={}, packetHash={}", sagaId, packetHash);
        // In a real system, this might involve:
        // - Marking the packet as rejected
        // - Notifying the sender
        // - Cleaning up temporary resources
        sagaStateManager.updateSagaState(sagaId, "COMPENSATED_PAYMENT_RECEIVED");
    }

    /**
     * Compensate validation step
     */
    private void compensateValidation(String sagaId, String packetHash) {
        log.info("Compensating validation: sagaId={}, packetHash={}", sagaId, packetHash);
        // Compensation for validation typically involves:
        // - Reverting any state changes made during validation
        // - Notifying relevant parties
        sagaStateManager.updateSagaState(sagaId, "COMPENSATED_VALIDATION");
    }

    /**
     * Compensate settlement request step
     */
    private void compensateSettlementRequest(String sagaId, String packetHash) {
        log.info("Compensating settlement request: sagaId={}, packetHash={}", sagaId, packetHash);
        // Compensation for settlement request:
        // - Canceling the settlement request
        // - Releasing any held funds
        // - Notifying settlement system
        sagaStateManager.updateSagaState(sagaId, "COMPENSATED_SETTLEMENT_REQUEST");
    }

    /**
     * Compensate settlement step
     */
    private void compensateSettlement(String sagaId, String packetHash) {
        log.info("Compensating settlement: sagaId={}, packetHash={}", sagaId, packetHash);
        // Compensation for settlement:
        // - Reversing the settlement transaction
        // - Restoring original balances
        // - Notifying all parties
        sagaStateManager.updateSagaState(sagaId, "COMPENSATED_SETTLEMENT");
    }

    /**
     * Handle saga failure with full compensation
     */
    @Transactional
    public void handleSagaFailure(String sagaId, String packetHash, String failureReason) {
        log.error("Handling saga failure: sagaId={}, packetHash={}, reason={}", sagaId, packetHash, failureReason);
        
        SagaState currentState = sagaStateManager.getSagaState(sagaId);
        
        // Execute compensation based on current state
        switch (currentState.getState()) {
            case "SETTLEMENT_COMPLETION_FAILED":
                compensateSettlement(sagaId, packetHash);
                compensateSettlementRequest(sagaId, packetHash);
                break;
            case "SETTLEMENT_REQUEST_FAILED":
                compensateSettlementRequest(sagaId, packetHash);
                break;
            case "PAYMENT_VALIDATION_FAILED":
                compensateValidation(sagaId, packetHash);
                break;
            case "PAYMENT_RECEIVED_FAILED":
                compensatePaymentReceived(sagaId, packetHash);
                break;
            default:
                log.warn("Unknown saga state for compensation: {}", currentState.getState());
        }
        
        sagaStateManager.updateSagaState(sagaId, "SAGA_COMPENSATED");
    }
}
