# Chiến lược kiểm thử Audit Logging System

## Tổng quan

Chiến lược kiểm thử toàn diện cho hệ thống Audit Logging System, bao gồm unit testing, integration testing, performance testing và security testing.

## Testing Pyramid

```
                    /\
                   /  \
                  / E2E \
                 /________\
                /          \
               / Integration \
              /______________\
             /                \
            /     Unit Tests    \
           /____________________\
```

## 1. Unit Testing

### Mục tiêu
- Test coverage tối thiểu 80%
- Test tất cả business logic
- Mock external dependencies
- Fast execution (< 1 phút)

### Framework và Tools
- **JUnit 5**: Testing framework
- **Mockito**: Mocking framework
- **AssertJ**: Fluent assertions
- **JaCoCo**: Code coverage

### Test Structure
```
src/test/java/
├── unit/
│   ├── service/
│   │   ├── AuditLogServiceTest.java
│   │   └── AuditLogValidationServiceTest.java
│   ├── controller/
│   │   ├── AuditLogControllerTest.java
│   │   └── AdminControllerTest.java
│   ├── repository/
│   │   └── AuditLogRepositoryTest.java
│   └── util/
│       ├── MapperUtilsTest.java
│       └── ValidationUtilsTest.java
```

### Example Unit Test
```java
@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {
    
    @Mock
    private AuditLogRepository auditLogRepository;
    
    @Mock
    private AuditLogMapper auditLogMapper;
    
    @InjectMocks
    private AuditLogService auditLogService;
    
    @Test
    @DisplayName("Should create audit log successfully")
    void shouldCreateAuditLogSuccessfully() {
        // Given
        AuditLogDto inputDto = AuditLogDto.builder()
            .userId("user123")
            .action("LOGIN")
            .timestamp(Instant.now())
            .build();
            
        AuditLog entity = new AuditLog();
        entity.setId("test-id");
        
        AuditLogDto expectedDto = AuditLogDto.builder()
            .id("test-id")
            .userId("user123")
            .action("LOGIN")
            .build();
        
        when(auditLogMapper.toEntity(inputDto)).thenReturn(entity);
        when(auditLogRepository.save(entity)).thenReturn(entity);
        when(auditLogMapper.toDto(entity)).thenReturn(expectedDto);
        
        // When
        AuditLogDto result = auditLogService.createAuditLog(inputDto);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("test-id");
        assertThat(result.getUserId()).isEqualTo("user123");
        
        verify(auditLogRepository).save(entity);
    }
    
    @Test
    @DisplayName("Should throw exception when user ID is null")
    void shouldThrowExceptionWhenUserIdIsNull() {
        // Given
        AuditLogDto inputDto = AuditLogDto.builder()
            .action("LOGIN")
            .timestamp(Instant.now())
            .build();
        
        // When & Then
        assertThatThrownBy(() -> auditLogService.createAuditLog(inputDto))
            .isInstanceOf(ValidationException.class)
            .hasMessage("User ID is required");
    }
}
```

### Test Categories

#### Service Layer Tests
- Business logic validation
- Data transformation
- Error handling
- Edge cases

#### Controller Layer Tests
- Request/response mapping
- Validation annotations
- HTTP status codes
- Error responses

#### Repository Layer Tests
- Query methods
- Data persistence
- Transaction handling

#### Utility Tests
- Helper methods
- Data formatting
- Validation logic

## 2. Integration Testing

### Mục tiêu
- Test component interactions
- Test database operations
- Test API endpoints
- Test external service integration

### Framework và Tools
- **Spring Boot Test**: Integration testing
- **Testcontainers**: Database containers
- **RestAssured**: API testing
- **H2 Database**: In-memory database

### Test Structure
```
src/test/java/
├── integration/
│   ├── api/
│   │   ├── AuditLogControllerIntegrationTest.java
│   │   └── AdminControllerIntegrationTest.java
│   ├── service/
│   │   └── AuditLogServiceIntegrationTest.java
│   ├── repository/
│   │   └── AuditLogRepositoryIntegrationTest.java
│   └── database/
│       └── DatabaseMigrationTest.java
```

