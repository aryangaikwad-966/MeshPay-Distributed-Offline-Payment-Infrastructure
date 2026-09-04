# MeshPay Service Boundaries

## Phase 1: Service Boundary Definition

### Service Overview

**4 Services:**
1. meshpay-eureka-server (Port 8761)
2. meshpay-api-gateway (Port 8080)
3. meshpay-payment-service (Port 8081)
4. meshpay-saga-service (Port 8082)

---

## 1. EUREKA SERVER

### Purpose
Service discovery and registration for microservices.

### Responsibilities
- Service registration
- Service discovery
- Health monitoring
- Load balancing support

### Components
- Eureka Server only
- No business logic

### Port
8761

### Dependencies
- Spring Cloud Eureka Server

---

## 2. API GATEWAY

### Purpose
Single entry point for all client requests, routing to appropriate microservices.

### Responsibilities
- Request routing
- Load balancing
- Authentication/authorization (JWT validation)
- Rate limiting
- CORS handling
- Request/response transformation

### Routes
- `/api/server-key` → Payment Service
- `/api/demo/**` → Payment Service (demo endpoints)
- `/api/mesh/**` → Payment Service (mesh simulation)
- `/api/bridge/ingest` → Payment Service (production endpoint)
- `/api/accounts` → Payment Service
- `/api/transactions` → Payment Service
- `/api/sagas/**` → Saga Service (saga management endpoints)

### Port
8080

### Dependencies
- Spring Cloud Gateway
- Eureka Client
- Spring Security (JWT validation)

### Components
- GatewayApplication.java
- Gateway configuration (routes, filters)
- Security configuration (JWT validation only)

---

## 3. PAYMENT SERVICE

### Purpose
Core payment processing, account management, and transaction settlement.

### Responsibilities
- Payment ingestion from bridge nodes
- Packet validation and decryption
- Account and transaction management
- Balance updates
- Idempotency enforcement
- Event publishing (outbox pattern)
- Mesh network simulation (demo)

### Components to Extract

**Controllers:**
- ApiController.java (all payment endpoints)
- DashboardController.java (UI)
- GlobalExceptionHandler.java

**Services:**
- BridgeIngestionService.java
- PaymentSettlementService.java
- SettlementService.java
- IdempotencyService.java
- OutboxService.java
- KafkaProducerService.java
- MeshSimulatorService.java
- DemoService.java
- VirtualDevice.java

**Crypto:**
- HybridCryptoService.java
- ServerKeyHolder.java

**Entities:**
- Account.java
- AccountRepository.java
- Transaction.java
- TransactionRepository.java
- MeshPacket.java
- PaymentInstruction.java
- Outbox.java
- EventProcessed.java

**Kafka:**
- Produces: payment-received, payment-validated, payment-settlement-requested, payment-settled, payment-failed
- Consumes: (optional - for saga responses)

**Configuration:**
- SecurityConfig.java (full security with JWT)
- KafkaConfig.java (producer configuration)
- RedisConfig.java (idempotency cache)
- ResilienceConfig.java (circuit breakers, retries)
- RateLimiterConfig.java
- TracingConfig.java
- ActuatorConfig.java
- OpenApiConfig.java

### Database Ownership
**Owns:**
- account table
- transaction table
- outbox table
- event_processed table

### Redis Usage
- Idempotency cache (SETNX for duplicate prevention)
- Optional: Session caching

### Port
8081

### Dependencies
- Spring Boot Web
- Spring Data JPA
- MySQL Driver
- Redis (Jedis)
- Spring Kafka
- Spring Security (full)
- Resilience4j
- OpenTelemetry
- Micrometer
- Eureka Client

### Communication
**Inbound:**
- HTTP from API Gateway
- Kafka events (optional - from Saga Service)

**Outbound:**
- Kafka events (payment lifecycle events)
- MySQL (persistence)
- Redis (idempotency)

---

## 4. SAGA SERVICE

### Purpose
Distributed transaction orchestration and saga state management.

### Responsibilities
- Saga orchestration
- Saga state management (persisted)
- Compensation logic execution
- Event-driven coordination with Payment Service
- Saga lifecycle management

### Components to Extract

**Services:**
- PaymentSagaOrchestrator.java
- SagaStateManager.java (needs persistence)

**Entities:**
- SagaState.java (needs to be persisted to database)
- SagaStateRepository.java (new)

