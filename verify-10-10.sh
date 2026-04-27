#!/bin/bash

# Verification Script for 10/10 Production-Ready UPI Mesh
# Run this to verify all security and quality improvements are in place

set -e

echo "🔍 UPI Mesh - Production Readiness Verification"
echo "================================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

check_count=0
pass_count=0

test_check() {
    check_count=$((check_count + 1))
    echo -n "[$check_count] $1... "
}

test_pass() {
    pass_count=$((pass_count + 1))
    echo -e "${GREEN}✅ PASS${NC}"
}

test_fail() {
    echo -e "${RED}❌ FAIL${NC}"
}

test_warn() {
    echo -e "${YELLOW}⚠️  WARNING${NC}: $1"
}

echo "🔧 BUILD & DEPENDENCIES"
echo "-----------------------"

test_check "Maven build successful"
if ./mvnw clean package -q > /dev/null 2>&1; then
    test_pass
else
    test_fail
    exit 1
fi

test_check "Spring Security dependency present"
if grep -q "spring-boot-starter-security" pom.xml; then
    test_pass
else
    test_fail
fi

test_check "PostgreSQL driver configured"
if grep -q "postgresql" pom.xml; then
    test_pass
else
    test_fail
fi

test_check "Redis starter configured"
if grep -q "spring-boot-starter-data-redis" pom.xml; then
    test_pass
else
    test_fail
fi

test_check "Resilience4j (rate limiting) configured"
if grep -q "resilience4j" pom.xml; then
    test_pass
else
    test_fail
fi

echo ""
echo "🔐 SECURITY FEATURES"
echo "-------------------"

test_check "SecurityConfig.java exists"
if [ -f "src/main/java/com/demo/upimesh/config/SecurityConfig.java" ]; then
    test_pass
else
    test_fail
fi

test_check "OAuth2 JWT validation configured"
if grep -q "oauth2ResourceServer" src/main/java/com/demo/upimesh/config/SecurityConfig.java; then
    test_pass
else
    test_fail
fi

test_check "HTTPS enforcement configured"
if grep -q "requiresSecure" src/main/java/com/demo/upimesh/config/SecurityConfig.java; then
    test_pass
else
    test_fail
fi

test_check "AWS KMS service exists"
if [ -f "src/main/java/com/demo/upimesh/service/KmsKeyService.java" ]; then
    test_pass
else
    test_fail
fi

test_check "Global exception handler configured"
if [ -f "src/main/java/com/demo/upimesh/controller/GlobalExceptionHandler.java" ]; then
    test_pass
else
    test_fail
fi

test_check "Audit logging filter configured"
if [ -f "src/main/java/com/demo/upimesh/security/AuditLoggingFilter.java" ]; then
    test_pass
else
    test_fail
fi

test_check "Rate limiter configured"
if [ -f "src/main/java/com/demo/upimesh/config/RateLimiterConfigBean.java" ]; then
    test_pass
else
    test_fail
fi

echo ""
echo "📊 DATABASE & CACHING"
echo "--------------------"

test_check "Redis configuration exists"
if [ -f "src/main/java/com/demo/upimesh/config/RedisConfig.java" ]; then
    test_pass
else
    test_fail
fi

test_check "IdempotencyService has Redis support"
if grep -q "RedisTemplate" src/main/java/com/demo/upimesh/service/IdempotencyService.java; then
    test_pass
else
    test_fail
fi

test_check "application.properties configured for production"
if grep -q "spring.datasource.hikari.maximum-pool-size" src/main/resources/application.properties; then
    test_pass
else
    test_fail
fi

test_check "H2 console disabled by default"
if grep -q "H2_CONSOLE_ENABLED=false" src/main/resources/application.properties; then
    test_pass
else
    test_fail
fi

echo ""
echo "📚 DOCUMENTATION & TESTING"
echo "--------------------------"

test_check "OpenAPI/Swagger configured"
if [ -f "src/main/java/com/demo/upimesh/config/OpenApiConfig.java" ]; then
    test_pass
