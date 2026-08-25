package com.demo.upimesh.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Event emitted when a payment processing fails
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFailedEvent {
    
    private String packetId;
    private String packetHash;
    private String failureReason;
    private String failureType; // INSUFFICIENT_FUNDS, INVALID_SIGNATURE, STALE_PACKET, DUPLICATE, etc.
    private Instant failedAt;
}
