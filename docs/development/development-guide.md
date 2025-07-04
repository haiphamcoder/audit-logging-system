# Hướng dẫn phát triển Audit Logging System

## Yêu cầu hệ thống

### Yêu cầu tối thiểu
- **Java**: JDK 17 hoặc cao hơn
- **Maven**: 3.6+ 
- **Docker**: 20.10+
- **Docker Compose**: 2.0+
- **IDE**: IntelliJ IDEA, Eclipse, hoặc VS Code

### Yêu cầu khuyến nghị
- **RAM**: 8GB+
- **Storage**: 10GB+ free space
- **OS**: Linux, macOS, hoặc Windows 10+

## Thiết lập môi trường phát triển

### 1. Clone repository
```bash
git clone <repository-url>
cd audit-logging-system
```

### 2. Cài đặt dependencies
```bash
# Build toàn bộ project
mvn clean install

# Hoặc build từng module
mvn clean install -pl audit-log-common
mvn clean install -pl audit-log-agent
mvn clean install -pl audit-log-worker
mvn clean install -pl audit-log-query
```

### 3. Cấu hình IDE
#### IntelliJ IDEA
1. Mở project trong IntelliJ IDEA
2. Import Maven project
3. Cấu hình JDK 17
4. Enable annotation processing cho Lombok

#### Eclipse
1. Import Maven project
2. Cài đặt Lombok plugin
3. Cấu hình JDK 17

## Cấu trúc project

```
audit-logging-system/
├── audit-log-agent/          # Module thu thập logs
├── audit-log-common/         # Module dùng chung
├── audit-log-query/          # Module truy vấn
├── audit-log-worker/         # Module xử lý
├── docs/                     # Tài liệu
├── docker-compose.yml        # Docker orchestration
└── pom.xml                   # Parent POM
```

## Quy trình phát triển

### 1. Branch Strategy
```
main (production)
├── develop (integration)
├── feature/audit-log-agent
├── feature/audit-log-worker
├── feature/audit-log-query
└── hotfix/critical-fix
```

### 2. Git Workflow
1. **Feature Development**
   ```bash
   git checkout develop
   git pull origin develop
   git checkout -b feature/your-feature-name
   # Develop your feature
   git commit -m "feat: add new feature"
   git push origin feature/your-feature-name
   # Create Pull Request
   ```

2. **Code Review**
   - Tất cả code phải được review
   - Sử dụng Pull Request
   - Require approval từ ít nhất 1 reviewer

3. **Merge Strategy**
   - Squash merge cho feature branches
   - Merge commit cho hotfix

