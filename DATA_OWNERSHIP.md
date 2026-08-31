# MySQL Data Ownership Boundaries

## Overview
This document defines data ownership boundaries for the MySQL database after the PostgreSQL migration. It clarifies which services/contexts own which data entities and how data flows between them.

## Data Domains

### 1. Payment Domain
**Owner:** PaymentSettlementService, PaymentSagaOrchestrator

**Tables:**
- `account` - Account balances and user information
- `transaction` - Payment transaction records
- `outbox_event` - Event publishing outbox pattern

**Responsibilities:**
- Managing account balances
- Recording payment transactions
- Publishing payment events to Kafka via outbox pattern
- Ensuring transactional consistency between business data and events

**Data Access Pattern:**
- Read/Write: PaymentSettlementService
- Read-only: DashboardController (for display)
- Write: OutboxEventProcessor (for event publishing)

### 2. Event Domain
**Owner:** OutboxService, OutboxEventProcessor

**Tables:**
- `outbox_event` - Event publishing queue

**Responsibilities:**
- Storing events transactionally with business data
- Publishing events to Kafka
- Cleaning up published events

**Data Access Pattern:**
- Write: OutboxService (during business transactions)
- Read/Write: OutboxEventProcessor (scheduled task)
- No direct access from other services

### 3. Idempotency Domain
**Owner:** IdempotencyService, PaymentEventConsumer

**Tables:**
- `event_processed` - Idempotency tracking for Kafka events

**Responsibilities:**
- Tracking processed events to prevent duplicate processing
- Ensuring exactly-once semantics for event consumption

**Data Access Pattern:**
- Read/Write: IdempotencyService
- Read: PaymentEventConsumer (before processing)
- Write: PaymentEventConsumer (after processing)

### 4. Saga Domain
**Owner:** SagaStateManager, PaymentSagaOrchestrator

**Tables:**
- Currently in-memory (ConcurrentHashMap)
- **Future:** `saga_state` table for persistent saga tracking

**Responsibilities:**
- Tracking saga state across distributed transactions
- Managing compensation logic
- Providing saga recovery capabilities

**Data Access Pattern:**
- Read/Write: SagaStateManager
- Read: PaymentSagaOrchestrator (for state transitions)
- Write: PaymentSagaOrchestrator (for state updates)

**Note:** Currently implemented as in-memory for simplicity. Production should use a persistent table.

### 5. Mesh Simulator Domain (Development Only)
**Owner:** MeshSimulatorService, DemoService

**Tables:**
- None (in-memory simulation)

**Responsibilities:**
- Simulating offline mesh network behavior
- Testing idempotency and concurrent scenarios

**Data Access Pattern:**
- In-memory only, no database access

## Data Flow Boundaries

### Payment Settlement Flow

```
1. Bridge Ingestion (ApiController)
   ↓
2. PaymentSettlementService
   - Reads/Writes: account, transaction
   - Writes: outbox_event
   ↓
3. OutboxEventProcessor (Scheduled)
   - Reads: outbox_event
   - Publishes to Kafka
   - Deletes: outbox_event
   ↓
4. PaymentEventConsumer
   - Reads: event_processed (for idempotency)
   - Writes: event_processed
   - Calls PaymentSettlementService for business logic
```

### Saga Flow

```
1. PaymentSagaOrchestrator.startPaymentSaga()
   - Writes: saga_state (in-memory)
   - Calls PaymentSettlementService
   ↓
2. PaymentSettlementService
   - Reads/Writes: account, transaction
   - Writes: outbox_event
   ↓
3. Saga State Transitions
   - Updates: saga_state (in-memory)
   - Triggers compensation on failure
```

## Data Ownership Rules

### 1. Single Writer Principle
- Each table should have a single primary writer service
- Other services may read but should not write directly
- Exceptions: OutboxEventProcessor writes to outbox_event (scheduled task)

### 2. Transactional Boundaries
- Business data and outbox events must be written in the same transaction
- Idempotency checks should be outside the main transaction
- Saga state updates should be atomic with business operations

### 3. Event Sourcing
- Events are the source of truth for system state
- Database tables are projections of event stream
- Outbox pattern ensures event delivery guarantees

