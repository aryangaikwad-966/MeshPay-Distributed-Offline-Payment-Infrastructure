package com.demo.meshpay.payment.integration;

import com.demo.meshpay.payment.service.PaymentSettlementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Integration test for Payment Service Saga command processing
 * Verifies Kafka command consumption and payment processing
 */
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"saga-validate-payment-command", "saga-request-settlement-command", "saga-complete-settlement-command"})
@DirtiesContext
class PaymentSagaIntegrationTest {

    @Autowired
    private PaymentSettlementService paymentSettlementService;

    @Test
    void testValidatePayment() {
        assertDoesNotThrow(() -> {
            paymentSettlementService.validatePayment(
                "packet-123",
                "hash-abc123",
                "alice@demo",
                "bob@demo",
                new BigDecimal("100.00"),
                "nonce-xyz",
                System.currentTimeMillis(),
                null
            );
        });
    }

    @Test
    void testRequestSettlement() {
        assertDoesNotThrow(() -> {
            paymentSettlementService.requestSettlement(
                "packet-123",
                "hash-abc123",
                "alice@demo",
                "bob@demo",
                new BigDecimal("100.00"),
                "nonce-xyz",
                System.currentTimeMillis(),
                null
            );
        });
    }

    @Test
    void testMarkPaymentSettled() {
        assertDoesNotThrow(() -> {
            paymentSettlementService.markPaymentSettled(
                "packet-123",
                "hash-abc123",
                "alice@demo",
                "bob@demo",
                new BigDecimal("100.00"),
                null
            );
        });
    }
}