### 3. Commit Convention
Sử dụng [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add new audit log endpoint
fix: resolve database connection issue
docs: update API documentation
style: format code according to style guide
refactor: restructure audit log processing
test: add unit tests for audit service
chore: update dependencies
```

## Coding Standards

### Java Coding Standards
- Tuân thủ Google Java Style Guide
- Sử dụng Java 17 features
- Prefer immutability
- Use meaningful variable names
- Add comprehensive comments

### Spring Boot Best Practices
- Use constructor injection
- Implement proper exception handling
- Use @Valid for input validation
- Implement proper logging
- Use configuration properties

### Example Code Structure
```java
@Service
@Slf4j
@RequiredArgsConstructor
public class AuditLogService {
    
    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;
    
    public AuditLogDto createAuditLog(AuditLogDto auditLogDto) {
        log.info("Creating audit log for user: {}", auditLogDto.getUserId());
        
        try {
            AuditLog auditLog = auditLogMapper.toEntity(auditLogDto);
            AuditLog savedAuditLog = auditLogRepository.save(auditLog);
            
            log.info("Successfully created audit log with id: {}", savedAuditLog.getId());
            return auditLogMapper.toDto(savedAuditLog);
            
        } catch (Exception e) {
            log.error("Error creating audit log: {}", e.getMessage(), e);
            throw new AuditLogException("Failed to create audit log", e);
        }
    }
}
```

## Testing Strategy

### 1. Unit Tests
- Test coverage tối thiểu 80%
- Sử dụng JUnit 5
- Mock external dependencies
- Test edge cases

### 2. Integration Tests
- Test API endpoints
- Test database operations
- Test service interactions

### 3. Test Structure
```
src/
├── main/
│   └── java/
└── test/
    ├── java/
    │   └── com/haiphamcoder/auditlog/
    │       ├── unit/
    │       ├── integration/
    │       └── e2e/
    └── resources/
        ├── application-test.properties
        └── test-data/
```

### 4. Running Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=AuditLogServiceTest

# Run with coverage
mvn test jacoco:report
```

## API Development

### REST API Design
- Sử dụng HTTP methods đúng cách
- Return appropriate HTTP status codes
- Implement proper error handling
- Use consistent URL patterns

### API Documentation
- Sử dụng OpenAPI 3.0 (Swagger)
- Document all endpoints
- Provide request/response examples
- Include error responses

### Example API Endpoint
```java
@RestController
@RequestMapping("/api/v1/audit-logs")
@Validated
@Slf4j
public class AuditLogController {
    
    private final AuditLogService auditLogService;
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<AuditLogDto> createAuditLog(
            @Valid @RequestBody AuditLogDto auditLogDto) {
        
        log.info("Received audit log creation request");
        AuditLogDto created = auditLogService.createAuditLog(auditLogDto);
        
        return ResponseEntity.created(
            URI.create("/api/v1/audit-logs/" + created.getId())
        ).body(created);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<AuditLogDto> getAuditLog(@PathVariable String id) {
        AuditLogDto auditLog = auditLogService.getAuditLog(id);
        return ResponseEntity.ok(auditLog);
    }
}
```

## Database Development

### Entity Design
```java
@Entity
@Table(name = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "action", nullable = false)
    private String action;
    
    @Column(name = "resource")
    private String resource;
    
    @Column(name = "resource_id")
    private String resourceId;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "user_agent")
    private String userAgent;
    
    @Column(name = "additional_data", columnDefinition = "JSON")
    private String additionalData;
}
```

### Migration Strategy
- Sử dụng Flyway hoặc Liquibase
- Version control database schema
- Test migrations trước khi deploy

## Logging và Monitoring

### Logging Standards
```java
@Slf4j
public class AuditLogService {
    
    public void processAuditLog(AuditLogDto auditLog) {
        log.info("Processing audit log for user: {}, action: {}", 
                auditLog.getUserId(), auditLog.getAction());
        
        try {
            // Processing logic
            log.debug("Audit log processed successfully");
        } catch (Exception e) {
            log.error("Failed to process audit log: {}", e.getMessage(), e);
            throw e;
        }
    }
}
```

### Log Levels
- **ERROR**: Lỗi nghiêm trọng cần xử lý ngay
- **WARN**: Cảnh báo, có thể ảnh hưởng đến hệ thống
- **INFO**: Thông tin quan trọng về business logic
- **DEBUG**: Thông tin chi tiết cho debugging
- **TRACE**: Thông tin rất chi tiết

## Performance Considerations

### Database Optimization
- Sử dụng proper indexing
- Implement pagination
- Use connection pooling
- Optimize queries

### Caching Strategy
- Cache frequently accessed data
- Use Redis cho distributed caching
- Implement cache invalidation

### Async Processing
- Use @Async cho heavy operations
- Implement message queues
- Handle timeouts properly

## Security Best Practices

### Input Validation
```java
@Valid
public class AuditLogDto {
    
    @NotBlank(message = "User ID is required")
    @Size(max = 100, message = "User ID must not exceed 100 characters")
    private String userId;
    
    @NotBlank(message = "Action is required")
    @Pattern(regexp = "^[A-Z_]+$", message = "Action must be uppercase with underscores")
    private String action;
}
```

### Authentication & Authorization
- Implement proper authentication
- Use role-based access control
- Validate permissions
- Log security events

## Troubleshooting

### Common Issues
1. **Build Failures**
   - Check Java version
   - Verify Maven dependencies
   - Check for compilation errors

2. **Runtime Errors**
   - Check application logs
   - Verify configuration
   - Check database connectivity

3. **Performance Issues**
   - Monitor resource usage
   - Check database queries
   - Review caching strategy

### Debug Tools
- Spring Boot Actuator
- Application logs
- Database query logs
- Performance monitoring tools

## Resources

### Documentation
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Java 17 Documentation](https://docs.oracle.com/en/java/javase/17/)
- [Maven Documentation](https://maven.apache.org/guides/)

### Tools
- [IntelliJ IDEA](https://www.jetbrains.com/idea/)
- [Postman](https://www.postman.com/) (API testing)
- [DBeaver](https://dbeaver.io/) (Database client)
- [Docker Desktop](https://www.docker.com/products/docker-desktop) 