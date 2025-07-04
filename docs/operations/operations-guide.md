# Hướng dẫn vận hành Audit Logging System

## Tổng quan

Hướng dẫn vận hành hệ thống Audit Logging System, bao gồm monitoring, maintenance, troubleshooting và các quy trình vận hành hàng ngày.

## Monitoring và Alerting

### 1. Health Checks

#### Application Health
```bash
# Check agent service health
curl -f http://localhost:8080/actuator/health

# Check worker service health
curl -f http://localhost:8081/actuator/health

# Check query service health
curl -f http://localhost:8082/actuator/health
```

#### Database Health
```bash
# Check PostgreSQL connection
docker exec postgres pg_isready -U audit_user

# Check database size
docker exec postgres psql -U audit_user -d audit_logs -c "SELECT pg_size_pretty(pg_database_size('audit_logs'));"
```

### 2. Key Metrics

#### Application Metrics
- **Request Rate**: Số lượng requests per second
- **Response Time**: Average, P95, P99 response times
- **Error Rate**: Percentage of failed requests
- **Memory Usage**: JVM heap usage
- **CPU Usage**: Application CPU consumption

#### Database Metrics
- **Connection Pool**: Active/idle connections
- **Query Performance**: Slow queries, query count
- **Storage**: Database size, table sizes
- **Index Usage**: Index hit ratio

#### System Metrics
- **Disk Usage**: Available disk space
- **Network**: Bandwidth usage
- **Load Average**: System load

### 3. Alerting Rules

#### Critical Alerts
```yaml
# Prometheus Alert Rules
groups:
  - name: audit-logging-critical
    rules:
      - alert: ServiceDown
        expr: up{job=~"audit-log-.*"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Service {{ $labels.instance }} is down"
          
      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.1
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          
      - alert: DatabaseConnectionFailed
        expr: audit_db_connection_status == 0
        for: 30s
        labels:
          severity: critical
        annotations:
          summary: "Database connection failed"
```

#### Warning Alerts
```yaml
      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes / jvm_memory_max_bytes > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High memory usage detected"
          
      - alert: SlowResponseTime
        expr: histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m])) > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Slow response time detected"
```

## Log Management

### 1. Log Configuration

#### Application Logs
```yaml
# application.yml
logging:
  level:
    com.haiphamcoder.auditlog: INFO
    org.springframework.web: INFO
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/audit-logging.log
    max-size: 100MB
    max-history: 30
```

#### Docker Logs
```bash
# View service logs
docker-compose logs -f audit-log-agent
docker-compose logs -f audit-log-worker
docker-compose logs -f audit-log-query

# View logs with timestamps
docker-compose logs -f --timestamps audit-log-agent

# View logs for specific time period
docker-compose logs --since="2024-01-15T10:00:00" --until="2024-01-15T11:00:00" audit-log-agent
```

### 2. Log Rotation

#### Application Log Rotation
```bash
#!/bin/bash
# log-rotation.sh
LOG_DIR="/var/log/audit-logging"
DATE=$(date +%Y%m%d)

# Compress old logs
find $LOG_DIR -name "*.log.*" -mtime +7 -exec gzip {} \;

# Remove logs older than 30 days
find $LOG_DIR -name "*.log.*.gz" -mtime +30 -delete

# Archive logs older than 7 days
find $LOG_DIR -name "*.log" -mtime +7 -exec mv {} {}.$DATE \;
```

#### Docker Log Rotation
```yaml
# docker-compose.yml
services:
  audit-log-agent:
    logging:
      driver: "json-file"
      options:
        max-size: "100m"
        max-file: "3"
```

## Backup và Recovery

### 1. Database Backup

#### Automated Backup Script
```bash
#!/bin/bash
# backup-database.sh
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backup/database"
DB_NAME="audit_logs"
DB_USER="audit_user"
RETENTION_DAYS=7

# Create backup directory
mkdir -p $BACKUP_DIR

# Create backup
docker exec postgres pg_dump -U $DB_USER $DB_NAME > $BACKUP_DIR/audit_logs_$DATE.sql

# Compress backup
gzip $BACKUP_DIR/audit_logs_$DATE.sql

# Remove old backups
find $BACKUP_DIR -name "audit_logs_*.sql.gz" -mtime +$RETENTION_DAYS -delete

# Verify backup
if [ -f "$BACKUP_DIR/audit_logs_$DATE.sql.gz" ]; then
    echo "Backup completed successfully: audit_logs_$DATE.sql.gz"
else
    echo "Backup failed!"
    exit 1
fi
```

