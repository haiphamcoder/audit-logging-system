# Hướng dẫn triển khai Audit Logging System

## Tổng quan

Hướng dẫn này mô tả cách triển khai hệ thống Audit Logging System trong các môi trường khác nhau: Development, Staging và Production.

## Yêu cầu hệ thống

### Yêu cầu tối thiểu cho Production
- **CPU**: 4 cores
- **RAM**: 8GB
- **Storage**: 100GB SSD
- **Network**: 1Gbps
- **OS**: Ubuntu 20.04 LTS hoặc CentOS 8

### Yêu cầu cho Development/Staging
- **CPU**: 2 cores
- **RAM**: 4GB
- **Storage**: 50GB
- **Network**: 100Mbps

## Môi trường triển khai

### 1. Development Environment
- Mục đích: Phát triển và testing
- Cấu hình: Minimal resources
- Database: H2 hoặc PostgreSQL local
- Monitoring: Basic logging

### 2. Staging Environment
- Mục đích: Testing trước production
- Cấu hình: Tương tự production
- Database: PostgreSQL
- Monitoring: Full monitoring

### 3. Production Environment
- Mục đích: Môi trường live
- Cấu hình: High availability
- Database: PostgreSQL cluster
- Monitoring: Comprehensive monitoring

## Triển khai với Docker Compose

### 1. Cấu hình cơ bản

Tạo file `docker-compose.yml`:

```yaml
version: '3.8'

services:
  # Database
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: audit_logs
      POSTGRES_USER: audit_user
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    networks:
      - audit-network

  # Redis (optional)
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    networks:
      - audit-network

  # Audit Log Agent
  audit-log-agent:
    build:
      context: ./audit-log-agent
      dockerfile: Dockerfile
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE}
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: audit_logs
      DB_USER: audit_user
      DB_PASSWORD: ${DB_PASSWORD}
    ports:
      - "8080:8080"
    depends_on:
      - postgres
    networks:
      - audit-network
    restart: unless-stopped

  # Audit Log Worker
  audit-log-worker:
    build:
      context: ./audit-log-worker
      dockerfile: Dockerfile
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE}
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: audit_logs
      DB_USER: audit_user
      DB_PASSWORD: ${DB_PASSWORD}
    depends_on:
      - postgres
    networks:
      - audit-network
    restart: unless-stopped

  # Audit Log Query
  audit-log-query:
    build:
      context: ./audit-log-query
      dockerfile: Dockerfile
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE}
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: audit_logs
      DB_USER: audit_user
      DB_PASSWORD: ${DB_PASSWORD}
    ports:
      - "8082:8080"
    depends_on:
      - postgres
    networks:
      - audit-network
    restart: unless-stopped

  # Nginx (Load Balancer)
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf
      - ./nginx/ssl:/etc/nginx/ssl
    depends_on:
      - audit-log-agent
      - audit-log-query
    networks:
      - audit-network
    restart: unless-stopped

networks:
  audit-network:
    driver: bridge

volumes:
  postgres_data:
```

### 2. File môi trường

Tạo file `.env`:

```bash
# Database Configuration
DB_PASSWORD=your_secure_password_here
DB_HOST=postgres
DB_PORT=5432
DB_NAME=audit_logs
DB_USER=audit_user

# Spring Profiles
SPRING_PROFILES_ACTIVE=production

# Application Configuration
AUDIT_LOG_AGENT_PORT=8080
AUDIT_LOG_WORKER_PORT=8081
AUDIT_LOG_QUERY_PORT=8082

# Security
JWT_SECRET=your_jwt_secret_here
API_KEY=your_api_key_here

# Monitoring
PROMETHEUS_ENABLED=true
GRAFANA_ENABLED=true
```

### 3. Triển khai

```bash
# Build và start services
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f audit-log-agent
docker-compose logs -f audit-log-worker
docker-compose logs -f audit-log-query

# Stop services
docker-compose down

# Stop và remove volumes
docker-compose down -v
```

## Triển khai với Kubernetes

### 1. Namespace

```yaml
# namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: audit-logging-system
```

### 2. ConfigMap

```yaml
# configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: audit-logging-config
  namespace: audit-logging-system
data:
  application.yml: |
    spring:
      profiles:
        active: production
      datasource:
        url: jdbc:postgresql://postgres:5432/audit_logs
        username: audit_user
        password: ${DB_PASSWORD}
    logging:
      level:
        com.haiphamcoder.auditlog: INFO
```

### 3. Secret

```yaml
# secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: audit-logging-secret
  namespace: audit-logging-system
type: Opaque
data:
  db-password: <base64-encoded-password>
  jwt-secret: <base64-encoded-jwt-secret>
  api-key: <base64-encoded-api-key>
```

### 4. Deployment

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: audit-log-agent
  namespace: audit-logging-system
spec:
  replicas: 3
  selector:
    matchLabels:
      app: audit-log-agent
  template:
    metadata:
      labels:
        app: audit-log-agent
    spec:
      containers:
      - name: audit-log-agent
        image: audit-log-agent:latest
        ports:
        - containerPort: 8080
        env:
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: audit-logging-secret
              key: db-password
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: audit-logging-secret
              key: jwt-secret
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 5
          periodSeconds: 5
```

### 5. Service

```yaml
# service.yaml
apiVersion: v1
kind: Service
metadata:
  name: audit-log-agent-service
  namespace: audit-logging-system
