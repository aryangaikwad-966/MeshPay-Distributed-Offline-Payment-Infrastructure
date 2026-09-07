# MeshPay: Microservices Architecture for Distributed Payments

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Java: 17+](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot: 3.3](https://img.shields.io/badge/Spring%20Boot-3.3-green.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud: 2023.0](https://img.shields.io/badge/Spring%20Cloud-2023.0-blue.svg)](https://spring.io/projects/spring-cloud)

MeshPay is a production-ready microservices architecture for distributed payment processing, featuring event-driven communication, saga pattern for distributed transactions, and comprehensive observability.

---

## 🏗️ Architecture

### Microservices

MeshPay consists of the following microservices:

| Service | Port | Description |
|---------|------|-------------|
| **API Gateway** | 8080 | Single entry point, routing, authentication |
| **Payment Service** | 8081 | Payment processing, account management |
| **Saga Service** | 8082 | Distributed transaction orchestration |
| **Eureka Server** | 8761 | Service discovery and registration |

### Infrastructure

- **MySQL 8.0** - Primary database for each service
- **Redis 7** - Caching and distributed locking
- **Apache Kafka 3.x** - Event-driven messaging
- **Zookeeper** - Kafka coordination

### Architecture Diagram

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       ▼
┌──────────────────┐
│  API Gateway     │
│  (Port 8080)     │
└──────┬───────────┘
       │
       ├──────────────┬──────────────┐
       ▼              ▼              ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│   Payment    │ │    Saga      │ │   Eureka     │
│   Service    │ │   Service    │ │   Server     │
│  (Port 8081) │ │  (Port 8082) │ │  (Port 8761) │
└──────┬───────┘ └──────┬───────┘ └──────────────┘
       │                │
       │                │
       ▼                ▼
┌──────────────┐ ┌──────────────┐
│   MySQL      │ │   Kafka      │
│   (3307)     │ │   (9092)     │
└──────────────┘ └──────┬───────┘
                       │
                       ▼
                ┌──────────────┐
                │   Redis      │
                │   (6379)     │
                └──────────────┘
```

---

## 🚀 Getting Started

### Prerequisites

- Java 17 or higher
- Docker & Docker Compose
- Maven 3.8+

### Quick Start with Docker Compose

```bash
# Start all services and infrastructure
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

### Manual Setup

Start infrastructure services first, then run each microservice:

```bash
# Terminal 1 - Eureka Server
cd meshpay-eureka-server
mvn spring-boot:run

# Terminal 2 - Payment Service
cd meshpay-payment-service
mvn spring-boot:run

# Terminal 3 - Saga Service
cd meshpay-saga-service
mvn spring-boot:run

# Terminal 4 - API Gateway
cd meshpay-api-gateway
mvn spring-boot:run
```

---

## 📚 Services Documentation

### API Gateway (Port 8080)

**Health Check:**
```bash
curl http://localhost:8080/actuator/health
```

**Routes:**
- `/api/payment/**` → Payment Service
- `/api/saga/**` → Saga Service

### Payment Service (Port 8081)

**Health Check:**
```bash
curl http://localhost:8081/actuator/health
```

**API Endpoints:**
- `POST /api/payment/validate` - Validate payment
- `POST /api/payment/settlement` - Request settlement
- `POST /api/payment/complete` - Complete settlement
- `GET /api/payment/accounts` - List accounts
- `GET /api/payment/transactions` - List transactions

**Swagger UI:**
```
http://localhost:8081/swagger-ui/index.html
```

### Saga Service (Port 8082)

**Health Check:**
```bash
curl http://localhost:8082/actuator/health
```

**API Endpoints:**
- `POST /api/saga/start` - Start new saga
- `POST /api/saga/validate` - Validate payment in saga
- `POST /api/saga/settlement` - Request settlement in saga
- `POST /api/saga/complete` - Complete settlement in saga
- `GET /api/saga/status/{sagaId}` - Get saga status

**Swagger UI:**
```
http://localhost:8082/swagger-ui/index.html
```

### Eureka Server (Port 8761)

**Dashboard:**
```
http://localhost:8761
```

---

## 🔑 Key Patterns

### Transactional Outbox Pattern

Events are written to the outbox table in the same transaction as business data, ensuring exactly-once event delivery:

```java
@Transactional
public void processPayment(Payment payment) {
    // Business logic
    paymentRepository.save(payment);
    
    // Event in same transaction
    outboxService.saveEvent(paymentEvent, "payment-validated", payment.getPacketHash());
}
```

A scheduled processor publishes unprocessed events to Kafka every 5 seconds.

### Saga Pattern

Distributed transactions are orchestrated using the Saga pattern:

1. **Saga Start** - Initialize saga state
2. **Payment Validation** - Validate payment details
3. **Settlement Request** - Request settlement from bank
4. **Settlement Completion** - Finalize transaction
5. **Compensation** - Rollback on failure

Each step has a compensation action for failure recovery.

### Event-Driven Architecture

Services communicate via Kafka topics:

- `saga-validate-payment-command` - Commands from Saga to Payment
- `saga-request-settlement-command` - Settlement request commands
- `saga-complete-settlement-command` - Completion commands
- `payment-validated` - Validation completion events
- `payment-settlement-requested` - Settlement request events
- `payment-settled` - Final settlement events
- `payment-failed` - Failure events
- `saga-completed` - Saga completion events
- `saga-failed` - Saga failure events

---

## 🔐 Security

### Profile-Based Configuration

**Dev Profile (default):**
- Authentication disabled for testing
- All endpoints permitted

**Prod Profile:**
- OAuth2 JWT validation enabled
- Actuator, Swagger, and health endpoints public
- All other endpoints require authentication

### Configuration

Set the profile in `application.yml` or environment variable:

```yaml
spring:
  profiles:
    active: prod
```

Or:
```bash
export SPRING_PROFILES_ACTIVE=prod
```

---

## 📊 Observability

### Health Checks

Each service exposes health endpoints:

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

### Metrics (Prometheus)

```bash
curl http://localhost:8081/actuator/prometheus
curl http://localhost:8082/actuator/prometheus
```

### Custom Metrics

- Payment validation count
- Settlement success/failure rates
- Saga completion rates
- Active saga count
- Outbox processing metrics

---

## 🛠️ Development

### Project Structure

```
Meshpay/
├── meshpay-api-gateway/
│   ├── src/main/java/com/demo/meshpay/gateway/
│   └── pom.xml
├── meshpay-payment-service/
│   ├── src/main/java/com/demo/meshpay/payment/
│   │   ├── config/
│   │   ├── controller/
│   │   ├── model/
│   │   ├── repository/
│   │   ├── service/
│   │   ├── processor/
│   │   └── PaymentServiceApplication.java
│   └── pom.xml
├── meshpay-saga-service/
│   ├── src/main/java/com/demo/meshpay/saga/
│   │   ├── config/
│   │   ├── controller/
│   │   ├── consumer/
│   │   ├── events/
│   │   ├── model/
│   │   ├── repository/
│   │   ├── service/
│   │   ├── processor/
│   │   └── SagaServiceApplication.java
│   └── pom.xml
├── meshpay-eureka-server/
│   ├── src/main/java/com/demo/meshpay/eureka/
│   │   └── EurekaServerApplication.java
│   └── pom.xml
├── docker-compose.yml
├── test-microservices.sh
└── README.md
```

### Building

```bash
# Build all services
cd meshpay-payment-service && mvn clean package
cd ../meshpay-saga-service && mvn clean package
cd ../meshpay-api-gateway && mvn clean package
cd ../meshpay-eureka-server && mvn clean package
```

### Testing

```bash
# Run integration tests
cd meshpay-payment-service && mvn test
cd ../meshpay-saga-service && mvn test
```

### End-to-End Testing

```bash
# Run the test script
./test-microservices.sh
```

This script:
- Starts all services via Docker Compose
- Verifies service health
- Tests Eureka registration
- Tests API Gateway routing
- Verifies Kafka topics
- Tests basic API endpoints

---

## 🔧 Configuration

### Environment Variables

Key environment variables can be set in `docker-compose.yml`:

```yaml
environment:
  - SPRING_PROFILES_ACTIVE=prod
  - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/meshpay_payment
  - SPRING_REDIS_HOST=redis
  - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
```

### Database Configuration

Each service has its own database:

- **Payment Service**: `meshpay_payment` (port 3307)
- **Saga Service**: `meshpay_saga` (port 3308)

---

## 📝 Technical Stack

- **Java**: 17
- **Spring Boot**: 3.3.5
- **Spring Cloud**: 2023.0.3
- **Spring Cloud Gateway**: 4.1.x
- **Spring Cloud Netflix Eureka**: 4.1.x
- **MySQL**: 8.0
- **Redis**: 7
- **Kafka**: 3.x
- **Resilience4j**: 2.1.0
- **Lombok**: 1.18.30
- **Maven**: 3.8+

---

## 🐛 Troubleshooting

### Service won't start

1. Check if infrastructure is running:
```bash
docker-compose ps
```

2. Check service logs:
```bash
docker-compose logs payment-service
docker-compose logs saga-service
```

3. Verify database connectivity:
```bash
docker-compose exec mysql mysql -uroot -proot -e "SHOW DATABASES;"
```

### Kafka connection issues

1. Verify Kafka is running:
```bash
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list
```

2. Check topics are created:
```bash
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list
```

### Service discovery issues

1. Check Eureka dashboard:
```
http://localhost:8761
```

2. Verify services are registered:
```bash
curl http://localhost:8761/eureka/apps
```

---

## � License

This project is licensed under the MIT License.

---

## 👥 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

---

<div align="center">
  <h3>MeshPay Microservices Architecture</h3>
  <p>Production-ready distributed payment processing</p>
</div>
