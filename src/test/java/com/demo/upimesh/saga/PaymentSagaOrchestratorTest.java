package com.demo.upimesh.saga;

import com.demo.upimesh.metrics.PaymentMetrics;
import com.demo.upimesh.service.PaymentSettlementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentSagaOrchestrator
 * Tests the saga orchestration and compensation logic
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Payment Saga Orchestrator Tests")
public class PaymentSagaOrchestratorTest {

    @Mock
    private PaymentSettlementService paymentSettlementService;

    @Mock
    private SagaStateManager sagaStateManager;

    @Mock
    private PaymentMetrics paymentMetrics;

    @InjectMocks
    private PaymentSagaOrchestrator paymentSagaOrchestrator;

    @BeforeEach
    void setUp() {
        reset(paymentSettlementService, sagaStateManager, paymentMetrics);
    }

    @Test
    @DisplayName("Start payment saga creates saga state and increments metrics")
    void testStartPaymentSaga() {
        // Arrange
        String packetId = "packet-123";
        String packetHash = "hash-abc";
        String bridgeNodeId = "bridge-1";
        String ciphertext = "encrypted-data";

        // Act
        paymentSagaOrchestrator.startPaymentSaga(packetId, packetHash, bridgeNodeId, ciphertext);

        // Assert
        verify(sagaStateManager).createSaga(anyString(), anyString(), anyString());
        verify(paymentMetrics).incrementSagaStarted();
        verify(paymentMetrics).incrementActiveSagas();
    }

    @Test
    @DisplayName("Saga failure triggers compensation and decrements active sagas")
    void testSagaFailure() {
        // Arrange
        String packetId = "packet-123";
        String packetHash = "hash-abc";
        String bridgeNodeId = "bridge-1";
        String ciphertext = "encrypted-data";

        doThrow(new RuntimeException("Test failure")).when(paymentSettlementService).processPaymentReceived(anyString(), anyString(), anyString(), anyString());

        // Act
        try {
            paymentSagaOrchestrator.startPaymentSaga(packetId, packetHash, bridgeNodeId, ciphertext);
        } catch (Exception e) {
            // Expected
        }

        // Assert - compensation should be triggered
        verify(paymentMetrics, atLeastOnce()).incrementSagaCompensated();
        verify(paymentMetrics, atLeastOnce()).decrementActiveSagas();
    }

    @Test
    @DisplayName("Complete saga marks as completed and decrements active sagas")
    void testCompleteSaga() {
        // Arrange
        String sagaId = "saga-123";
        String packetId = "packet-123";
        String packetHash = "hash-abc";
        String senderVpa = "alice@demo";
        String receiverVpa = "bob@demo";
        java.math.BigDecimal amount = new java.math.BigDecimal("100.00");

        // Act
        paymentSagaOrchestrator.completePaymentSaga(sagaId, packetId, packetHash, senderVpa, receiverVpa, amount);

        // Assert
        verify(sagaStateManager).updateSagaState(sagaId, "SAGA_COMPLETED");
        verify(paymentMetrics).incrementSagaCompleted();
        verify(paymentMetrics).decrementActiveSagas();
    }
}
