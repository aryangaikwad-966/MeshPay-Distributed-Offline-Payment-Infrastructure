# Monolith Cleanup Status

## Overview

The original monolithic application (`/src`) has been refactored into independent microservices. This document tracks the cleanup status and migration path.

## Extracted Components

The following components have been extracted from the monolith into separate microservices:

### Payment Service (`meshpay-payment-service`)
- **Port**: 8081
- **Database**: `meshpay_payment`
- **Extracted Components**:
  - Models: `Account`, `Transaction`, `Outbox`
  - Repositories: `AccountRepository`, `TransactionRepository`, `OutboxRepository`
  - Services: `PaymentSettlementService`, `OutboxService`
  - Events: `PaymentValidatedEvent`, `PaymentSettlementRequestedEvent`, `PaymentSettledEvent`, `PaymentFailedEvent`
  - Consumers: `SagaCommandConsumer`
  - Metrics: `PaymentMetrics`

### Saga Service (`meshpay-saga-service`)
- **Port**: 8082
- **Database**: `meshpay_saga`
- **Extracted Components**:
  - Models: `SagaState`
  - Repositories: `SagaStateRepository`
  - Services: `SagaStateManager`, `SagaCommandProducer`, `PaymentSagaOrchestrator`
  - Events: `ValidatePaymentCommand`, `RequestSettlementCommand`, `CompleteSettlementCommand`
  - Metrics: `SagaMetrics`

### Infrastructure Services
- **Eureka Server** (`meshpay-eureka-server`): Port 8761 - Service discovery
- **API Gateway** (`meshpay-api-gateway`): Port 8080 - Single entry point

## Monolith Status

### Current Configuration
- **Port**: 8083 (changed from 8080 to avoid conflicts)
- **Application Name**: `meshpay-monolith` (changed from `payment-service`)
- **Database**: `meshpay` (original database)
- **Status**: Legacy - can run alongside microservices for backward compatibility

### Remaining in Monolith
The monolith still contains:
- Mesh simulation logic (`MeshSimulatorService`, `VirtualDevice`)
- Bridge ingestion logic (`BridgeIngestionService`)
- Demo services (`DemoService`)
- Dashboard UI (`DashboardController`)
- API controllers (`ApiController`)
- Crypto utilities (`ServerKeyHolder`)
- All original configuration and dependencies

## Cleanup Recommendations

### Phase 1: Safe Migration (Current)
- ✅ Monolith runs on port 8083 alongside microservices
- ✅ Monolith registers with Eureka as `meshpay-monolith`
- ✅ Both systems can coexist during transition

### Phase 2: Gradual Migration
- [ ] Route traffic through API Gateway
- [ ] Migrate mesh simulation to separate service (if needed)
- [ ] Migrate dashboard to separate frontend service
- [ ] Update clients to use microservices endpoints

### Phase 3: Monolith Deprecation
- [ ] Mark monolith as deprecated
- [ ] Add deprecation notices in API responses
- [ ] Set sunset date for monolith shutdown
- [ ] Monitor microservices for stability

### Phase 4: Final Cleanup
- [ ] Archive monolith code
- [ ] Remove monolith from Docker Compose
- [ ] Clean up old database (`meshpay`)
- [ ] Update documentation

## Database Migration

### Current State
- **Monolith**: Uses `meshpay` database
- **Payment Service**: Uses `meshpay_payment` database
- **Saga Service**: Uses `meshpay_saga` database

### Migration Path
1. **Data Export**: Export existing data from `meshpay` database
2. **Schema Mapping**: Map monolith tables to microservice schemas
3. **Data Import**: Import data into respective microservice databases
4. **Validation**: Verify data integrity after migration
5. **Cutover**: Switch to microservice databases
6. **Cleanup**: Drop old `meshpay` database

## API Compatibility

### Monolith Endpoints (Port 8083)
- `GET /api/server-key` - Server public key
- `POST /api/demo/send` - Demo payment sending
- `GET /api/mesh/state` - Mesh simulation state
- `POST /api/mesh/gossip` - Trigger mesh gossip
- `POST /api/mesh/flush` - Flush bridge uploads
- `POST /api/mesh/reset` - Reset mesh simulation
- `POST /api/bridge/ingest` - Bridge ingestion endpoint
- `GET /api/accounts` - List accounts
- `GET /api/transactions` - List transactions
- `GET /` - Dashboard UI

### Microservices Endpoints
- **API Gateway (8080)**: Routes to Payment and Saga services
- **Payment Service (8081)**: Payment processing endpoints (to be added)
- **Saga Service (8082)**: Saga orchestration endpoints (to be added)

## Risk Assessment

### Low Risk
- Monolith and microservices can run simultaneously
- Separate databases prevent data conflicts
- Different ports prevent port conflicts
- Eureka distinguishes services by name

### Medium Risk
- Kafka topic sharing between monolith and microservices
- Potential duplicate event processing
- Need to ensure idempotency across systems

### Mitigation
- Use separate Kafka consumer groups
- Implement idempotency checks at database level
- Monitor for duplicate events
- Gradual traffic migration with monitoring

## Next Steps

1. **Testing**: Run both systems in parallel and verify functionality
2. **Monitoring**: Set up monitoring to compare monolith vs microservices
3. **Gradual Migration**: Route percentage of traffic to microservices
4. **Data Migration**: Plan and execute database migration
5. **Decommissioning**: Plan monolith shutdown timeline

## Rollback Plan

If microservices encounter issues:
1. Stop microservices
2. Route all traffic back to monolith (port 8083)
3. Investigate and fix microservice issues
4. Resume gradual migration

## Notes

- The monolith codebase remains intact for reference and rollback
- No code has been deleted from the monolith
- All original functionality remains available
- Microservices are additive, not replacements at this stage