#### Backup Verification
```bash
#!/bin/bash
# verify-backup.sh
BACKUP_FILE=$1
DB_NAME="audit_logs_test"

# Create test database
docker exec postgres createdb -U audit_user $DB_NAME

# Restore backup to test database
gunzip -c $BACKUP_FILE | docker exec -i postgres psql -U audit_user -d $DB_NAME

# Verify data
RECORD_COUNT=$(docker exec postgres psql -U audit_user -d $DB_NAME -t -c "SELECT COUNT(*) FROM audit_logs;")
echo "Backup contains $RECORD_COUNT records"

# Clean up test database
docker exec postgres dropdb -U audit_user $DB_NAME
```

### 2. Application Backup

#### Configuration Backup
```bash
#!/bin/bash
# backup-config.sh
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backup/config"

# Backup configuration files
tar -czf $BACKUP_DIR/config_$DATE.tar.gz \
    /etc/audit-logging/ \
    /opt/audit-logging/config/

# Backup environment variables
env | grep -E "(AUDIT|DB_|SPRING_)" > $BACKUP_DIR/env_$DATE.txt
```

### 3. Disaster Recovery

#### Recovery Procedures
```bash
#!/bin/bash
# disaster-recovery.sh
BACKUP_FILE=$1
DB_NAME="audit_logs"

# Stop services
docker-compose down

# Restore database
gunzip -c $BACKUP_FILE | docker exec -i postgres psql -U audit_user -d $DB_NAME

# Restore configuration
tar -xzf config_$DATE.tar.gz -C /

# Start services
docker-compose up -d

# Verify recovery
sleep 30
curl -f http://localhost:8080/actuator/health
curl -f http://localhost:8081/actuator/health
curl -f http://localhost:8082/actuator/health
```

## Performance Tuning

### 1. JVM Tuning

#### Production JVM Options
```bash
# JVM options for production
JAVA_OPTS="-server \
  -Xms2g \
  -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UnlockExperimentalVMOptions \
  -XX:+UseStringDeduplication \
  -XX:+UseCompressedOops \
  -XX:+UseCompressedClassPointers \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/log/audit-logging/heapdump.hprof \
  -XX:+PrintGCDetails \
  -XX:+PrintGCTimeStamps \
  -Xloggc:/var/log/audit-logging/gc.log"
```

#### JVM Monitoring
```bash
# Check JVM memory usage
jstat -gc <pid> 1000

# Check JVM threads
jstack <pid>

# Generate heap dump
jmap -dump:format=b,file=heapdump.hprof <pid>
```

### 2. Database Tuning

#### PostgreSQL Tuning
```sql
-- PostgreSQL performance tuning
ALTER SYSTEM SET shared_buffers = '1GB';
ALTER SYSTEM SET effective_cache_size = '3GB';
ALTER SYSTEM SET maintenance_work_mem = '256MB';
ALTER SYSTEM SET checkpoint_completion_target = 0.9;
ALTER SYSTEM SET wal_buffers = '16MB';
ALTER SYSTEM SET default_statistics_target = 100;
ALTER SYSTEM SET random_page_cost = 1.1;
ALTER SYSTEM SET effective_io_concurrency = 200;

-- Reload configuration
SELECT pg_reload_conf();
```

#### Database Maintenance
```sql
-- Regular maintenance tasks
-- Update statistics
ANALYZE audit_logs;

-- Vacuum tables
VACUUM ANALYZE audit_logs;

-- Reindex if needed
REINDEX TABLE audit_logs;
```

### 3. Application Tuning

#### Connection Pool Tuning
```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

#### Cache Tuning
```yaml
# application.yml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 300000
      cache-null-values: false
```

## Security Operations

### 1. Access Control

#### API Key Management
```bash
#!/bin/bash
# rotate-api-keys.sh
# Generate new API key
NEW_API_KEY=$(openssl rand -hex 32)

# Update configuration
sed -i "s/API_KEY=.*/API_KEY=$NEW_API_KEY/" .env

# Restart services
docker-compose restart audit-log-agent audit-log-query

# Notify stakeholders
echo "API key rotated successfully"
```

#### SSL Certificate Management
```bash
#!/bin/bash
# renew-ssl-cert.sh
# Renew SSL certificate
certbot renew --nginx

# Reload nginx
docker-compose exec nginx nginx -s reload

