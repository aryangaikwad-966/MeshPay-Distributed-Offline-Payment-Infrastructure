package com.demo.upimesh.health;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.common.KafkaFuture;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Kafka Health Indicator
 * Checks if Kafka broker is accessible and cluster is healthy
 */
@Component
public class KafkaHealthIndicator implements HealthIndicator {

    private final KafkaAdmin kafkaAdmin;

    public KafkaHealthIndicator(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    @Override
    public Health health() {
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            DescribeClusterOptions options = new DescribeClusterOptions()
                    .timeoutMs(5000);
            
            DescribeClusterResult clusterResult = adminClient.describeCluster(options);
            
            KafkaFuture<String> clusterId = clusterResult.clusterId();
            KafkaFuture<Integer> nodeCount = clusterResult.nodes().thenApply(nodes -> nodes.size());
            
            String id = clusterId.get(5, TimeUnit.SECONDS);
            Integer nodes = nodeCount.get(5, TimeUnit.SECONDS);
            
            return Health.up()
                    .withDetail("clusterId", id)
                    .withDetail("nodeCount", nodes)
                    .withDetail("status", "connected")
                    .build();
                    
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("status", "disconnected")
                    .build();
        }
    }
}
