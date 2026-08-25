package com.demo.upimesh.service;

import com.demo.upimesh.events.EventEnvelope;
import com.demo.upimesh.events.PaymentFailedEvent;
import com.demo.upimesh.events.PaymentReceivedEvent;
import com.demo.upimesh.events.PaymentSettledEvent;
import com.demo.upimesh.events.PaymentValidatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service for orchestrating event-driven payment settlement flow
 * Uses the transactional outbox pattern to ensure reliable event publishing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentSettlementService {

    private final OutboxService outboxService;

    /**
     * Process a received payment packet
     * This is the entry point for the payment settlement flow
     */
    @Transactional
    public void processPaymentReceived(String packetId, String packetHash, String bridgeNodeId, String ciphertext) {
        try {
            String eventId = UUID.randomUUID().toString();
            String aggregateId = packetHash;
            
            log.info("Processing payment received: packetId={}, packetHash={}", packetId, packetHash);
            
            // Create the payment received event
            PaymentReceivedEvent paymentReceivedEvent = PaymentReceivedEvent.builder()
                    .packetId(packetId)
                    .packetHash(packetHash)
                    .bridgeNodeId(bridgeNodeId)
                    .receivedAt(java.time.Instant.now())
                    .packetCreatedAt(System.currentTimeMillis())
                    .ttl(3600)
                    .ciphertext(ciphertext)
                    .build();
            
            // Create event envelope
            EventEnvelope eventEnvelope = EventEnvelope.builder()
                    .eventId(eventId)
                    .eventType("PAYMENT_RECEIVED")
                    .aggregateId(aggregateId)
                    .packetHash(packetHash)
                    .timestamp(java.time.Instant.now())
                    .producer("PaymentSettlementService")
                    .schemaVersion("1.0")
                    .payload(paymentReceivedEvent)
                    .build();
            
            // Save to outbox within the same transaction
            outboxService.saveEvent(
                    eventEnvelope, 
                    "payment-received", 
                    packetHash, 
                    "PaymentSettlementService"
            );
            
            log.info("Payment received event saved to outbox: eventId={}", eventId);
            
        } catch (Exception e) {
            log.error("Error processing payment received: packetHash={}", packetHash, e);
            throw new RuntimeException("Failed to process payment received", e);
        }
    }

    /**
     * Validate a payment
     * Called after payment is received
     */
    @Transactional
    public void validatePayment(String packetId, String packetHash, String senderVpa, String receiverVpa, 
                                BigDecimal amount, String nonce, Long signedAt, String parentEventId) {
        try {
            String eventId = UUID.randomUUID().toString();
            String aggregateId = packetHash;
            
            log.info("Validating payment: packetHash={}", packetHash);
            
            // Perform validation logic (simplified for demo)
            boolean isValid = validatePaymentLogic(senderVpa, receiverVpa, amount);
            
            if (isValid) {
                // Create payment validated event
                PaymentValidatedEvent paymentValidatedEvent = PaymentValidatedEvent.builder()
                        .packetId(packetId)
                        .packetHash(packetHash)
                        .senderVpa(senderVpa)
                        .receiverVpa(receiverVpa)
                        .amount(amount)
                        .nonce(nonce)
                        .signedAt(signedAt)
                        .validatedAt(java.time.Instant.now())
                        .build();
                
                // Create event envelope
                EventEnvelope eventEnvelope = EventEnvelope.builder()
                        .eventId(eventId)
                        .eventType("PAYMENT_VALIDATED")
                        .aggregateId(aggregateId)
                        .packetHash(packetHash)
                        .correlationId(parentEventId)
                        .timestamp(java.time.Instant.now())
                        .producer("PaymentSettlementService")
                        .schemaVersion("1.0")
                        .payload(paymentValidatedEvent)
                        .build();
                
                // Save to outbox
                outboxService.saveEvent(
                        eventEnvelope, 
                        "payment-validated", 
                        packetHash, 
                        "PaymentSettlementService"
                );
                
                log.info("Payment validated event saved to outbox: eventId={}", eventId);
                
                // Trigger settlement request
                requestSettlement(packetId, packetHash, senderVpa, receiverVpa, amount, nonce, signedAt, eventId);
                
            } else {
                // Payment validation failed
                handlePaymentFailure(packetId, packetHash, "VALIDATION_FAILED", "Validation failed", parentEventId);
            }
            
        } catch (Exception e) {
            log.error("Error validating payment: packetHash={}", packetHash, e);
            throw new RuntimeException("Failed to validate payment", e);
        }
    }

    /**
     * Request settlement for a validated payment
     */
    @Transactional
    public void requestSettlement(String packetId, String packetHash, String senderVpa, String receiverVpa, 
                                  BigDecimal amount, String nonce, Long signedAt, String parentEventId) {
        try {
            String eventId = UUID.randomUUID().toString();
            String aggregateId = packetHash;
            
            log.info("Requesting settlement: packetHash={}", packetHash);
            
            // Create settlement request event
            com.demo.upimesh.events.PaymentSettlementRequestedEvent settlementRequestedEvent = 
                com.demo.upimesh.events.PaymentSettlementRequestedEvent.builder()
                        .packetId(packetId)
                        .packetHash(packetHash)
                        .senderVpa(senderVpa)
                        .receiverVpa(receiverVpa)
                        .amount(amount)
                        .nonce(nonce)
                        .signedAt(signedAt)
                        .requestedAt(java.time.Instant.now())
                        .build();
            
            // Create event envelope
            EventEnvelope eventEnvelope = EventEnvelope.builder()
                    .eventId(eventId)
                    .eventType("PAYMENT_SETTLEMENT_REQUESTED")
                    .aggregateId(aggregateId)
                    .packetHash(packetHash)
                    .correlationId(parentEventId)
                    .timestamp(java.time.Instant.now())
                    .producer("PaymentSettlementService")
                    .schemaVersion("1.0")
                    .payload(settlementRequestedEvent)
                    .build();
            
            // Save to outbox
            outboxService.saveEvent(
                    eventEnvelope, 
                    "payment-settlement-requested", 
                    packetHash, 
                    "PaymentSettlementService"
            );
            
            log.info("Settlement requested event saved to outbox: eventId={}", eventId);
            
            // In a real system, this would trigger external settlement
            // For demo, we'll directly mark as settled
            markPaymentSettled(packetId, packetHash, senderVpa, receiverVpa, amount, eventId);
            
        } catch (Exception e) {
            log.error("Error requesting settlement: packetHash={}", packetHash, e);
            throw new RuntimeException("Failed to request settlement", e);
        }
    }

    /**
     * Mark a payment as settled
     */
    @Transactional
    public void markPaymentSettled(String packetId, String packetHash, String senderVpa, String receiverVpa, 
                                   BigDecimal amount, String parentEventId) {
        try {
            String eventId = UUID.randomUUID().toString();
            String aggregateId = packetHash;
            
            log.info("Marking payment as settled: packetHash={}", packetHash);
            
            // Create payment settled event
            PaymentSettledEvent paymentSettledEvent = PaymentSettledEvent.builder()
                    .packetId(packetId)
                    .packetHash(packetHash)
                    .senderVpa(senderVpa)
                    .receiverVpa(receiverVpa)
                    .amount(amount)
                    .settledAt(java.time.Instant.now())
                    .build();
            
            // Create event envelope
            EventEnvelope eventEnvelope = EventEnvelope.builder()
                    .eventId(eventId)
                    .eventType("PAYMENT_SETTLED")
                    .aggregateId(aggregateId)
                    .packetHash(packetHash)
                    .correlationId(parentEventId)
                    .timestamp(java.time.Instant.now())
                    .producer("PaymentSettlementService")
                    .schemaVersion("1.0")
                    .payload(paymentSettledEvent)
                    .build();
            
            // Save to outbox
            outboxService.saveEvent(
                    eventEnvelope, 
                    "payment-settled", 
                    packetHash, 
                    "PaymentSettlementService"
            );
            
            log.info("Payment settled event saved to outbox: eventId={}", eventId);
            
        } catch (Exception e) {
            log.error("Error marking payment as settled: packetHash={}", packetHash, e);
            throw new RuntimeException("Failed to mark payment as settled", e);
        }
    }

    /**
     * Handle payment failure
     */
    @Transactional
    public void handlePaymentFailure(String packetId, String packetHash, String failureType, String failureReason, String parentEventId) {
        try {
            String eventId = UUID.randomUUID().toString();
            String aggregateId = packetHash;
            
            log.error("Handling payment failure: packetHash={}, failureType={}", packetHash, failureType);
            
            // Create payment failed event
            PaymentFailedEvent paymentFailedEvent = PaymentFailedEvent.builder()
                    .packetId(packetId)
                    .packetHash(packetHash)
                    .failureReason(failureReason)
                    .failureType(failureType)
                    .failedAt(java.time.Instant.now())
                    .build();
            
            // Create event envelope
            EventEnvelope eventEnvelope = EventEnvelope.builder()
                    .eventId(eventId)
                    .eventType("PAYMENT_FAILED")
                    .aggregateId(aggregateId)
                    .packetHash(packetHash)
                    .correlationId(parentEventId)
                    .timestamp(java.time.Instant.now())
                    .producer("PaymentSettlementService")
                    .schemaVersion("1.0")
                    .payload(paymentFailedEvent)
                    .build();
            
            // Save to outbox
            outboxService.saveEvent(
                    eventEnvelope, 
                    "payment-failed", 
                    packetHash, 
                    "PaymentSettlementService"
            );
            
            log.info("Payment failed event saved to outbox: eventId={}", eventId);
            
        } catch (Exception e) {
            log.error("Error handling payment failure: packetHash={}", packetHash, e);
            throw new RuntimeException("Failed to handle payment failure", e);
        }
    }

    /**
     * Simplified validation logic
     * In a real system, this would check balances, limits, KYC, etc.
     */
    private boolean validatePaymentLogic(String senderVpa, String receiverVpa, BigDecimal amount) {
        // Simplified validation - always return true for demo
        // In production, implement actual validation logic
        log.debug("Validating payment: sender={}, receiver={}, amount={}", senderVpa, receiverVpa, amount);
        return true;
    }
}
