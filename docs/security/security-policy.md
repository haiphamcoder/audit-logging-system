# Chính sách bảo mật Audit Logging System

## Tổng quan

Chính sách bảo mật này định nghĩa các nguyên tắc, quy tắc và thủ tục bảo mật cho hệ thống Audit Logging System, đảm bảo tính bảo mật, tính toàn vẹn và tính khả dụng của dữ liệu audit.

## 1. Nguyên tắc bảo mật

### 1.1 Nguyên tắc Defense in Depth
- Triển khai nhiều lớp bảo mật
- Không phụ thuộc vào một biện pháp bảo mật duy nhất
- Bảo vệ ở mọi tầng của hệ thống

### 1.2 Nguyên tắc Least Privilege
- Cấp quyền tối thiểu cần thiết
- Kiểm soát truy cập dựa trên vai trò
- Định kỳ review và thu hồi quyền

### 1.3 Nguyên tắc Zero Trust
- Không tin tưởng mặc định
- Xác thực và phân quyền mọi request
- Giám sát liên tục

## 2. Bảo mật ứng dụng

### 2.1 Xác thực (Authentication)

#### API Key Authentication
```yaml
# Cấu hình API Key
audit:
  security:
    api-key:
      enabled: true
      header-name: "X-API-Key"
      rotation-period: 90d
      max-keys-per-client: 2
```

#### JWT Authentication (Admin)
```yaml
# Cấu hình JWT
audit:
  security:
    jwt:
      secret: ${JWT_SECRET}
      expiration: 3600s
      refresh-expiration: 86400s
      issuer: "audit-logging-system"
      audience: "audit-admin"
```

#### Multi-Factor Authentication
- Yêu cầu MFA cho admin access
- Sử dụng TOTP hoặc SMS
- Backup codes cho emergency access

### 2.2 Phân quyền (Authorization)

#### Role-Based Access Control (RBAC)
```java
public enum AuditRole {
    AUDIT_VIEWER,      // Chỉ xem audit logs
    AUDIT_ANALYST,     // Xem và phân tích
    AUDIT_ADMIN,       // Quản lý hệ thống
    SYSTEM_ADMIN       // Quản lý toàn bộ
}
```

#### Resource-Based Permissions
```java
@PreAuthorize("hasRole('AUDIT_ADMIN') or @auditPermissionEvaluator.canAccess(#auditLogId)")
public AuditLogDto getAuditLog(String auditLogId) {
    // Implementation
}
```

### 2.3 Input Validation

#### Request Validation
```java
@Valid
public class AuditLogDto {
    
    @NotBlank(message = "User ID is required")
    @Size(max = 100, message = "User ID must not exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "User ID contains invalid characters")
    private String userId;
    
    @NotBlank(message = "Action is required")
    @Size(max = 50, message = "Action must not exceed 50 characters")
    @Pattern(regexp = "^[A-Z_]+$", message = "Action must be uppercase with underscores")
    private String action;
    
    @Size(max = 500, message = "Request path must not exceed 500 characters")
    @Pattern(regexp = "^[a-zA-Z0-9/_-]+$", message = "Request path contains invalid characters")
    private String requestPath;
}
```

#### SQL Injection Prevention
```java
// Sử dụng Prepared Statements
@Query("SELECT a FROM AuditLog a WHERE a.userId = :userId AND a.timestamp BETWEEN :startDate AND :endDate")
List<AuditLog> findByUserIdAndDateRange(
    @Param("userId") String userId,
    @Param("startDate") Instant startDate,
    @Param("endDate") Instant endDate
);
```

#### XSS Prevention
```java
// Sanitize input
@Component
public class InputSanitizer {
    
    public String sanitizeHtml(String input) {
        return Jsoup.clean(input, Whitelist.none());
    }
    
    public String sanitizeJson(String input) {
        // JSON validation and sanitization
        return validateAndSanitizeJson(input);
    }
}
```

## 3. Bảo mật dữ liệu

### 3.1 Mã hóa dữ liệu

#### Encryption at Rest
```yaml
# Database encryption
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/audit_logs?sslmode=require
    hikari:
      ssl: true
      ssl-cert: /path/to/client-cert.pem
      ssl-key: /path/to/client-key.pem
      ssl-root-cert: /path/to/ca-cert.pem
```

#### Encryption in Transit
```yaml
# TLS configuration
server:
  ssl:
    enabled: true
    key-store: /path/to/keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: audit-logging
  port: 8443
```

#### Field-Level Encryption
```java
@Entity
public class AuditLog {
    
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "sensitive_data")
    private String sensitiveData;
    
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "additional_data")
    private String additionalData;
}
```

### 3.2 Data Masking

#### Sensitive Data Masking
```java
@Component
public class DataMaskingService {
    
    public String maskUserId(String userId) {
        if (userId == null || userId.length() <= 4) {
            return "***";
        }
        return userId.substring(0, 2) + "***" + userId.substring(userId.length() - 2);
    }
    
    public String maskIpAddress(String ipAddress) {
        if (ipAddress == null) {
            return "***";
        }
        String[] parts = ipAddress.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + ".***.***";
        }
        return "***";
    }
}
```

