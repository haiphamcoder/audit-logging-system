# Kiến trúc hệ thống Audit Logging System

## Tổng quan kiến trúc

Hệ thống Audit Logging System được thiết kế theo kiến trúc microservice với 4 module chính, mỗi module có trách nhiệm riêng biệt và có thể triển khai độc lập.

## Sơ đồ kiến trúc tổng thể

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Client Apps   │    │   Web Apps      │    │   Mobile Apps   │
└─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘
          │                      │                      │
          └──────────────────────┼──────────────────────┘
                                 │
                    ┌─────────────▼─────────────┐
                    │    audit-log-agent        │
                    │   (Port: 8080)            │
                    │   - Thu thập logs         │
                    │   - Validate data         │
                    │   - Forward to worker     │
                    └─────────────┬─────────────┘
                                  │
                    ┌─────────────▼─────────────┐
                    │    audit-log-worker       │
                    │   (Port: 8081)            │
                    │   - Xử lý logs            │
                    │   - Lưu trữ database      │
                    │   - Indexing              │
                    └─────────────┬─────────────┘
                                  │
                    ┌─────────────▼─────────────┐
                    │    audit-log-query        │
                    │   (Port: 8082)            │
                    │   - API truy vấn          │
                    │   - Dashboard             │
                    │   - Reports               │
                    └───────────────────────────┘
```

## Chi tiết các module

### 1. Audit Log Agent (Port: 8080)

**Chức năng chính:**
- Thu thập audit logs từ các ứng dụng client
- Validate và chuẩn hóa dữ liệu
- Forward logs đến worker service
- Cung cấp REST API để nhận logs

**Công nghệ:**
- Spring Boot 3.5.3
- RESTful API
- Data validation
- Message queuing (nếu cần)

### 2. Audit Log Worker (Port: 8081)

**Chức năng chính:**
- Xử lý và enrich audit logs
- Lưu trữ vào database
- Indexing cho tìm kiếm
- Batch processing
- Data retention management

**Công nghệ:**
- Spring Boot 3.5.3
- Database connectivity
- Message processing
- Background jobs

### 3. Audit Log Query (Port: 8082)

**Chức năng chính:**
- REST API để truy vấn logs
- Dashboard web interface
- Reporting và analytics
- Export functionality
- Search và filtering

**Công nghệ:**
- Spring Boot 3.5.3
- RESTful API
- Web interface
- Search engine integration

### 4. Audit Log Common

**Chức năng chính:**
- Shared DTOs và models
- Common utilities
- Constants và enums
- Mapper utilities

**Công nghệ:**
- Java 17
- Jackson for JSON
- Lombok
- Common utilities

## Luồng dữ liệu

### 1. Thu thập logs
```
Client App → audit-log-agent → audit-log-worker → Database
```

### 2. Truy vấn logs
```
User/Client → audit-log-query → Database → Response
```

## Công nghệ và Framework

### Backend Stack
- **Java**: 17
- **Spring Boot**: 3.5.3
- **Maven**: Build tool
- **Docker**: Containerization
- **Docker Compose**: Orchestration

### Database (Đề xuất)
- **Primary DB**: PostgreSQL hoặc MySQL
- **Search Engine**: Elasticsearch (tùy chọn)
- **Cache**: Redis (tùy chọn)

### Monitoring & Logging
- **Application Logs**: Logback
- **Metrics**: Micrometer + Prometheus
- **Health Checks**: Spring Boot Actuator

## Scalability Considerations

### Horizontal Scaling
- Mỗi service có thể scale độc lập
- Load balancer cho agent service
- Database clustering
- Message queue cho async processing

### Performance Optimization
- Database indexing
- Caching strategies
- Connection pooling
- Batch processing

## Security Architecture

### Authentication & Authorization
- API key authentication
- JWT tokens
- Role-based access control

### Data Protection
- Encryption at rest
- Encryption in transit (HTTPS)
- Data masking
- Audit trail protection

## Deployment Architecture

### Container Strategy
- Mỗi service trong container riêng
- Docker Compose cho development
- Kubernetes cho production

### Environment Strategy
- Development
- Staging
- Production

## Monitoring & Observability

### Health Checks
- Spring Boot Actuator endpoints
- Database connectivity
- External service dependencies

### Metrics
- Request/response times
- Error rates
- Throughput
- Resource utilization

### Logging
- Structured logging
- Centralized log aggregation
- Log retention policies 