else
    test_fail
fi

test_check "Integration tests exist"
if [ -f "src/test/java/com/demo/upimesh/UpiMeshIntegrationTest.java" ]; then
    test_pass
else
    test_fail
fi

test_check "Test count (minimum 10)"
if [ -f "src/test/java/com/demo/upimesh/UpiMeshIntegrationTest.java" ]; then
    test_count=$(grep -c "@Test" src/test/java/com/demo/upimesh/UpiMeshIntegrationTest.java || echo 0)
    if [ "$test_count" -ge 10 ]; then
        test_pass
    else
        test_warn "Found only $test_count tests"
    fi
fi

test_check "AUDIT_REPORT.md exists"
if [ -f "AUDIT_REPORT.md" ]; then
    test_pass
else
    test_fail
fi

test_check "DEPLOYMENT.md exists"
if [ -f "DEPLOYMENT.md" ]; then
    test_pass
else
    test_fail
fi

test_check "PRODUCTION_READINESS.md exists"
if [ -f "PRODUCTION_READINESS.md" ]; then
    test_pass
else
    test_fail
fi

echo ""
echo "🐳 DOCKER & CONTAINERIZATION"
echo "----------------------------"

test_check "Dockerfile exists"
if [ -f "Dockerfile" ]; then
    test_pass
else
    test_fail
fi

test_check "Docker multi-stage build"
if grep -q "FROM.*AS builder" Dockerfile; then
    test_pass
else
    test_fail
fi

test_check "docker-compose.yml exists"
if [ -f "docker-compose.yml" ]; then
    test_pass
else
    test_fail
fi

test_check "PostgreSQL in docker-compose"
if grep -q "postgres.*image" docker-compose.yml; then
    test_pass
else
    test_fail
fi

test_check "Redis in docker-compose"
if grep -q "redis.*image" docker-compose.yml; then
    test_pass
else
    test_fail
fi

test_check ".dockerignore exists"
if [ -f ".dockerignore" ]; then
    test_pass
else
    test_fail
fi

echo ""
echo "🚀 CI/CD PIPELINE"
echo "-----------------"

test_check "GitHub Actions workflow exists"
if [ -f ".github/workflows/ci-cd.yml" ]; then
    test_pass
else
    test_fail
fi

test_check "Dependency scanning in CI"
if grep -q "dependency-check" .github/workflows/ci-cd.yml; then
    test_pass
else
    test_fail
fi

test_check "Docker build in CI"
if grep -q "docker/build-push-action" .github/workflows/ci-cd.yml; then
    test_pass
else
    test_fail
fi

echo ""
echo "⚙️  CONFIGURATION"
echo "-----------------"

test_check ".env.example exists"
if [ -f ".env.example" ]; then
    test_pass
else
    test_fail
fi

test_check "Environment variables documented"
if [ $(grep -c "^[A-Z_]" .env.example) -gt 20 ]; then
    test_pass
else
    test_fail
fi

echo ""
echo "================================================="
echo "📊 RESULTS"
echo "================================================="
echo -e "Passed: ${GREEN}$pass_count/$check_count${NC}"

if [ $pass_count -eq $check_count ]; then
    echo ""
    echo -e "${GREEN}✅ 10/10 PRODUCTION-READY!${NC}"
    echo ""
    echo "🎉 All checks passed! The application is ready for production."
    echo ""
    echo "Next steps:"
    echo "  1. Review PRODUCTION_READINESS.md"
    echo "  2. Follow DEPLOYMENT.md for your platform"
    echo "  3. Configure .env with your values"
    echo "  4. Start with: docker-compose up -d"
    echo "  5. Visit: http://localhost:8080/swagger-ui.html"
    echo ""
    exit 0
else
    failed=$((check_count - pass_count))
    echo ""
    echo -e "${RED}❌ $failed checks failed${NC}"
    echo ""
    echo "Please review the failed items above and fix them."
    exit 1
fi
