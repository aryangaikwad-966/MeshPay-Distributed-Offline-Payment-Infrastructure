# MeshPay Current Architecture State

## Phase 0: Current State Analysis

### Repository Structure

**Package Organization:**
```
com.demo.upimesh/
├── MeshPayApplication.java (Main monolithic application)
├── config/ (13 files)
│   ├── ActuatorConfig.java
│   ├── AppConfig.java
│   ├── HttpClientConfig.java
│   ├── KafkaConfig.java
│   ├── KafkaErrorHandler.java
│   ├── KafkaRetryConfig.java
│   ├── OpenApiConfig.java
│   ├── RateLimiterConfig.java
│   ├── RateLimiterConfigBean.java
│   ├── RedisConfig.java
│   ├── ResilienceConfig.java
│   ├── SecurityConfig.java
│   └── TracingConfig.java
├── consumer/ (3 files)
│   ├── DeadLetterQueueConsumer.java
│   ├── IdempotentConsumer.java
│   └── PaymentEventConsumer.java
├── controller/ (3 files)
│   ├── ApiController.java
│   ├── DashboardController.java
│   └── GlobalExceptionHandler.java
├── crypto/ (2 files)
│   ├── HybridCryptoService.java
│   └── ServerKeyHolder.java
├── discovery/ (1 file)
│   └── EurekaServerApplication.java (Infrastructure only)
├── events/ (6 files)
│   ├── EventEnvelope.java
│   ├── PaymentFailedEvent.java
│   ├── PaymentReceivedEvent.java
│   ├── PaymentSettledEvent.java
│   ├── PaymentSettlementRequestedEvent.java
│   └── PaymentValidatedEvent.java
├── gateway/ (1 file)
│   └── GatewayApplication.java (Infrastructure only)
├── health/ (1 file)
│   └── KafkaHealthIndicator.java
├── metrics/ (1 file)
│   └── PaymentMetrics.java
├── model/ (10 files)
│   ├── Account.java
│   ├── AccountRepository.java
│   ├── ErrorResponse.java
│   ├── EventProcessed.java
│   ├── InsufficientFundsException.java
│   ├── MeshPacket.java
│   ├── Outbox.java
│   ├── PaymentInstruction.java
│   ├── Transaction.java
│   └── TransactionRepository.java
├── processor/ (1 file)
│   └── OutboxEventProcessor.java
├── repository/ (2 files)
│   ├── AccountRepository.java
│   └── TransactionRepository.java
├── saga/ (3 files)
│   ├── PaymentSagaOrchestrator.java
│   ├── SagaStateManager.java
│   └── SagaState.java
├── security/ (1 file)
│   └── AuditLogFilter.java
└── service/ (10 files)
    ├── BridgeIngestionService.java
    ├── DemoService.java
    ├── IdempotencyService.java
    ├── KafkaProducerService.java
    ├── KmsKeyService.java
    ├── MeshSimulatorService.java
    ├── OutboxService.java
    ├── PaymentSettlementService.java
    ├── SettlementService.java
    └── VirtualDevice.java
```

### Current Architecture

**Single Spring Boot Application (Monolith):**
- Port: 8080 (default)
- Database: MySQL 8.0
- Cache: Redis 7
- Message Broker: Apache Kafka
- All components in single deployable JAR

### Controllers

**ApiController:**
- `GET /api/server-key` - Fetch server public key
- `POST /api/demo/send` - Demo packet injection
- `GET /api/mesh/state` - Mesh network state
- `POST /api/mesh/gossip` - Simulate gossip protocol
- `POST /api/mesh/flush` - Bridge node flush (concurrent idempotency test)
- `POST /api/mesh/reset` - Reset mesh simulation
- `POST /api/bridge/ingest` - Production bridge ingestion endpoint
- `GET /api/accounts` - List accounts
- `GET /api/transactions` - List transactions

**DashboardController:**
- Serves Thymeleaf dashboard UI at root path

**GlobalExceptionHandler:**
- Centralized error handling

### Services

**Payment Processing:**
- `BridgeIngestionService` - Handles bridge node packet ingestion with idempotency
- `PaymentSettlementService` - Orchestrates event-driven payment settlement flow
- `SettlementService` - Actual settlement logic (balance updates)
- `IdempotencyService` - Redis-based idempotency (SETNX)

**Event-Driven:**
- `OutboxService` - Transactional outbox pattern implementation
- `KafkaProducerService` - Kafka event publishing
- `OutboxEventProcessor` - Scheduled outbox event processor

**Saga:**
- `PaymentSagaOrchestrator` - Saga orchestration with compensation logic
- `SagaStateManager` - In-memory saga state management (ConcurrentHashMap)

**Mesh Simulation:**
- `MeshSimulatorService` - Virtual mesh network simulation
- `VirtualDevice` - Virtual device representation
- `DemoService` - Demo packet creation

**Crypto:**
- `HybridCryptoService` - RSA-2048/OAEP + AES-256-GCM hybrid encryption
- `ServerKeyHolder` - Server key management

**Other:**
- `KmsKeyService` - AWS KMS integration (optional)
- `AuditLogFilter` - Security audit logging

### Entities

**Payment Domain:**
- `Account` - User account entity
- `Transaction` - Payment transaction entity
- `MeshPacket` - Encrypted mesh packet
- `PaymentInstruction` - Decrypted payment instruction

**Event-Driven:**
- `Outbox` - Outbox event entity
- `EventProcessed` - Kafka event idempotency tracking

