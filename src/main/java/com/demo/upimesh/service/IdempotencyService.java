package com.demo.upimesh.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Idempotency Service - Ensures payments are settled exactly once
 * 
 * Production: Uses Redis for distributed idempotency across multiple server instances
 * Development: Falls back to in-memory ConcurrentHashMap for single instance
 * 
 * Strategy: Idempotency key = SHA256(ciphertext), not packetId
 * Reason: Intermediate nodes cannot forge valid ciphertexts, so this is tamper-proof
 */
@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private final Map<String, Instant> seen = new ConcurrentHashMap<>();
    private final RedisTemplate<String, String> redisTemplate;
    private final long ttlSeconds;
    private final boolean useRedis;

    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:";

    @Autowired
    public IdempotencyService(
            @Autowired(required = false) RedisTemplate<String, String> redisTemplate,
            @Value("${upi.mesh.idempotency-ttl-seconds:3600}") long ttlSeconds,
            @Value("${spring.redis.enabled:false}") boolean redisEnabled) {
        
        this.redisTemplate = redisTemplate;
        this.ttlSeconds = ttlSeconds;
        this.useRedis = redisEnabled && redisTemplate != null;
        
        if (useRedis) {
            log.info("Idempotency Service initialized with Redis (distributed mode)");
        } else {
            log.warn("Idempotency Service initialized with in-memory cache (NOT suitable for horizontally scaled deployments)");
        }
    }

    /**
     * Try to claim a hash. Returns true if this caller is the first; false if
     * someone else already claimed it (i.e. the packet is a duplicate).
     * 
     * This is atomic and thread-safe across distributed instances (when Redis is enabled)
     */
    public boolean claim(String packetHash) {
        if (useRedis) {
            return claimWithRedis(packetHash);
        } else {
            return claimInMemory(packetHash);
        }
    }

    /**
     * Claim using Redis (distributed)
     */
    private boolean claimWithRedis(String packetHash) {
        try {
            String key = IDEMPOTENCY_KEY_PREFIX + packetHash;
            Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(key, "claimed", Duration.ofSeconds(ttlSeconds));
            
            boolean claimed = result != null && result;
            if (claimed) {
                log.debug("Idempotency claim GRANTED: {}", packetHash.substring(0, 8));
            } else {
                log.debug("Idempotency claim DUPLICATE: {}", packetHash.substring(0, 8));
            }
            return claimed;
        } catch (Exception e) {
            log.error("Redis idempotency check failed: {}. Falling back to in-memory.", e.getMessage());
            return claimInMemory(packetHash);
        }
    }

    /**
     * Claim using in-memory ConcurrentHashMap
     */
    private boolean claimInMemory(String packetHash) {
        Instant now = Instant.now();
        Instant prev = seen.putIfAbsent(packetHash, now);
        boolean claimed = prev == null;
        
        if (claimed) {
            log.debug("Idempotency claim GRANTED (in-memory): {}", packetHash.substring(0, 8));
        } else {
            log.debug("Idempotency claim DUPLICATE (in-memory): {}", packetHash.substring(0, 8));
        }
        return claimed;
    }

    public int size() {
        return useRedis ? 
            (redisTemplate.keys(IDEMPOTENCY_KEY_PREFIX + "*").size()) :
            seen.size();
    }

    /** Periodically evict entries past their TTL so the map doesn't grow forever. */
    @Scheduled(fixedDelay = 60_000)
    public void evictExpired() {
        if (!useRedis) {
            Instant cutoff = Instant.now().minusSeconds(ttlSeconds);
            seen.entrySet().removeIf(e -> e.getValue().isBefore(cutoff));
            log.debug("Evicted expired idempotency entries. Current size: {}", seen.size());
        }
    }

    /** Test/demo helper - use with caution! */
    public void clear() {
        log.warn("Clearing all idempotency claims");
        if (useRedis) {
            try {
                redisTemplate.delete(redisTemplate.keys(IDEMPOTENCY_KEY_PREFIX + "*"));
            } catch (Exception e) {
                log.error("Failed to clear Redis claims: {}", e.getMessage());
            }
        } else {
            seen.clear();
        }
    }
}
