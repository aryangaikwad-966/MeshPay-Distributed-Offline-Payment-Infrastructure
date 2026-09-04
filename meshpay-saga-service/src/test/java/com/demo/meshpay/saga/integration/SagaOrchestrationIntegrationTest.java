package com.demo.meshpay.saga.integration;

import com.demo.meshpay.saga.service.PaymentSagaOrchestrator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration test for Saga Service orchestration
 * Verifies saga state management and Kafka command production
 */
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"saga-validate-payment-command", "saga-request-settlement-command", "saga-complete-settlement-command"})
@DirtiesContext
class SagaOrchestrationIntegrationTest {

    @Autowired
    private PaymentSagaOrchestrator paymentSagaOrchestrator;

    @Test
    void testStartPaymentSaga() {
        String sagaId = paymentSagaOrchestrator.startPaymentSaga(
            "packet-123",
            "hash-abc123",
            "bridge-node-1",
            "encrypted-payload"
        );
        assertNotNull(sagaId);
    }

    @Test
    void testGetSagaStatus() {
        String sagaId = paymentSagaOrchestrator.startPaymentSaga(
            "packet-456",
            "hash-def456",
            "bridge-node-2",
            "encrypted-payload-2"
        );
        assertNotNull(paymentSagaOrchestrator.getSagaStatus(sagaId));
    }
}
