package com.demo.upimesh.consumer;

import com.demo.upimesh.events.CompleteSettlementCommand;
import com.demo.upimesh.events.RequestSettlementCommand;
import com.demo.upimesh.events.ValidatePaymentCommand;
import com.demo.upimesh.service.PaymentSettlementService;
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
            ValidatePaymentCommand command = objectMapper.readValue(message, ValidatePaymentCommand.class);
            log.info("Received validate payment command: sagaId={}, packetId={}", command.getSagaId(), command.getPacketId());
            
            // Execute payment validation
            paymentSettlementService.validatePayment(
                command.getPacketId(),
                command.getPacketHash(),
                command.getSenderVpa(),
                command.getReceiverVpa(),
                command.getAmount(),
                command.getNonce(),
                command.getSignedAt(),
                null // parentEventId - will be derived from saga context
            );
            
            log.info("Successfully processed validate payment command: sagaId={}", command.getSagaId());
        } catch (Exception e) {
            log.error("Failed to process validate payment command", e);
            throw new RuntimeException("Failed to process validate payment command", e);
        }
    }

    @KafkaListener(topics = "saga-request-settlement-command", groupId = "payment-service-group")
    public void handleRequestSettlementCommand(String message) {
        try {
            RequestSettlementCommand command = objectMapper.readValue(message, RequestSettlementCommand.class);
            log.info("Received request settlement command: sagaId={}, packetId={}", command.getSagaId(), command.getPacketId());
            
            // Execute settlement request
            paymentSettlementService.requestSettlement(
                command.getPacketId(),
                command.getPacketHash(),
                command.getSenderVpa(),
                command.getReceiverVpa(),
                command.getAmount(),
                command.getNonce(),
                command.getSignedAt(),
                null // parentEventId
            );
            
            log.info("Successfully processed request settlement command: sagaId={}", command.getSagaId());
        } catch (Exception e) {
            log.error("Failed to process request settlement command", e);
            throw new RuntimeException("Failed to process request settlement command", e);
        }
    }

    @KafkaListener(topics = "saga-complete-settlement-command", groupId = "payment-service-group")
    public void handleCompleteSettlementCommand(String message) {
        try {
            CompleteSettlementCommand command = objectMapper.readValue(message, CompleteSettlementCommand.class);
            log.info("Received complete settlement command: sagaId={}, packetId={}", command.getSagaId(), command.getPacketId());
            
            // Execute settlement completion
            paymentSettlementService.markPaymentSettled(
                command.getPacketId(),
                command.getPacketHash(),
                command.getSenderVpa(),
                command.getReceiverVpa(),
                command.getAmount(),
                null // parentEventId
            );
            
            log.info("Successfully processed complete settlement command: sagaId={}", command.getSagaId());
        } catch (Exception e) {
            log.error("Failed to process complete settlement command", e);
            throw new RuntimeException("Failed to process complete settlement command", e);
        }
    }
}
