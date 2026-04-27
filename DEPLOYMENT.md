# UPI Offline Mesh - Deployment Guide

## Prerequisites

- Docker & Docker Compose
- PostgreSQL 16+
- Redis 7+
- Java 17+
- Maven 3.8+

## Local Development Setup

### 1. Using Docker Compose (Recommended)

```bash
# Clone the repository
git clone <repo-url>
cd UPI_Without_Internet-main

# Create environment file
cp .env.example .env

# Edit .env with your settings
nano .env

# Start all services (app, PostgreSQL, Redis)
docker-compose up -d

# Check logs
docker-compose logs -f app

# Access the application
# API: http://localhost:8080/api
# Swagger UI: http://localhost:8080/swagger-ui.html
# Prometheus Metrics: http://localhost:8080/actuator/prometheus
# Health: http://localhost:8080/actuator/health
```

### 2. Local Machine Setup

```bash
# Install PostgreSQL
brew install postgresql  # macOS
# or apt-get install postgresql  # Ubuntu

# Install Redis
brew install redis  # macOS
# or apt-get install redis-server  # Ubuntu

# Start services
redis-server &
pg_ctl -D /usr/local/var/postgres start

# Build and run application
./mvnw clean package
java -jar target/upi-offline-mesh-*.jar
```

## Production Deployment

### AWS ECS with RDS + ElastiCache

```bash
# 1. Build Docker image
docker build -t 123456789.dkr.ecr.us-east-1.amazonaws.com/upi-mesh:latest .

# 2. Push to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 123456789.dkr.ecr.us-east-1.amazonaws.com
docker push 123456789.dkr.ecr.us-east-1.amazonaws.com/upi-mesh:latest

# 3. Create ECS Task Definition (see task-definition.json)

# 4. Create ECS Service
aws ecs create-service \
  --cluster production \
  --service-name upi-mesh \
  --task-definition upi-mesh:1 \
  --desired-count 3 \
  --load-balancers targetGroupArn=arn:aws:elasticloadbalancing:...,containerName=upi-mesh,containerPort=8080

# 5. Set up Auto Scaling
aws application-autoscaling register-scalable-target \
  --service-namespace ecs \
  --resource-id service/production/upi-mesh \
  --scalable-dimension ecs:service:DesiredCount \
  --min-capacity 3 \
  --max-capacity 10
```

### Kubernetes Deployment

```bash
# 1. Build and push image to your registry
docker build -t your-registry.com/upi-mesh:1.0.0 .
docker push your-registry.com/upi-mesh:1.0.0

# 2. Create namespace
kubectl create namespace upi-mesh

# 3. Create secrets
kubectl create secret -n upi-mesh generic app-secrets \
  --from-literal=db-password=xxx \
  --from-literal=redis-password=yyy

# 4. Deploy with Helm
helm install upi-mesh ./helm/upi-mesh \
  -n upi-mesh \
  -f helm/values-production.yaml

# 5. Check deployment
kubectl get pods -n upi-mesh
kubectl logs -n upi-mesh deployment/upi-mesh
```

## Database Setup

### PostgreSQL Initialization

```sql
-- Connect as superuser
psql -U postgres

-- Create database
CREATE DATABASE upi_mesh
  WITH OWNER upi_user
  ENCODING 'UTF8'
  LC_COLLATE 'en_US.UTF-8'
  LC_CTYPE 'en_US.UTF-8';

-- Create user
CREATE USER upi_user WITH PASSWORD 'secure_password_here';
GRANT CONNECT ON DATABASE upi_mesh TO upi_user;
GRANT USAGE ON SCHEMA public TO upi_user;
GRANT CREATE ON SCHEMA public TO upi_user;

-- Enable extensions
\c upi_mesh
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Tables will be auto-created by Hibernate (set spring.jpa.hibernate.ddl-auto=create or update)
```

### Database Encryption at Rest (Optional)

```sql
-- Enable transparent data encryption
-- This is database-specific and typically configured at the DB instance level
-- For AWS RDS: Enable "Encryption in storage" during RDS creation
```

## Security Checklist

- [ ] SSL/TLS enabled on all endpoints
- [ ] JWT authentication configured
- [ ] Database user has least privileges
- [ ] Redis password protected
- [ ] KMS keys configured for encryption
- [ ] Network policies restrict access
- [ ] CORS properly configured for your domain
- [ ] Secrets stored in vault (not in code or .env)
- [ ] Rate limiting enabled
- [ ] Audit logging enabled

## Monitoring Setup

### Prometheus

```yaml
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: upi-mesh
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ['localhost:8080']
```

### Grafana Dashboard

```bash
# Add Prometheus datasource
# Create dashboards to monitor:
# - JVM metrics (memory, threads, GC)
# - Application metrics (requests, errors, latency)
# - Business metrics (payments processed, settled, failed)
```

### Centralized Logging (ELK Stack)

```yaml
# logstash.yml
input {
  file {
    path => "/var/log/upi-mesh/upi-mesh.log"
    start_position => "beginning"
  }
}

filter {
  json {
    source => "message"
  }
}

output {
  elasticsearch {
    hosts => ["localhost:9200"]
    index => "upi-mesh-%{+YYYY.MM.dd}"
  }
}
```

## Health Checks

```bash
# Liveness probe (pod is running)
curl http://localhost:8080/actuator/health/liveness

# Readiness probe (pod can accept traffic)
curl http://localhost:8080/actuator/health/readiness

# Full health status
curl http://localhost:8080/actuator/health
```

## Scaling Considerations

1. **Horizontal Scaling**: 
   - Load balancer distributes traffic
   - Redis provides shared idempotency cache
   - Database connection pooling manages DB connections

2. **Performance Tuning**:
   - JVM: `-XX:+UseG1GC -XX:MaxRAMPercentage=75.0`
   - Database: Enable query caching, add indexes
   - Redis: Configure eviction policy, max memory

3. **Bottleneck Analysis**:
   - Monitor database query latency
   - Track Redis cache hit rate
   - Profile JVM heap usage

## Disaster Recovery

```bash
# Database backup
pg_dump -h localhost -U upi_user upi_mesh > backup-$(date +%Y%m%d).sql

# Database restore
psql -h localhost -U upi_user upi_mesh < backup-20240101.sql

# Redis backup
redis-cli BGSAVE
redis-cli --rdb /path/to/backup.rdb
```

## Troubleshooting

### Application won't start
```bash
# Check logs
docker-compose logs app

# Verify database connection
docker-compose exec app wget -O- http://localhost:8080/actuator/health
```

### High latency
```bash
# Check database performance
docker-compose exec postgres psql -U upimesh -d upi_mesh -c "SELECT * FROM pg_stat_statements ORDER BY mean_time DESC LIMIT 10;"

# Check Redis keys
docker-compose exec redis redis-cli INFO stats
```

### OOM errors
```bash
# Increase heap size
export JAVA_OPTS="-Xmx2g -Xms1g"
```

## References

- [Spring Boot Production Deployment](https://spring.io/guides/gs/spring-boot/)
- [PostgreSQL High Availability](https://www.postgresql.org/docs/current/high-availability.html)
- [Redis Replication](https://redis.io/topics/replication)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