**Kafka:**
- Consumes: payment-received, payment-validated, payment-settlement-requested, payment-settled, payment-failed
- Produces: saga-state-changed, compensation-events (optional)

**Configuration:**
- KafkaConfig.java (consumer configuration)
- ResilienceConfig.java (circuit breakers for inter-service calls)
- TracingConfig.java
- ActuatorConfig.java

### Database Ownership
**Owns:**
- saga_state table (new - needs to be created)

### Port
8082

### Dependencies
- Spring Boot Web
- Spring Data JPA
- MySQL Driver
- Spring Kafka
- Resilience4j
- OpenTelemetry
- Micrometer
- Eureka Client

### Communication
**Inbound:**
- Kafka events from Payment Service
- HTTP from API Gateway (saga management endpoints)

**Outbound:**
- Kafka events (compensation, state changes)
- MySQL (saga state persistence)
- REST (optional - direct calls to Payment Service if needed)

---

## INTER-SERVICE COMMUNICATION

### Synchronous (REST)
**Gateway → Payment Service:**
- All payment-related HTTP requests
- Demo/simulation endpoints

**Gateway → Saga Service:**
- Saga management endpoints (query saga state, manual compensation)

**Saga Service → Payment Service (Optional):**
- Direct REST calls for compensation if Kafka not suitable
- Fallback mechanism

### Asynchronous (Kafka)
**Payment Service → Saga Service:**
- payment-received
- payment-validated
- payment-settlement-requested
- payment-settled
- payment-failed

**Saga Service → Payment Service (Optional):**
- compensation requests
- saga completion notifications

---

## DATA OWNERSHIP

### Payment Service Database (meshpay_payment)
**Tables:**
- account
- transaction
- outbox
- event_processed

### Saga Service Database (meshpay_saga)
**Tables:**
- saga_state

### Shared Infrastructure
**MySQL:** Separate databases per service (same instance initially)
**Redis:** Shared by Payment Service (idempotency)
**Kafka:** Shared by all services

---

## MIGRATION STRATEGY

### Step 1: Create Saga State Persistence
- Add saga_state table to MySQL
- Persist SagaStateManager to database instead of in-memory

### Step 2: Decouple Saga from Payment Service
- Remove direct calls from PaymentSagaOrchestrator to PaymentSettlementService
- Use Kafka events for coordination
- Payment Service publishes events, Saga Service consumes

### Step 3: Create Independent Applications
- Create meshpay-payment-service module
- Create meshpay-saga-service module
- Move appropriate components to each service

### Step 4: Configure Service Discovery
- Register both services with Eureka
- Configure Gateway to discover services

### Step 5: Update Communication
- Replace direct method calls with Kafka events
- Add REST endpoints where synchronous response needed

### Step 6: Testing
- Test each service independently
- Test inter-service communication
- Test failure scenarios
- Test saga recovery

---

## DEMO/SIMULATION CODE DECISION

**Decision:** Keep demo/simulation code in Payment Service

**Rationale:**
- Demo endpoints are part of the payment flow
- Mesh simulation is specific to payment testing
- No need for separate demo service
- Can be disabled in production via configuration

---

## BACKWARD COMPATIBILITY

**API Compatibility:**
- Preserve all existing API endpoints
- Preserve request/response contracts
- Preserve authentication behavior

**Event Compatibility:**
- Preserve existing Kafka event schemas
- Preserve topic names
- Preserve consumer group IDs

**Database Compatibility:**
- Preserve existing table structures
- Add new tables (saga_state) without breaking existing tables

---

## DEPENDENCY GRAPH

```
Eureka Server
    ↓ (service discovery)
API Gateway
    ↓ (routing)
    ├─→ Payment Service
    │       ↓ (Kafka events)
    │   Saga Service
    │       ↓ (Kafka events - compensation)
    │   Payment Service
    │
    └─→ Saga Service (management endpoints)
```

---

## NEXT STEPS

1. **Persist Saga State** - Create saga_state table and repository
2. **Decouple Services** - Remove direct method calls, use Kafka events
3. **Create Service Modules** - Independent Spring Boot applications
4. **Configure Discovery** - Eureka registration and discovery
5. **Configure Gateway** - Routing and load balancing
6. **Test Integration** - End-to-end distributed testing