### 4. Data Isolation
- Each domain owns its data tables
- Cross-domain access should go through service APIs
- No direct table access across service boundaries

## MySQL-Specific Considerations

### 1. Storage Engine
- Use InnoDB for all tables (transactional support)
- Enable row-level locking for concurrent access

### 2. Character Set
- Use utf8mb4 for full Unicode support
- Collation: utf8mb4_unicode_ci

### 3. Indexing Strategy
- Primary keys on all tables
- Foreign key indexes for joins
- Composite indexes for common query patterns
- Index on aggregate_id for event tables

### 4. Connection Pooling
- HikariCP configuration in application.properties
- Pool size: 20 (max), 5 (min)
- Connection timeout: 30 seconds

## Migration Impact

### From PostgreSQL to MySQL

**Schema Changes:**
- Serial → AUTO_INCREMENT
- TEXT types may need size limits
- JSON type support (MySQL 5.7+)
- Boolean → TINYINT(1)

**Data Types Mapping:**
- PostgreSQL `serial` → MySQL `BIGINT AUTO_INCREMENT`
- PostgreSQL `text` → MySQL `VARCHAR(65535)` or `TEXT`
- PostgreSQL `jsonb` → MySQL `JSON`
- PostgreSQL `boolean` → MySQL `TINYINT(1)`
- PostgreSQL `timestamp` → MySQL `DATETIME` or `TIMESTAMP`

**Query Differences:**
- PostgreSQL `RETURNING` clause → MySQL `LAST_INSERT_ID()`
- PostgreSQL `ON CONFLICT` → MySQL `ON DUPLICATE KEY UPDATE`
- PostgreSQL `LIMIT/OFFSET` → MySQL `LIMIT/OFFSET` (same syntax)
- PostgreSQL string concatenation (`||`) → MySQL `CONCAT()`

## Data Retention Policies

### Outbox Events
- Retention: 7 days after successful publication
- Cleanup: Scheduled task runs hourly
- Archive: Consider archiving to cold storage

### Event Processed
- Retention: 30 days
- Cleanup: Scheduled task runs daily
- Purpose: Idempotency window

### Transactions
- Retention: Indefinite (audit trail)
- Archive: Consider archiving old transactions

### Saga State
- Retention: 30 days after completion
- Cleanup: Scheduled task runs daily
- Purpose: Saga recovery window

## Security Boundaries

### Database Access
- Application user: meshpay (read/write on app tables)
- Read-only user: meshpay_read (for reporting/analytics)
- Admin user: meshpay_admin (for schema changes)

### Row-Level Security
- Consider implementing row-level security for multi-tenant scenarios
- Filter by tenant_id in queries
- Use database triggers for enforcement

### Encryption at Rest
- Enable MySQL encryption at rest (InnoDB tablespace encryption)
- Encrypt sensitive columns (if any)
- Use MySQL Enterprise Transparent Data Encryption (TDE)

## Monitoring and Observability

### Database Metrics
- Connection pool usage
- Query performance (slow query log)
- Table sizes and growth rates
- Deadlock detection

### Data Quality Checks
- Foreign key integrity
- Orphaned record detection
- Data consistency checks
- Duplicate detection

## Recommendations

### 1. Implement Persistent Saga State
- Move from in-memory to database table
- Add saga_state table with proper indexes
- Implement saga recovery mechanism

### 2. Add Audit Logging
- Log all data modifications
- Track who changed what and when
- Use database triggers or application-level logging

### 3. Implement Data Archival
- Archive old transactions to cold storage
- Implement data retention policies
- Provide data export capabilities

### 4. Add Database Backups
- Regular automated backups
- Point-in-time recovery capability
- Backup validation and testing

### 5. Implement Read Replicas
- For reporting and analytics
- Reduce load on primary database
- Improve read performance

## Conclusion

This document defines clear data ownership boundaries for the MySQL database after migration. Following these boundaries ensures:
- Clear separation of concerns
- Data consistency and integrity
- Maintainable architecture
- Scalable design

The event-driven architecture with outbox pattern provides strong consistency guarantees while maintaining loose coupling between services.
