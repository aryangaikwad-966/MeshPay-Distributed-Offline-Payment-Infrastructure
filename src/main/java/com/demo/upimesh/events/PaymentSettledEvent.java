package com.demo.upimesh.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event emitted when a payment is successfully settled
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSettledEvent {
    
    private String packetId;
    private String packetHash;
    private String transactionId;
    private String senderVpa;
    private String receiverVpa;
    private BigDecimal amount;
    private Instant settledAt;
    private Long senderBalanceAfter;
    private Long receiverBalanceAfter;
}