# Verify certificate
openssl x509 -in /etc/letsencrypt/live/audit.example.com/cert.pem -text -noout
```

### 2. Security Monitoring

#### Failed Login Monitoring
```bash
#!/bin/bash
# monitor-failed-logins.sh
# Check for failed login attempts
FAILED_LOGINS=$(docker exec postgres psql -U audit_user -d audit_logs -t -c "
  SELECT COUNT(*) FROM audit_logs 
  WHERE action = 'LOGIN' AND status = 'FAILED' 
  AND timestamp > NOW() - INTERVAL '1 hour';")

if [ $FAILED_LOGINS -gt 100 ]; then
    echo "WARNING: High number of failed logins detected: $FAILED_LOGINS"
    # Send alert
fi
```

#### Suspicious Activity Detection
```bash
#!/bin/bash
# detect-suspicious-activity.sh
# Check for unusual patterns
SUSPICIOUS_ACTIVITY=$(docker exec postgres psql -U audit_user -d audit_logs -t -c "
  SELECT user_id, COUNT(*) as action_count
  FROM audit_logs 
  WHERE timestamp > NOW() - INTERVAL '1 hour'
  GROUP BY user_id 
  HAVING COUNT(*) > 1000;")

if [ ! -z "$SUSPICIOUS_ACTIVITY" ]; then
    echo "SUSPICIOUS ACTIVITY DETECTED:"
    echo "$SUSPICIOUS_ACTIVITY"
    # Send alert
fi
```

## Troubleshooting

### 1. Common Issues

#### Service Won't Start
```bash
# Check logs
docker-compose logs audit-log-agent

# Check resource usage
docker stats

# Check port conflicts
netstat -tulpn | grep :8080

# Check disk space
df -h

# Check memory
free -h
```

#### Database Connection Issues
```bash
# Test database connectivity
docker exec postgres pg_isready -U audit_user

# Check database logs
docker-compose logs postgres

# Check connection pool
docker exec postgres psql -U audit_user -d audit_logs -c "
  SELECT count(*) as active_connections 
  FROM pg_stat_activity 
  WHERE datname = 'audit_logs';"
```

#### High Memory Usage
```bash
# Check JVM memory
docker exec audit-log-agent jstat -gc 1 1000

# Check system memory
docker stats

# Check for memory leaks
docker exec audit-log-agent jmap -histo:live 1
```

### 2. Performance Issues

#### Slow Queries
```sql
-- Find slow queries
SELECT query, mean_time, calls, total_time
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;

-- Check query execution plan
EXPLAIN ANALYZE SELECT * FROM audit_logs WHERE user_id = 'user123';
```

#### High CPU Usage
```bash
# Check CPU usage by process
top -p $(docker exec audit-log-agent jps | grep AuditLogAgentApplication | awk '{print $1}')

# Check thread dump
docker exec audit-log-agent jstack 1 > thread_dump.txt

# Analyze thread dump
grep "BLOCKED\|WAITING" thread_dump.txt
```

### 3. Network Issues

#### Connectivity Problems
```bash
# Test network connectivity
docker exec audit-log-agent ping postgres
docker exec audit-log-agent telnet postgres 5432

# Check DNS resolution
docker exec audit-log-agent nslookup postgres

# Check firewall rules
iptables -L
```

## Maintenance Schedule

### 1. Daily Tasks
- [ ] Check service health
- [ ] Review error logs
- [ ] Monitor disk usage
- [ ] Check backup status
- [ ] Review performance metrics

### 2. Weekly Tasks
- [ ] Database maintenance (VACUUM, ANALYZE)
- [ ] Log rotation
- [ ] Security audit
- [ ] Performance review
- [ ] Update monitoring dashboards

### 3. Monthly Tasks
- [ ] SSL certificate renewal check
- [ ] API key rotation
- [ ] Database backup verification
- [ ] Capacity planning review
- [ ] Security patch review

### 4. Quarterly Tasks
- [ ] Disaster recovery testing
- [ ] Performance optimization
- [ ] Security assessment
- [ ] Documentation update
- [ ] Training and knowledge transfer

## Emergency Procedures

### 1. Service Outage
```bash
# Emergency restart
docker-compose down
docker-compose up -d

# Check service status
docker-compose ps

# Verify functionality
curl -f http://localhost:8080/actuator/health
```

### 2. Database Outage
```bash
# Check database status
docker exec postgres pg_isready -U audit_user

# Restart database
docker-compose restart postgres

# Verify data integrity
docker exec postgres psql -U audit_user -d audit_logs -c "SELECT COUNT(*) FROM audit_logs;"
```

### 3. Data Loss
```bash
# Stop services
docker-compose down

# Restore from backup
gunzip -c /backup/database/audit_logs_20240115_120000.sql.gz | \
docker exec -i postgres psql -U audit_user -d audit_logs

# Start services
docker-compose up -d

# Verify recovery
curl -f http://localhost:8080/actuator/health
```

## Contact Information

### Emergency Contacts
- **System Administrator**: admin@example.com
- **Database Administrator**: dba@example.com
- **Security Team**: security@example.com

### Escalation Matrix
1. **Level 1**: On-call engineer (15 minutes)
2. **Level 2**: Senior engineer (30 minutes)
3. **Level 3**: System architect (1 hour)
4. **Level 4**: CTO (2 hours) 