### Example Integration Test
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuditLogControllerIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("audit_logs_test")
        .withUsername("test_user")
        .withPassword("test_password");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Test
    @DisplayName("Should create audit log via API")
    void shouldCreateAuditLogViaApi() {
        // Given
        AuditLogDto request = AuditLogDto.builder()
            .userId("user123")
            .action("LOGIN")
            .timestamp(Instant.now())
            .build();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", "test-api-key");
        
        HttpEntity<AuditLogDto> entity = new HttpEntity<>(request, headers);
        
        // When
        ResponseEntity<ApiResponse<AuditLogDto>> response = restTemplate.exchange(
            "/api/v1/audit-logs",
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<ApiResponse<AuditLogDto>>() {}
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getUserId()).isEqualTo("user123");
    }
    
    @Test
    @DisplayName("Should return 401 when API key is missing")
    void shouldReturn401WhenApiKeyIsMissing() {
        // Given
        AuditLogDto request = AuditLogDto.builder()
            .userId("user123")
            .action("LOGIN")
            .build();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<AuditLogDto> entity = new HttpEntity<>(request, headers);
        
        // When
        ResponseEntity<ApiResponse<AuditLogDto>> response = restTemplate.exchange(
            "/api/v1/audit-logs",
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<ApiResponse<AuditLogDto>>() {}
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
```

### Test Categories

#### API Integration Tests
- Endpoint functionality
- Request/response validation
- Authentication/authorization
- Error handling

#### Database Integration Tests
- CRUD operations
- Transaction handling
- Data integrity
- Migration testing

#### Service Integration Tests
- Component interactions
- Business workflow
- Error propagation

## 3. End-to-End Testing

### Mục tiêu
- Test complete user workflows
- Test system integration
- Validate business requirements
- Test production-like scenarios

### Framework và Tools
- **Cucumber**: BDD testing
- **Selenium**: Web UI testing
- **RestAssured**: API testing
- **Docker Compose**: Environment setup

### Test Structure
```
src/test/java/
├── e2e/
│   ├── features/
│   │   ├── audit_log_creation.feature
│   │   ├── audit_log_search.feature
│   │   └── admin_operations.feature
│   ├── steps/
│   │   ├── AuditLogSteps.java
│   │   └── AdminSteps.java
│   └── support/
│       ├── TestContext.java
│       └── TestDataBuilder.java
```

### Example E2E Test
```gherkin
Feature: Audit Log Creation and Search
  As a system administrator
  I want to create and search audit logs
  So that I can track user activities

  Scenario: Create audit log and search by user
    Given I have a valid API key
    When I create an audit log for user "user123" with action "LOGIN"
    Then the audit log should be created successfully
    And I should be able to search for audit logs by user "user123"
    And the search results should contain the created audit log
```

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuditLogE2ETest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    
    @Container
    static RedisContainer<?> redis = new RedisContainer<>("redis:7-alpine");
    
    @Test
    @DisplayName("Complete audit log workflow")
    void completeAuditLogWorkflow() {
        // Step 1: Create audit log
        AuditLogDto createdLog = createAuditLog("user123", "LOGIN");
        assertThat(createdLog.getId()).isNotNull();
        
        // Step 2: Search audit log
        List<AuditLogDto> searchResults = searchAuditLogs("user123");
        assertThat(searchResults).hasSize(1);
        assertThat(searchResults.get(0).getId()).isEqualTo(createdLog.getId());
        
        // Step 3: Get audit log by ID
        AuditLogDto retrievedLog = getAuditLogById(createdLog.getId());
        assertThat(retrievedLog).isEqualTo(createdLog);
        
        // Step 4: Get statistics
        AuditLogStatistics stats = getAuditLogStatistics();
        assertThat(stats.getTotalLogs()).isGreaterThan(0);
    }
    
    private AuditLogDto createAuditLog(String userId, String action) {
        // Implementation
    }
    
    private List<AuditLogDto> searchAuditLogs(String userId) {
        // Implementation
    }
    
    private AuditLogDto getAuditLogById(String id) {
        // Implementation
    }
    
    private AuditLogStatistics getAuditLogStatistics() {
        // Implementation
    }
}
```

## 4. Performance Testing

### Mục tiêu
- Validate system performance under load
- Identify performance bottlenecks
- Test scalability
- Establish performance baselines

### Framework và Tools
- **JMeter**: Load testing
- **Gatling**: Performance testing
- **Prometheus**: Metrics collection
- **Grafana**: Performance visualization

### Test Scenarios

#### Load Testing
```scala
// Gatling test
class AuditLogLoadTest extends Simulation {
  
  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .header("X-API-Key", "test-api-key")
    .contentTypeHeader("application/json")
  
  val createAuditLog = exec(
    http("Create Audit Log")
      .post("/api/v1/audit-logs")
      .body(StringBody("""{
        "userId": "user${random.nextInt(1000)}",
        "action": "LOGIN",
        "timestamp": "${System.currentTimeMillis()}"
      }"""))
      .check(status.is(201))
  )
  
  val searchAuditLogs = exec(
    http("Search Audit Logs")
      .get("/api/v1/audit-logs/search?userId=user123")
      .check(status.is(200))
  )
  
  val scn = scenario("Audit Log Load Test")
    .exec(createAuditLog)
    .exec(searchAuditLogs)
  
  setUp(
    scn.inject(
      rampUsers(100).during(10.seconds),
      constantUsersPerSec(50).during(30.seconds)
    )
  ).protocols(httpProtocol)
}
```

#### Stress Testing
- Test system behavior under extreme load
- Identify breaking points
- Test recovery mechanisms

#### Endurance Testing
- Test system stability over time
- Monitor memory leaks
- Test resource exhaustion

### Performance Metrics
- **Response Time**: Average, P95, P99
- **Throughput**: Requests per second
- **Error Rate**: Percentage of failed requests
- **Resource Usage**: CPU, memory, disk I/O

## 5. Security Testing

### Mục tiêu
- Identify security vulnerabilities
- Test authentication/authorization
- Validate data protection
- Test input validation

### Framework và Tools
- **OWASP ZAP**: Security scanning
- **SonarQube**: Code security analysis
- **JUnit Security**: Security testing
- **Dependency Check**: Vulnerability scanning

### Test Categories

#### Authentication Testing
```java
@Test
@DisplayName("Should reject requests without API key")
void shouldRejectRequestsWithoutApiKey() {
    // Test implementation
}

@Test
@DisplayName("Should reject invalid API keys")
void shouldRejectInvalidApiKeys() {
    // Test implementation
}
```

#### Authorization Testing
```java
@Test
@DisplayName("Should allow admin access to admin endpoints")
void shouldAllowAdminAccessToAdminEndpoints() {
    // Test implementation
}

@Test
@DisplayName("Should deny non-admin access to admin endpoints")
void shouldDenyNonAdminAccessToAdminEndpoints() {
    // Test implementation
}
```

#### Input Validation Testing
```java
@Test
@DisplayName("Should reject SQL injection attempts")
void shouldRejectSqlInjectionAttempts() {
    // Test implementation
}

@Test
@DisplayName("Should reject XSS attempts")
void shouldRejectXssAttempts() {
    // Test implementation
}
```

## 6. Test Data Management

### Test Data Strategy
```java
@Component
public class TestDataBuilder {
    
    public AuditLogDto createValidAuditLog() {
        return AuditLogDto.builder()
            .userId("test-user-" + UUID.randomUUID())
            .action("TEST_ACTION")
            .timestamp(Instant.now())
            .status("SUCCESS")
            .build();
    }
    
    public AuditLogDto createInvalidAuditLog() {
        return AuditLogDto.builder()
            .action("TEST_ACTION")
            .timestamp(Instant.now())
            .build();
    }
    
    public List<AuditLogDto> createMultipleAuditLogs(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> createValidAuditLog())
            .collect(Collectors.toList());
    }
}
```

### Database Seeding
```java
@Sql(scripts = {
    "classpath:sql/cleanup.sql",
    "classpath:sql/test-data.sql"
})
class AuditLogRepositoryIntegrationTest {
    // Test implementation
}
```

## 7. Test Configuration

### Test Properties
```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  test:
    database:
      replace: none

logging:
  level:
    com.haiphamcoder.auditlog: DEBUG
    org.springframework.web: DEBUG

audit:
  api:
    key: test-api-key
  security:
    jwt:
      secret: test-jwt-secret
```

### Test Profiles
```java
@ActiveProfiles("test")
@SpringBootTest
class AuditLogServiceTest {
    // Test implementation
}
```

## 8. Continuous Testing

### CI/CD Integration
```yaml
# .github/workflows/test.yml
name: Tests
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
          
      - name: Run unit tests
        run: mvn test
        
      - name: Run integration tests
        run: mvn verify -P integration-test
        
      - name: Generate coverage report
        run: mvn jacoco:report
        
      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
```

### Test Reporting
- **JaCoCo**: Code coverage reports
- **Allure**: Test reporting
- **SonarQube**: Quality gates
- **Grafana**: Performance dashboards

## 9. Test Maintenance

### Best Practices
- Keep tests simple and focused
- Use descriptive test names
- Maintain test data consistency
- Regular test review and cleanup
- Update tests with code changes

### Test Documentation
- Document test scenarios
- Maintain test data schemas
- Document test environment setup
- Keep test runbooks updated

## 10. Quality Gates

### Coverage Requirements
- Unit test coverage: ≥ 80%
- Integration test coverage: ≥ 70%
- Critical path coverage: 100%

### Performance Requirements
- Response time: < 500ms (P95)
- Throughput: > 1000 req/sec
- Error rate: < 1%

### Security Requirements
- No critical vulnerabilities
- All security tests passing
- Dependency vulnerabilities resolved 