# MeshPay Microservices Architecture

## Overview

MeshPay is implemented as a microservices architecture with service discovery, API gateway, and event-driven communication between services.

## Architecture

### Services

| Service | Port | Description |
|---------|------|-------------|
| Eureka Server | 8761 | Service discovery and registration |
| API Gateway | 8080 | Single entry point, routing, cross-cutting concerns |
| Payment Service | 8081 | Core payment processing, accounts, transactions |
| Saga Service | 8082 | Distributed transaction orchestration |

### Infrastructure

| Component | Port | Description |
|-----------|------|-------------|
| MySQL | 3306 | Primary database for Payment Service |
| Redis | 6379 | Distributed caching and idempotency |
| Kafka | 9092 | Event streaming between services |
| Zookeeper | 2181 | Kafka coordination |

## Service Communication

### API Gateway Routing

The API Gateway routes requests based on path patterns:

- `/api/payments/**` → Payment Service
- `/api/sagas/**` → Saga Service
- `/api/mesh/**` → Payment Service (mesh network endpoints)

### Service Discovery

- All services register with Eureka on startup
- Gateway discovers services dynamically via Eureka
- Services communicate via service names (not hardcoded URLs)

### Event-Driven Communication

Services communicate asynchronously via Kafka:

1. Payment Service publishes events to Kafka topics
2. Saga Service consumes events and orchestrates transactions
3. Compensation events are published for failure recovery

## Deployment

### Local Development

```bash
# Start infrastructure
docker-compose -f docker-compose-microservices.yml up -d mysql redis kafka zookeeper

# Start Eureka Server
./mvnw spring-boot:run -Dspring-boot.run.profiles=eureka

# Start Payment Service
./mvnw spring-boot:run -Dspring-boot.run.profiles=payment

# Start Saga Service
./mvnw spring-boot:run -Dspring-boot.run.profiles=saga

# Start API Gateway
./mvnw spring-boot:run -Dspring-boot.run.profiles=gateway
```

### Docker Deployment

```bash
# Build all services
docker-compose -f docker-compose-microservices.yml build

# Start all services
docker-compose -f docker-compose-microservices.yml up -d
```

## Configuration

### Eureka Server

- Port: 8761
- Profile: `eureka`
- Config: `application-eureka.properties`

### API Gateway

- Port: 8080
- Profile: `gateway`
- Config: `application-gateway.properties`
- Eureka URL: `http://localhost:8761/eureka/`

### Payment Service

- Port: 8081
- Profile: `payment`
- Registers as: `payment-service`
- Database: MySQL (meshpay_payment)
- Cache: Redis
- Kafka: localhost:9092

### Saga Service

- Port: 8082
- Profile: `saga`
- Registers as: `saga-service`
- Kafka: localhost:9092

## Scalability

### Horizontal Scaling

Each service can be scaled independently:

```bash
# Scale Payment Service to 3 instances
docker-compose -f docker-compose-microservices.yml up -d --scale payment-service=3
```

### Load Balancing

- Eureka provides service discovery
- Gateway performs client-side load balancing
- Services can register multiple instances

## Monitoring

### Health Checks

- Eureka Dashboard: http://localhost:8761
- Gateway Health: http://localhost:8080/actuator/health
- Payment Service Health: http://localhost:8081/actuator/health
- Saga Service Health: http://localhost:8082/actuator/health

### Metrics

- Prometheus: http://localhost:8080/actuator/prometheus
- Custom metrics: Payment flow, saga lifecycle, failures

### Distributed Tracing

- OpenTelemetry traces flow across services
- Jaeger/Tempo integration for visualization

## Security

### Service-to-Service Communication

- Services communicate via internal network
- Gateway handles external authentication
- JWT tokens validated at gateway level

### Kafka Security

- Trusted packages restricted for deserialization
- SASL/SSL can be enabled for production

## Data Ownership

### Payment Service

- Owns: accounts, transactions, outbox events
- Database: meshpay_payment
- Event topics: payment-received, payment-validated

### Saga Service

- Owns: saga state, compensation logic
- Event topics: payment-settlement-requested, payment-settled, payment-failed

## Failure Handling

### Service Failure

- Circuit breakers prevent cascading failures
- Retry logic with exponential backoff
- Dead letter queue for failed events

### Network Partitions

- Eureka handles service registration changes
- Kafka provides message persistence
- Saga pattern ensures eventual consistency
