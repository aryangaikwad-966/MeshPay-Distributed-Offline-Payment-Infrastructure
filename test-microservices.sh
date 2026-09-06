#!/bin/bash

# End-to-End Integration Test Script for MeshPay Microservices
# This script tests the complete microservices architecture

set -e

echo "=========================================="
echo "MeshPay Microservices E2E Test"
echo "=========================================="

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print test results
print_result() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓ $2${NC}"
    else
        echo -e "${RED}✗ $2${NC}"
        exit 1
    fi
}

# Function to wait for service to be healthy
wait_for_service() {
    local url=$1
    local service_name=$2
    local max_attempts=30
    local attempt=1

    echo -e "${YELLOW}Waiting for $service_name to be healthy...${NC}"
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s -f "$url" > /dev/null 2>&1; then
            print_result 0 "$service_name is healthy"
            return 0
        fi
        echo "Attempt $attempt/$max_attempts..."
        sleep 2
        attempt=$((attempt + 1))
    done
    
    print_result 1 "$service_name failed to become healthy"
}

echo ""
echo "Step 1: Starting Docker Compose services..."
docker-compose up -d

echo ""
echo "Step 2: Waiting for services to be healthy..."
wait_for_service "http://localhost:8761/actuator/health" "Eureka Server"
wait_for_service "http://localhost:8080/actuator/health" "API Gateway"
wait_for_service "http://localhost:8081/actuator/health" "Payment Service"
wait_for_service "http://localhost:8082/actuator/health" "Saga Service"

echo ""
echo "Step 3: Testing Eureka Server registration..."
EUREKA_RESPONSE=$(curl -s http://localhost:8761/eureka/apps)
if echo "$EUREKA_RESPONSE" | grep -q "payment-service"; then
    print_result 0 "Payment Service registered with Eureka"
else
    print_result 1 "Payment Service not registered with Eureka"
fi

if echo "$EUREKA_RESPONSE" | grep -q "saga-service"; then
    print_result 0 "Saga Service registered with Eureka"
else
    print_result 1 "Saga Service not registered with Eureka"
fi

echo ""
echo "Step 4: Testing Payment Service endpoints..."
PAYMENT_HEALTH=$(curl -s http://localhost:8081/api/payment/health)
if echo "$PAYMENT_HEALTH" | grep -q "UP"; then
    print_result 0 "Payment Service health check"
else
    print_result 1 "Payment Service health check failed"
fi

echo ""
echo "Step 5: Testing Saga Service endpoints..."
SAGA_HEALTH=$(curl -s http://localhost:8082/api/saga/health)
if echo "$SAGA_HEALTH" | grep -q "UP"; then
    print_result 0 "Saga Service health check"
else
    print_result 1 "Saga Service health check failed"
fi

echo ""
echo "Step 6: Testing API Gateway routing..."
GATEWAY_HEALTH=$(curl -s http://localhost:8080/actuator/health)
if echo "$GATEWAY_HEALTH" | grep -q "UP"; then
    print_result 0 "API Gateway health check"
else
    print_result 1 "API Gateway health check failed"
fi

echo ""
echo "Step 7: Testing Payment Service via API Gateway..."
PAYMENT_VIA_GATEWAY=$(curl -s http://localhost:8080/api/payment/health)
if echo "$PAYMENT_VIA_GATEWAY" | grep -q "payment-service"; then
    print_result 0 "Payment Service accessible via Gateway"
else
    print_result 1 "Payment Service not accessible via Gateway"
fi

echo ""
echo "Step 8: Testing Saga Service via API Gateway..."
SAGA_VIA_GATEWAY=$(curl -s http://localhost:8080/api/saga/health)
if echo "$SAGA_VIA_GATEWAY" | grep -q "saga-service"; then
    print_result 0 "Saga Service accessible via Gateway"
else
    print_result 1 "Saga Service not accessible via Gateway"
fi

echo ""
echo "Step 9: Testing Kafka connectivity..."
# Check if Kafka topics exist (requires kafka-topics command)
if command -v kafka-topics &> /dev/null; then
    kafka-topics --bootstrap-server localhost:9092 --list | grep -q "saga-validate-payment-command"
    print_result $? "Kafka topic 'saga-validate-payment-command' exists"
    
    kafka-topics --bootstrap-server localhost:9092 --list | grep -q "payment-validated"
    print_result $? "Kafka topic 'payment-validated' exists"
else
    echo -e "${YELLOW}Kafka CLI not found, skipping topic verification${NC}"
fi

echo ""
echo "Step 10: Testing Payment Service API..."
# Test validate payment endpoint
VALIDATE_RESPONSE=$(curl -s -X POST http://localhost:8081/api/payment/validate \
  -H "Content-Type: application/json" \
  -d '{"packetId":"test-123","packetHash":"hash-abc","senderVpa":"alice@demo","receiverVpa":"bob@demo","amount":100.00,"nonce":"nonce-xyz","signedAt":1234567890}')
if echo "$VALIDATE_RESPONSE" | grep -q "validation initiated"; then
    print_result 0 "Payment validation API"
else
    print_result 1 "Payment validation API failed"
fi

echo ""
echo "Step 11: Testing Saga Service API..."
# Test start saga endpoint
SAGA_RESPONSE=$(curl -s -X POST http://localhost:8082/api/saga/start \
  -H "Content-Type: application/json" \
  -d '{"packetId":"test-456","packetHash":"hash-def","bridgeNodeId":"bridge-1","ciphertext":"encrypted"}')
if echo "$SAGA_RESPONSE" | grep -q "STARTED"; then
    print_result 0 "Saga start API"
else
    print_result 1 "Saga start API failed"
fi

echo ""
echo "Step 12: Checking metrics endpoints..."
PAYMENT_METRICS=$(curl -s http://localhost:8081/actuator/prometheus)
if echo "$PAYMENT_METRICS" | grep -q "jvm"; then
    print_result 0 "Payment Service metrics endpoint"
else
    print_result 1 "Payment Service metrics endpoint failed"
fi

SAGA_METRICS=$(curl -s http://localhost:8082/actuator/prometheus)
if echo "$SAGA_METRICS" | grep -q "jvm"; then
    print_result 0 "Saga Service metrics endpoint"
else
    print_result 1 "Saga Service metrics endpoint failed"
fi

echo ""
echo "=========================================="
echo -e "${GREEN}All tests passed successfully!${NC}"
echo "=========================================="

echo ""
echo "Step 13: Cleaning up..."
docker-compose down

echo ""
echo "Test suite completed successfully!"