spec:
  selector:
    app: audit-log-agent
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: ClusterIP
```

### 6. Ingress

```yaml
# ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: audit-logging-ingress
  namespace: audit-logging-system
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    cert-manager.io/cluster-issuer: letsencrypt-prod
spec:
  tls:
  - hosts:
    - audit.example.com
    secretName: audit-logging-tls
  rules:
  - host: audit.example.com
    http:
      paths:
      - path: /api/agent
        pathType: Prefix
        backend:
          service:
            name: audit-log-agent-service
            port:
              number: 80
      - path: /api/query
        pathType: Prefix
        backend:
          service:
            name: audit-log-query-service
            port:
              number: 80
```

## Cấu hình Database

### 1. PostgreSQL Setup

```sql
-- Create database
CREATE DATABASE audit_logs;

-- Create user
CREATE USER audit_user WITH PASSWORD 'your_secure_password';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE audit_logs TO audit_user;

-- Create tables (if not using JPA auto-creation)
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    timestamp TIMESTAMP NOT NULL,
    user_id VARCHAR(100) NOT NULL,
    username VARCHAR(100),
    action VARCHAR(50) NOT NULL,
    resource VARCHAR(200),
    resource_id VARCHAR(100),
    request_path VARCHAR(500),
    status VARCHAR(20),
    ip_address VARCHAR(45),
    user_agent TEXT,
    request_id VARCHAR(100),
    additional_data JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp);
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_resource ON audit_logs(resource);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
```

### 2. Database Migration

Sử dụng Flyway cho database migration:

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

## Cấu hình Monitoring

### 1. Prometheus Configuration

```yaml
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'audit-log-agent'
    static_configs:
      - targets: ['audit-log-agent:8080']
    metrics_path: '/actuator/prometheus'

  - job_name: 'audit-log-worker'
    static_configs:
      - targets: ['audit-log-worker:8080']
    metrics_path: '/actuator/prometheus'

  - job_name: 'audit-log-query'
    static_configs:
      - targets: ['audit-log-query:8080']
    metrics_path: '/actuator/prometheus'
```

### 2. Grafana Dashboard

Tạo dashboard cho monitoring:
- Request rate
- Error rate
- Response time
- Database connections
- Memory usage
- CPU usage

## SSL/TLS Configuration

### 1. Nginx SSL Configuration

```nginx
# nginx.conf
server {
    listen 443 ssl http2;
    server_name audit.example.com;

    ssl_certificate /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512;

    location /api/agent {
        proxy_pass http://audit-log-agent:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /api/query {
        proxy_pass http://audit-log-query:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## Backup và Recovery

### 1. Database Backup

```bash
#!/bin/bash
# backup.sh
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backup"
DB_NAME="audit_logs"
DB_USER="audit_user"

# Create backup
docker exec postgres pg_dump -U $DB_USER $DB_NAME > $BACKUP_DIR/audit_logs_$DATE.sql

# Compress backup
gzip $BACKUP_DIR/audit_logs_$DATE.sql

# Keep only last 7 days
find $BACKUP_DIR -name "audit_logs_*.sql.gz" -mtime +7 -delete
```

### 2. Application Backup

```bash
#!/bin/bash
# app-backup.sh
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backup/app"

# Backup configuration files
tar -czf $BACKUP_DIR/config_$DATE.tar.gz /etc/audit-logging/

# Backup logs
tar -czf $BACKUP_DIR/logs_$DATE.tar.gz /var/log/audit-logging/
```

## Troubleshooting

### 1. Common Issues

**Service không start:**
```bash
# Check logs
docker-compose logs service-name

# Check resource usage
docker stats

# Check network connectivity
docker network ls
docker network inspect audit-logging-system_audit-network
```

**Database connection issues:**
```bash
# Test database connection
docker exec -it postgres psql -U audit_user -d audit_logs

# Check database logs
docker-compose logs postgres
```

**Memory issues:**
```bash
# Check memory usage
docker stats

# Increase memory limits in docker-compose.yml
```

### 2. Health Checks

```bash
# Check service health
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health

# Check database health
docker exec postgres pg_isready -U audit_user
```

## Rollback Strategy

### 1. Docker Compose Rollback

```bash
# Rollback to previous version
docker-compose down
docker tag audit-log-agent:previous audit-log-agent:latest
docker-compose up -d
```

### 2. Kubernetes Rollback

```bash
# Rollback deployment
kubectl rollout undo deployment/audit-log-agent -n audit-logging-system

# Check rollback status
kubectl rollout status deployment/audit-log-agent -n audit-logging-system
```

## Performance Tuning

### 1. JVM Tuning

```bash
# JVM options for production
JAVA_OPTS="-Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

### 2. Database Tuning

```sql
-- PostgreSQL tuning
ALTER SYSTEM SET shared_buffers = '256MB';
ALTER SYSTEM SET effective_cache_size = '1GB';
ALTER SYSTEM SET maintenance_work_mem = '64MB';
ALTER SYSTEM SET checkpoint_completion_target = 0.9;
ALTER SYSTEM SET wal_buffers = '16MB';
ALTER SYSTEM SET default_statistics_target = 100;
```

## Security Checklist

- [ ] SSL/TLS enabled
- [ ] Database password is secure
- [ ] API keys are rotated regularly
- [ ] Firewall rules configured
- [ ] Regular security updates
- [ ] Access logs enabled
- [ ] Backup encryption
- [ ] Network segmentation 