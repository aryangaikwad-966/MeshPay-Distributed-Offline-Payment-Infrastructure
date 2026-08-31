package com.demo.upimesh.service;

import com.demo.upimesh.events.EventEnvelope;
import com.demo.upimesh.metrics.PaymentMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentSettlementService
 * Tests the event-driven payment settlement flow
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Payment Settlement Service Tests")
public class PaymentSettlementServiceTest {

    @Mock
    private OutboxService outboxService;

    @Mock
    private PaymentMetrics paymentMetrics;

    @InjectMocks
    private PaymentSettlementService paymentSettlementService;

    @BeforeEach
    void setUp() {
        reset(outboxService, paymentMetrics);
    }

    @Test
    @DisplayName("Process payment received creates event and saves to outbox")
    void testProcessPaymentReceived() {
        // Arrange
        String packetId = "packet-123";
        String packetHash = "hash-abc";
        String bridgeNodeId = "bridge-1";
        String ciphertext = "encrypted-data";

        // Act
        paymentSettlementService.processPaymentReceived(packetId, packetHash, bridgeNodeId, ciphertext);

        // Assert
        ArgumentCaptor<EventEnvelope> eventCaptor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(outboxService).saveEvent(eventCaptor.capture(), anyString(), anyString(), anyString());

        EventEnvelope capturedEvent = eventCaptor.getValue();
        assertEquals("PAYMENT_RECEIVED", capturedEvent.getEventType());
        assertEquals(packetHash, capturedEvent.getAggregateId());

        verify(paymentMetrics).incrementPaymentReceived();
        verify(paymentMetrics).incrementPendingSettlements();
    }

    @Test
    @DisplayName("Validate payment creates validated event on success")
    void testValidatePaymentSuccess() {
        // Arrange
        String packetId = "packet-123";
        String packetHash = "hash-abc";
        String senderVpa = "alice@demo";
        String receiverVpa = "bob@demo";
        BigDecimal amount = new BigDecimal("100.00");
        String nonce = "nonce-123";
        Long signedAt = System.currentTimeMillis();

        // Act
        paymentSettlementService.validatePayment(packetId, packetHash, senderVpa, receiverVpa, amount, nonce, signedAt, null);

        // Assert
        ArgumentCaptor<EventEnvelope> eventCaptor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(outboxService, atLeastOnce()).saveEvent(eventCaptor.capture(), anyString(), anyString(), anyString());

        verify(paymentMetrics).incrementPaymentValidated();
    }

    @Test
    @DisplayName("Validate payment handles failure and increments failure metrics")
    void testValidatePaymentFailure() {
        // Arrange
        String packetId = "packet-123";
        String packetHash = "hash-abc";
        String senderVpa = "alice@demo";
        String receiverVpa = "bob@demo";
        BigDecimal amount = new BigDecimal("100.00");
        String nonce = "nonce-123";
        Long signedAt = System.currentTimeMillis();

        doThrow(new RuntimeException("Validation failed")).when(outboxService).saveEvent(any(), anyString(), anyString(), anyString());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            paymentSettlementService.validatePayment(packetId, packetHash, senderVpa, receiverVpa, amount, nonce, signedAt, null)
        );

        verify(paymentMetrics).incrementPaymentFailed();
    }

    @Test
    @DisplayName("Request settlement creates settlement requested event")
    void testRequestSettlement() {
        // Arrange
        String packetId = "packet-123";
        String packetHash = "hash-abc";
        String senderVpa = "alice@demo";
        String receiverVpa = "bob@demo";
        BigDecimal amount = new BigDecimal("100.00");
        String nonce = "nonce-123";
        Long signedAt = System.currentTimeMillis();

        // Act
        paymentSettlementService.requestSettlement(packetId, packetHash, senderVpa, receiverVpa, amount, nonce, signedAt, null);

        // Assert
        ArgumentCaptor<EventEnvelope> eventCaptor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(outboxService, atLeastOnce()).saveEvent(eventCaptor.capture(), anyString(), anyString(), anyString());

        verify(paymentMetrics).incrementSettlementRequested();
    }

    @Test
    @DisplayName("Mark payment settled creates settled event and decrements pending")
    void testMarkPaymentSettled() {
        // Arrange
        String packetId = "packet-123";
        String packetHash = "hash-abc";
        String senderVpa = "alice@demo";
        String receiverVpa = "bob@demo";
        BigDecimal amount = new BigDecimal("100.00");

        // Act
        paymentSettlementService.markPaymentSettled(packetId, packetHash, senderVpa, receiverVpa, amount, null);

        // Assert
        ArgumentCaptor<EventEnvelope> eventCaptor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(outboxService).saveEvent(eventCaptor.capture(), anyString(), anyString(), anyString());

        EventEnvelope capturedEvent = eventCaptor.getValue();
        assertEquals("PAYMENT_SETTLED", capturedEvent.getEventType());

        verify(paymentMetrics).incrementPaymentSettled();
        verify(paymentMetrics).decrementPendingSettlements();
    }

    @Test
    @DisplayName("Handle payment failure creates failed event")
    void testHandlePaymentFailure() {
        // Arrange
        String packetId = "packet-123";
        String packetHash = "hash-abc";
        String failureType = "VALIDATION_FAILED";
        String failureReason = "Invalid signature";

        // Act
        paymentSettlementService.handlePaymentFailure(packetId, packetHash, failureType, failureReason, null);

        // Assert
        ArgumentCaptor<EventEnvelope> eventCaptor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(outboxService).saveEvent(eventCaptor.capture(), anyString(), anyString(), anyString());

        EventEnvelope capturedEvent = eventCaptor.getValue();
        assertEquals("PAYMENT_FAILED", capturedEvent.getEventType());

        verify(paymentMetrics).incrementPaymentFailed();
        verify(paymentMetrics).decrementPendingSettlements();
    }
}