### 3.3 Data Retention

#### Retention Policy
```yaml
audit:
  retention:
    default-period: 7y
    sensitive-data-period: 3y
    admin-logs-period: 10y
    auto-purge: true
    purge-batch-size: 10000
```

#### Data Archival
```java
@Service
public class DataArchivalService {
    
    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    public void archiveOldData() {
        Instant cutoffDate = Instant.now().minus(Duration.ofDays(2555)); // 7 years
        
        List<AuditLog> oldLogs = auditLogRepository.findByTimestampBefore(cutoffDate);
        
        // Archive to cold storage
        archiveService.archive(oldLogs);
        
        // Delete from primary database
        auditLogRepository.deleteAll(oldLogs);
    }
}
```

## 4. Bảo mật mạng

### 4.1 Network Segmentation

#### Firewall Rules
```bash
# Allow only necessary ports
iptables -A INPUT -p tcp --dport 8443 -j ACCEPT  # HTTPS
iptables -A INPUT -p tcp --dport 5432 -j ACCEPT  # PostgreSQL (internal only)
iptables -A INPUT -p tcp --dport 6379 -j ACCEPT  # Redis (internal only)
iptables -A INPUT -j DROP
```

#### Network Policies (Kubernetes)
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: audit-logging-network-policy
  namespace: audit-logging-system
spec:
  podSelector:
    matchLabels:
      app: audit-logging
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: ingress-nginx
    ports:
    - protocol: TCP
      port: 8080
  egress:
  - to:
    - namespaceSelector:
        matchLabels:
          name: database
    ports:
    - protocol: TCP
      port: 5432
```

### 4.2 VPN và Remote Access

#### VPN Configuration
```bash
# OpenVPN configuration
client
dev tun
proto udp
remote audit.example.com 1194
resolv-retry infinite
nobind
persist-key
persist-tun
ca ca.crt
cert client.crt
key client.key
remote-cert-tls server
cipher AES-256-CBC
auth SHA256
key-direction 1
verb 3
```

### 4.3 DDoS Protection

#### Rate Limiting
```java
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    
    private final RateLimiter rateLimiter;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String clientId = getClientId(request);
        
        if (!rateLimiter.tryAcquire(clientId)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            return false;
        }
        
        return true;
    }
}
```

## 5. Monitoring và Logging

### 5.1 Security Event Logging

#### Security Logs
```java
@Component
public class SecurityEventLogger {
    
    public void logAuthenticationSuccess(String userId, String ipAddress) {
        logSecurityEvent("AUTH_SUCCESS", userId, ipAddress, "Authentication successful");
    }
    
    public void logAuthenticationFailure(String userId, String ipAddress, String reason) {
        logSecurityEvent("AUTH_FAILURE", userId, ipAddress, "Authentication failed: " + reason);
    }
    
    public void logAuthorizationFailure(String userId, String resource, String action) {
        logSecurityEvent("AUTHZ_FAILURE", userId, null, 
            "Authorization failed for " + action + " on " + resource);
    }
    
    private void logSecurityEvent(String eventType, String userId, String ipAddress, String message) {
        SecurityEvent event = SecurityEvent.builder()
            .eventType(eventType)
            .userId(userId)
            .ipAddress(ipAddress)
            .message(message)
            .timestamp(Instant.now())
            .build();
        
        securityEventRepository.save(event);
    }
}
```

### 5.2 Intrusion Detection

#### Anomaly Detection
```java
@Service
public class AnomalyDetectionService {
    
    @Scheduled(fixedRate = 60000) // Every minute
    public void detectAnomalies() {
        // Check for failed login attempts
        checkFailedLogins();
        
        // Check for unusual access patterns
        checkAccessPatterns();
        
        // Check for data exfiltration attempts
        checkDataExfiltration();
    }
    
    private void checkFailedLogins() {
        Instant oneHourAgo = Instant.now().minus(Duration.ofHours(1));
        
        List<SecurityEvent> failedLogins = securityEventRepository
            .findByEventTypeAndTimestampAfter("AUTH_FAILURE", oneHourAgo);
        
        Map<String, Long> failuresByIp = failedLogins.stream()
            .collect(Collectors.groupingBy(SecurityEvent::getIpAddress, Collectors.counting()));
        
        failuresByIp.entrySet().stream()
            .filter(entry -> entry.getValue() > 10)
            .forEach(entry -> {
                log.warn("Suspicious activity detected: {} failed logins from IP {}", 
                    entry.getValue(), entry.getKey());
                triggerAlert("SUSPICIOUS_LOGIN_ATTEMPTS", entry.getKey());
            });
    }
}
```

### 5.3 Security Alerts

#### Alert Configuration
```yaml
security:
  alerts:
    failed-login-threshold: 10
    suspicious-activity-threshold: 100
    data-exfiltration-threshold: 1000
    alert-channels:
      - email: security@example.com
      - slack: #security-alerts
      - pagerduty: audit-logging-security
