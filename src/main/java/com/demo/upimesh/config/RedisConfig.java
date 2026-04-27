package com.demo.upimesh.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis Configuration for distributed caching and idempotency
 */
@Configuration
@ConditionalOnProperty(name = "spring.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    /**
     * Configure Redis connection factory with connection pooling
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory(
            org.springframework.boot.autoconfigure.data.redis.RedisProperties redisProperties) {
        
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisProperties.getHost());
        config.setPort(redisProperties.getPort());
        
        if (redisProperties.getPassword() != null && !redisProperties.getPassword().isEmpty()) {
            config.setPassword(redisProperties.getPassword());
        }
        
        config.setDatabase(redisProperties.getDatabase());
        
        JedisClientConfiguration.JedisClientConfigurationBuilder builder = JedisClientConfiguration.builder();
        builder.connectTimeout(Duration.ofMillis(redisProperties.getTimeout().toMillis()));
        
        if (redisProperties.getJedis() != null) {
            builder.usePooling()
                .poolConfig(redisProperties.getJedis().getPool() != null ? 
                    new redis.clients.jedis.JedisPoolConfig() : null);
        }
        
        return new JedisConnectionFactory(config, builder.build());
    }

    /**
     * RedisTemplate bean for string operations (used by IdempotencyService)
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serializer for keys
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        
        // Use JSON serializer for values
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        log.info("RedisTemplate configured with String keys and JSON values");
        
        return template;
    }
}
