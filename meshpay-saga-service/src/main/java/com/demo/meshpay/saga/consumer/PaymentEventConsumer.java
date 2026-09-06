package com.demo.meshpay.saga.consumer;

import com.demo.meshpay.saga.events.PaymentFailedEvent;
import com.demo.meshpay.saga.events.PaymentSettlementRequestedEvent;
import com.demo.meshpay.saga.events.PaymentSettledEvent;
import com.demo.meshpay.saga.events.PaymentValidatedEvent;
import com.demo.meshpay.saga.service.PaymentSagaOrchestrator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for payment events from Payment Service
 * Handles payment lifecycle events to advance saga state
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final PaymentSagaOrchestrator paymentSagaOrchestrator;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment-validated", groupId = "saga-service-group")
    public void handlePaymentValidated(String message) {
        try {
            log.info("Received payment validated event: {}", message);
            PaymentValidatedEvent event = objectMapper.readValue(message, PaymentValidatedEvent.class);
            
            // Advance saga to settlement request
            // Use packetHash as correlation ID to find the saga
            String sagaId = paymentSagaOrchestrator.findSagaByPacketHash(event.getPacketHash());
            if (sagaId != null) {
                paymentSagaOrchestrator.requestSettlementSaga(
                    sagaId,
                    event.getPacketId(),
                    event.getPacketHash(),
                    event.getSenderVpa(),
                    event.getReceiverVpa(),
                    event.getAmount(),
                    event.getNonce(),
                    event.getSignedAt()
                );
                log.info("Payment validated, advanced saga to settlement request: sagaId={}", sagaId);
            } else {
                log.warn("No saga found for packetHash={}", event.getPacketHash());
            }
            
        } catch (Exception e) {
            log.error("Error processing payment validated event", e);
        }
    }

    @KafkaListener(topics = "payment-settlement-requested", groupId = "saga-service-group")
    public void handlePaymentSettlementRequested(String message) {
        try {
            log.info("Received payment settlement requested event: {}", message);
            PaymentSettlementRequestedEvent event = objectMapper.readValue(message, PaymentSettlementRequestedEvent.class);
            
            // Advance saga to completion
            String sagaId = paymentSagaOrchestrator.findSagaByPacketHash(event.getPacketHash());
            if (sagaId != null) {
                log.info("Payment settlement requested, saga already in progress: sagaId={}", sagaId);
            } else {
                log.warn("No saga found for packetHash={}", event.getPacketHash());
            }
            
        } catch (Exception e) {
            log.error("Error processing payment settlement requested event", e);
        }
    }

    @KafkaListener(topics = "payment-settled", groupId = "saga-service-group")
    public void handlePaymentSettled(String message) {
        try {
            log.info("Received payment settled event: {}", message);
            PaymentSettledEvent event = objectMapper.readValue(message, PaymentSettledEvent.class);
            
            // Mark saga as completed
            String sagaId = paymentSagaOrchestrator.findSagaByPacketHash(event.getPacketHash());
            if (sagaId != null) {
                paymentSagaOrchestrator.completePaymentSaga(
                    sagaId,
                    event.getPacketId(),
                    event.getPacketHash(),
                    event.getSenderVpa(),
                    event.getReceiverVpa(),
                    event.getAmount()
                );
                log.info("Payment settled, saga completed: sagaId={}", sagaId);
            } else {
                log.warn("No saga found for packetHash={}", event.getPacketHash());
            }
            
        } catch (Exception e) {
            log.error("Error processing payment settled event", e);
        }
    }

    @KafkaListener(topics = "payment-failed", groupId = "saga-service-group")
    public void handlePaymentFailed(String message) {
        try {
            log.info("Received payment failed event: {}", message);
            PaymentFailedEvent event = objectMapper.readValue(message, PaymentFailedEvent.class);
            
            // Mark saga as failed and trigger compensation
            String sagaId = paymentSagaOrchestrator.findSagaByPacketHash(event.getPacketHash());
            if (sagaId != null) {
                paymentSagaOrchestrator.compensatePaymentSaga(
                    sagaId,
                    event.getPacketHash(),
                    event.getFailureReason()
                );
                log.info("Payment failed, saga compensated: sagaId={}, reason={}", 
                    sagaId, event.getFailureReason());
            } else {
                log.warn("No saga found for packetHash={}", event.getPacketHash());
            }
            
        } catch (Exception e) {
            log.error("Error processing payment failed event", e);
        }
    }
}