```

## 6. Incident Response

### 6.1 Incident Classification

#### Severity Levels
- **Critical**: Data breach, system compromise
- **High**: Unauthorized access, suspicious activity
- **Medium**: Failed authentication attempts, unusual patterns
- **Low**: Policy violations, minor security events

### 6.2 Response Procedures

#### Critical Incident Response
```java
@Service
public class IncidentResponseService {
    
    public void handleCriticalIncident(SecurityIncident incident) {
        // 1. Immediate containment
        containThreat(incident);
        
        // 2. Preserve evidence
        preserveEvidence(incident);
        
        // 3. Notify stakeholders
        notifyStakeholders(incident);
        
        // 4. Investigate
        investigateIncident(incident);
        
        // 5. Remediate
        remediateIncident(incident);
        
        // 6. Document lessons learned
        documentLessonsLearned(incident);
    }
    
    private void containThreat(SecurityIncident incident) {
        // Block suspicious IPs
        // Disable compromised accounts
        // Isolate affected systems
    }
}
```

### 6.3 Forensics

#### Digital Forensics
```java
@Component
public class ForensicsService {
    
    public void collectEvidence(SecurityIncident incident) {
        // Collect system logs
        collectSystemLogs(incident);
        
        // Collect network logs
        collectNetworkLogs(incident);
        
        // Collect application logs
        collectApplicationLogs(incident);
        
        // Create forensic image
        createForensicImage(incident);
        
        // Preserve evidence
        preserveEvidence(incident);
    }
}
```

## 7. Compliance

### 7.1 GDPR Compliance

#### Data Protection
```java
@Service
public class GDPRComplianceService {
    
    public void handleDataSubjectRequest(String userId, String requestType) {
        switch (requestType) {
            case "ACCESS":
                provideDataAccess(userId);
                break;
            case "DELETION":
                deleteUserData(userId);
                break;
            case "PORTABILITY":
                exportUserData(userId);
                break;
            case "RECTIFICATION":
                rectifyUserData(userId);
                break;
        }
    }
    
    public void deleteUserData(String userId) {
        // Anonymize or delete user data
        List<AuditLog> userLogs = auditLogRepository.findByUserId(userId);
        
        userLogs.forEach(log -> {
            log.setUserId("ANONYMIZED_" + log.getId());
            log.setUsername("ANONYMIZED");
            log.setIpAddress("ANONYMIZED");
            log.setUserAgent("ANONYMIZED");
        });
        
        auditLogRepository.saveAll(userLogs);
    }
}
```

### 7.2 SOX Compliance

#### Audit Trail
```java
@Entity
@Table(name = "audit_trail")
public class AuditTrail {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(nullable = false)
    private String action;
    
    @Column(nullable = false)
    private String resource;
    
    @Column(nullable = false)
    private Instant timestamp;
    
    @Column(nullable = false)
    private String ipAddress;
    
    @Column(nullable = false)
    private String userAgent;
    
    @Column(columnDefinition = "TEXT")
    private String details;
    
    @Column(nullable = false)
    private String hash; // Cryptographic hash for integrity
}
```

## 8. Security Training

### 8.1 Developer Security Training

#### Security Guidelines
- Secure coding practices
- OWASP Top 10 awareness
- Input validation techniques
- Authentication and authorization best practices
- Data protection methods

### 8.2 Operational Security Training

#### Security Procedures
- Incident response procedures
- Access control management
- Security monitoring and alerting
- Backup and recovery procedures
- Change management security

## 9. Security Testing

### 9.1 Penetration Testing

#### Testing Schedule
- Quarterly penetration testing
- Annual security assessment
- Continuous vulnerability scanning
- Code security reviews

### 9.2 Security Tools

#### Static Analysis
- SonarQube security analysis
- OWASP Dependency Check
- SAST (Static Application Security Testing)

#### Dynamic Analysis
- OWASP ZAP
- Burp Suite
- DAST (Dynamic Application Security Testing)

## 10. Security Metrics

### 10.1 Key Performance Indicators

#### Security Metrics
- Number of security incidents
- Mean time to detect (MTTD)
- Mean time to respond (MTTR)
- Security patch compliance rate
- Failed authentication attempts
- Suspicious activity alerts

### 10.2 Security Dashboard

#### Monitoring Dashboard
```yaml
security:
  dashboard:
    metrics:
      - security_incidents_total
      - authentication_failures_total
      - suspicious_activities_total
      - data_access_attempts_total
    alerts:
      - critical_security_events
      - failed_authentication_threshold
      - unusual_access_patterns
```

## 11. Review và Cập nhật

### 11.1 Policy Review
- Annual security policy review
- Quarterly security procedure updates
- Monthly security metrics review
- Continuous improvement process

### 11.2 Compliance Audits
- Annual security compliance audit
- Quarterly security assessments
- Monthly security reviews
- Continuous monitoring and reporting 