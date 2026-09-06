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

import java.math.BigDecimal;

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
            // In a real implementation, we would extract sagaId from the event or correlation
            log.info("Payment validated, advancing saga for packetHash={}", event.getPacketHash());
            
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
            log.info("Payment settlement requested, advancing saga for packetHash={}", event.getPacketHash());
            
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
            log.info("Payment settled, completing saga for packetHash={}", event.getPacketHash());
            
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
            log.info("Payment failed, compensating saga for packetHash={}, reason={}", 
                event.getPacketHash(), event.getFailureReason());
            
        } catch (Exception e) {
            log.error("Error processing payment failed event", e);
        }
    }
}
