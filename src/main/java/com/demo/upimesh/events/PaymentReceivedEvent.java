package com.demo.upimesh.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event emitted when a payment packet is received from a bridge node
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentReceivedEvent {
    
    private String packetId;
    private String packetHash;
    private String bridgeNodeId;
    private Instant receivedAt;
    private Long packetCreatedAt;
    private int ttl;
    private String ciphertext; // Encrypted payload (not decrypted yet)
}
