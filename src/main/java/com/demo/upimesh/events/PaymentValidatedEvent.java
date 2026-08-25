package com.demo.upimesh.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event emitted when a payment packet is successfully validated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentValidatedEvent {
    
    private String packetId;
    private String packetHash;
    private String senderVpa;
    private String receiverVpa;
    private BigDecimal amount;
    private String nonce;
    private Long signedAt;
    private Instant validatedAt;
}
