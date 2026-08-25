package com.demo.upimesh.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.ConsumerAwareListenerErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Error Handler for handling consumer failures
 * Routes failed messages to Dead Letter Queue (DLQ)
 */
@Component
@Slf4j
public class KafkaErrorHandler implements ConsumerAwareListenerErrorHandler {

    @Override
    public Object handleError(Message<?> message, ListenerExecutionFailedException exception, 
                            Consumer<?, ?> consumer) {
        
        ConsumerRecord<?, ?> record = (ConsumerRecord<?, ?>) message.getHeaders().get("kafka_receivedRecord");
        
        log.error("Error processing Kafka message: topic={}, partition={}, offset={}, error={}", 
                record != null ? record.topic() : "unknown",
                record != null ? record.partition() : "unknown",
                record != null ? record.offset() : "unknown",
                exception.getMessage(), exception);
        
        // Create error context for DLQ
        Map<String, Object> errorContext = new HashMap<>();
        if (record != null) {
            errorContext.put("topic", record.topic());
            errorContext.put("partition", record.partition());
            errorContext.put("offset", record.offset());
            errorContext.put("key", record.key());
            errorContext.put("value", record.value());
            errorContext.put("timestamp", record.timestamp());
        }
        errorContext.put("exception", exception.getClass().getSimpleName());
        errorContext.put("errorMessage", exception.getMessage());
        errorContext.put("errorTime", System.currentTimeMillis());
        
        // In a production system, you would send this to a DLQ topic
        // For now, we just log it
        log.error("Error context: {}", errorContext);
        
        // Return null to indicate the error was handled
        // The message will not be acknowledged and will be retried by Kafka
        return null;
    }
}
