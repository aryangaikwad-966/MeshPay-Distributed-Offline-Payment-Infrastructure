package com.demo.meshpay.payment.consumer;

import com.demo.meshpay.payment.service.PaymentSettlementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for Saga commands from Saga Service
 * Enables decoupled microservices communication
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SagaCommandConsumer {

    private final PaymentSettlementService paymentSettlementService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "saga-validate-payment-command", groupId = "payment-service-group")
    public void handleValidatePaymentCommand(String message) {
        try {
            // Parse command (simplified - in production use proper command classes)
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(message);
            String sagaId = node.get("sagaId").asText();
            String packetId = node.get("packetId").asText();
            String packetHash = node.get("packetHash").asText();
            String senderVpa = node.get("senderVpa").asText();
            String receiverVpa = node.get("receiverVpa").asText();
            java.math.BigDecimal amount = node.get("amount").decimalValue();
            String nonce = node.get("nonce").asText();
            Long signedAt = node.get("signedAt").asLong();
            
            log.info("Received validate payment command: sagaId={}, packetId={}", sagaId, packetId);
            
            // Execute payment validation
            paymentSettlementService.validatePayment(
                packetId,
                packetHash,
                senderVpa,
                receiverVpa,
                amount,
                nonce,
                signedAt,
                null // parentEventId - will be derived from saga context
            );
            
            log.info("Successfully processed validate payment command: sagaId={}", sagaId);
        } catch (Exception e) {
            log.error("Failed to process validate payment command", e);
            throw new RuntimeException("Failed to process validate payment command", e);
        }
    }

    @KafkaListener(topics = "saga-request-settlement-command", groupId = "payment-service-group")
    public void handleRequestSettlementCommand(String message) {
        try {
            // Parse command
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(message);
            String sagaId = node.get("sagaId").asText();
            String packetId = node.get("packetId").asText();
            String packetHash = node.get("packetHash").asText();
            String senderVpa = node.get("senderVpa").asText();
            String receiverVpa = node.get("receiverVpa").asText();
            java.math.BigDecimal amount = node.get("amount").decimalValue();
            String nonce = node.get("nonce").asText();
            Long signedAt = node.get("signedAt").asLong();
            
            log.info("Received request settlement command: sagaId={}, packetId={}", sagaId, packetId);
            
            // Execute settlement request
            paymentSettlementService.requestSettlement(
                packetId,
                packetHash,
                senderVpa,
                receiverVpa,
                amount,
                nonce,
                signedAt,
                null // parentEventId
            );
            
            log.info("Successfully processed request settlement command: sagaId={}", sagaId);
        } catch (Exception e) {
            log.error("Failed to process request settlement command", e);
            throw new RuntimeException("Failed to process request settlement command", e);
        }
    }

    @KafkaListener(topics = "saga-complete-settlement-command", groupId = "payment-service-group")
    public void handleCompleteSettlementCommand(String message) {
        try {
            // Parse command
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(message);
            String sagaId = node.get("sagaId").asText();
            String packetId = node.get("packetId").asText();
            String packetHash = node.get("packetHash").asText();
            String senderVpa = node.get("senderVpa").asText();
            String receiverVpa = node.get("receiverVpa").asText();
            java.math.BigDecimal amount = node.get("amount").decimalValue();
            
            log.info("Received complete settlement command: sagaId={}, packetId={}", sagaId, packetId);
            
            // Execute settlement completion
            paymentSettlementService.markPaymentSettled(
                packetId,
                packetHash,
                senderVpa,
                receiverVpa,
                amount,
                null // parentEventId
            );
            
            log.info("Successfully processed complete settlement command: sagaId={}", sagaId);
        } catch (Exception e) {
            log.error("Failed to process complete settlement command", e);
            throw new RuntimeException("Failed to process complete settlement command", e);
        }
    }
}