**Saga:**
- `SagaState` - Saga state entity (currently in-memory only)

### Kafka Configuration

**Topics:**
- `payment-received` - Initial payment events
- `payment-validated` - Validation completion events
- `payment-settlement-requested` - Settlement request events
- `payment-settled` - Final settlement events
- `payment-failed` - Failure events
- `payment-retry` - Retry events
- `payment-events-dlq` - Dead letter queue

**Consumer Group:**
- `meshpay-settlement-group`

**Security:**
- Trusted packages: `com.demo.upimesh.events,com.demo.upimesh.model` (fixed from "*")

### Resilience4j Configuration

**Implemented:**
- Circuit breakers
- Retries
- Rate limiting
- Time limiters

### Observability

**Metrics (PaymentMetrics):**
- Payment received/validated/settled/failed counts
- Saga lifecycle metrics (started, completed, compensated, active)
- Settlement/validation timers
- Pending settlements count

**Distributed Tracing:**
- OpenTelemetry with OTLP exporter
- Micrometer Tracing bridge

**Health:**
- Kafka health indicator
- Actuator endpoints

### Security

**Authentication:**
- OAuth2/JWT with JWKS URI
- Role-based access control (BRIDGE_NODE, USER, ADMIN)

**Encryption:**
- RSA-2048/OAEP for key wrapping
- AES-256-GCM for payload encryption

**Network Security:**
- HTTPS enforcement
- CORS configuration
- CSRF disabled (stateless API)

### Database Schema

**MySQL Tables:**
- `account` - User accounts
- `transaction` - Payment transactions
- `outbox` - Outbox events
- `event_processed` - Kafka event idempotency

**Note:** Saga state is currently in-memory (ConcurrentHashMap), not persisted.

### Transaction Boundaries

**Current Transactional Methods:**
- `BridgeIngestionService.ingest()` - Packet ingestion with idempotency
- `PaymentSettlementService.processPaymentReceived()` - Payment received event creation
- `PaymentSettlementService.validatePayment()` - Payment validation
- `PaymentSettlementService.requestSettlement()` - Settlement request
- `PaymentSettlementService.markPaymentSettled()` - Settlement completion
- `PaymentSettlementService.handlePaymentFailure()` - Failure handling
- `PaymentSagaOrchestrator.startPaymentSaga()` - Saga initialization
- `PaymentSagaOrchestrator.validatePaymentSaga()` - Saga validation step
- `PaymentSagaOrchestrator.requestSettlementSaga()` - Saga settlement step
- `PaymentSagaOrchestrator.completePaymentSaga()` - Saga completion
- `PaymentSagaOrchestrator.handleSagaFailure()` - Saga failure compensation

### Dependencies Between Payment and Saga Logic

**Tight Coupling:**
- `PaymentSagaOrchestrator` directly calls `PaymentSettlementService` methods
- Saga state is in-memory, not persisted
- No clear separation between payment business logic and saga orchestration
- Both services share the same database and Kafka configuration

### Microservices Infrastructure (Not Yet Active)

**Created but Not Used:**
- `EurekaServerApplication.java` - Service discovery server
- `GatewayApplication.java` - API Gateway
- `application-eureka.properties` - Eureka configuration
- `application-gateway.properties` - Gateway configuration
- `docker-compose-microservices.yml` - Docker Compose for microservices
- Spring Cloud dependencies in pom.xml

**Current State:**
- Infrastructure exists but monolith still runs as single application
- Payment and Saga logic still in same JAR
- No independent service deployment
- No actual service discovery or gateway routing

### Key Issues for Microservices Extraction

1. **Saga State Management:**
   - Currently in-memory (ConcurrentHashMap)
   - Needs to be persisted to database for true microservices

2. **Tight Coupling:**
   - PaymentSagaOrchestrator directly calls PaymentSettlementService
   - Need to decouple via Kafka events or REST API

3. **Shared Database:**
   - All services currently share same MySQL instance
   - Need to establish clear ownership boundaries

4. **Demo/Simulation Code:**
   - MeshSimulatorService and DemoService are for demo/testing
   - Need to decide if these belong in Payment Service or separate demo service

5. **Transaction Boundaries:**
   - Current transactions span multiple concerns
   - Need to refine for microservices boundaries

### Test Coverage

**Existing Tests:**
- `PaymentSettlementServiceTest.java` - Unit tests for payment settlement
- `PaymentSagaOrchestratorTest.java` - Unit tests for saga orchestration

### Configuration Files

**application.properties:**
- Database configuration (MySQL)
- Redis configuration
- Kafka configuration
- Security configuration
- Resilience4j configuration
- OpenTelemetry configuration
- Eureka client configuration (added but not active)

### Docker Configuration

**docker-compose.yml:**
- MySQL
- Redis
- Kafka
- Zookeeper

**docker-compose-microservices.yml:**
- Eureka Server
- API Gateway
- Payment Service
- Saga Service
- MySQL
- Redis
- Kafka
- Zookeeper

### Summary

**Current State:**
- Single monolithic Spring Boot application
- All distributed systems patterns implemented within monolith
- Microservices infrastructure created but not actively used
- Saga state in-memory (needs persistence)
- Tight coupling between Payment and Saga logic
- Shared database with no clear ownership boundaries

**Migration Requirements:**
1. Persist saga state to database
2. Decouple Payment and Saga services via Kafka/REST
3. Establish clear database ownership
4. Create independent deployable services
5. Configure active service discovery and gateway routing
6. Preserve all existing functionality and guarantees
