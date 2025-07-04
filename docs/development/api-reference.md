# API Reference - Audit Logging System

## Tổng quan

API Reference cho hệ thống Audit Logging System, bao gồm các endpoint cho việc thu thập, xử lý và truy vấn audit logs.

## Base URLs

- **Development**: `http://localhost:8080`
- **Staging**: `https://staging-audit.example.com`
- **Production**: `https://audit.example.com`

## Authentication

### API Key Authentication
Tất cả API calls yêu cầu API key trong header:

```
X-API-Key: your-api-key-here
```

### JWT Authentication (cho admin endpoints)
```
Authorization: Bearer <jwt-token>
```

## Common Response Format

### Success Response
```json
{
  "success": true,
  "data": {
    // Response data
  },
  "message": "Operation completed successfully",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### Error Response
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": [
      {
        "field": "userId",
        "message": "User ID is required"
      }
    ]
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## HTTP Status Codes

- `200` - OK
- `201` - Created
- `400` - Bad Request
- `401` - Unauthorized
- `403` - Forbidden
- `404` - Not Found
- `422` - Unprocessable Entity
- `500` - Internal Server Error

## Audit Log Agent API

### 1. Create Audit Log

**POST** `/api/v1/audit-logs`

Tạo một audit log mới.

#### Request Body
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "userId": "user123",
  "username": "john.doe",
  "action": "CREATE_USER",
  "resource": "users",
  "resourceId": "user456",
  "requestPath": "/api/v1/users",
  "status": "SUCCESS",
  "ipAddress": "192.168.1.100",
  "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
  "requestId": "req-12345",
  "additionalData": {
    "department": "IT",
    "role": "ADMIN"
  }
}
```

#### Response
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "timestamp": "2024-01-15T10:30:00Z",
    "userId": "user123",
    "username": "john.doe",
    "action": "CREATE_USER",
    "resource": "users",
    "resourceId": "user456",
    "requestPath": "/api/v1/users",
    "status": "SUCCESS",
    "ipAddress": "192.168.1.100",
    "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
    "requestId": "req-12345",
    "additionalData": {
      "department": "IT",
      "role": "ADMIN"
    },
    "createdAt": "2024-01-15T10:30:00Z"
  },
  "message": "Audit log created successfully",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 2. Batch Create Audit Logs

**POST** `/api/v1/audit-logs/batch`

Tạo nhiều audit logs cùng lúc.

#### Request Body
```json
{
  "auditLogs": [
    {
      "timestamp": "2024-01-15T10:30:00Z",
      "userId": "user123",
      "action": "LOGIN",
      "status": "SUCCESS"
    },
    {
      "timestamp": "2024-01-15T10:31:00Z",
      "userId": "user123",
      "action": "VIEW_PROFILE",
      "status": "SUCCESS"
    }
  ]
}
```

#### Response
```json
{
  "success": true,
  "data": {
    "createdCount": 2,
    "failedCount": 0,
    "auditLogs": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440001",
        "timestamp": "2024-01-15T10:30:00Z",
        "userId": "user123",
        "action": "LOGIN",
        "status": "SUCCESS"
      },
      {
        "id": "550e8400-e29b-41d4-a716-446655440002",
        "timestamp": "2024-01-15T10:31:00Z",
        "userId": "user123",
        "action": "VIEW_PROFILE",
        "status": "SUCCESS"
      }
    ]
  },
  "message": "Batch audit logs created successfully",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## Audit Log Query API

### 1. Get Audit Log by ID

**GET** `/api/v1/audit-logs/{id}`

Lấy thông tin chi tiết của một audit log.

#### Path Parameters
- `id` (string, required): ID của audit log

#### Response
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "timestamp": "2024-01-15T10:30:00Z",
    "userId": "user123",
    "username": "john.doe",
    "action": "CREATE_USER",
    "resource": "users",
    "resourceId": "user456",
    "requestPath": "/api/v1/users",
    "status": "SUCCESS",
    "ipAddress": "192.168.1.100",
    "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
    "requestId": "req-12345",
    "additionalData": {
      "department": "IT",
      "role": "ADMIN"
    },
    "createdAt": "2024-01-15T10:30:00Z"
  },
  "message": "Audit log retrieved successfully",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 2. Search Audit Logs

**GET** `/api/v1/audit-logs/search`

Tìm kiếm audit logs với các filter.

#### Query Parameters
- `userId` (string, optional): Filter theo user ID
- `action` (string, optional): Filter theo action
- `resource` (string, optional): Filter theo resource
- `status` (string, optional): Filter theo status
- `startDate` (string, optional): Ngày bắt đầu (ISO 8601)
- `endDate` (string, optional): Ngày kết thúc (ISO 8601)
- `ipAddress` (string, optional): Filter theo IP address
- `page` (integer, optional): Số trang (default: 0)
- `size` (integer, optional): Kích thước trang (default: 20, max: 100)
- `sort` (string, optional): Sắp xếp (default: "timestamp,desc")

#### Example Request
```
GET /api/v1/audit-logs/search?userId=user123&action=LOGIN&startDate=2024-01-01T00:00:00Z&endDate=2024-01-31T23:59:59Z&page=0&size=10&sort=timestamp,desc
```

#### Response
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "timestamp": "2024-01-15T10:30:00Z",
        "userId": "user123",
        "username": "john.doe",
        "action": "LOGIN",
        "status": "SUCCESS",
        "ipAddress": "192.168.1.100"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10,
      "sort": {
        "sorted": true,
        "unsorted": false
      }
    },
    "totalElements": 1,
    "totalPages": 1,
    "last": true,
    "first": true,
    "numberOfElements": 1,
    "size": 10,
    "number": 0,
    "sort": {
      "sorted": true,
      "unsorted": false
    },
    "empty": false
  },
  "message": "Search completed successfully",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 3. Get Audit Log Statistics

**GET** `/api/v1/audit-logs/statistics`

Lấy thống kê audit logs.

#### Query Parameters
- `startDate` (string, required): Ngày bắt đầu (ISO 8601)
- `endDate` (string, required): Ngày kết thúc (ISO 8601)
- `groupBy` (string, optional): Nhóm theo (user, action, resource, status)

#### Example Request
```
GET /api/v1/audit-logs/statistics?startDate=2024-01-01T00:00:00Z&endDate=2024-01-31T23:59:59Z&groupBy=action
```

#### Response
```json
{
  "success": true,
  "data": {
    "totalLogs": 1500,
    "successfulLogs": 1450,
    "failedLogs": 50,
    "uniqueUsers": 45,
    "groupedData": [
      {
        "group": "LOGIN",
        "count": 500,
        "percentage": 33.33
      },
      {
        "group": "CREATE_USER",
        "count": 200,
        "percentage": 13.33
      },
      {
        "group": "UPDATE_USER",
        "count": 300,
        "percentage": 20.00
      }
    ],
    "timeSeries": [
      {
        "date": "2024-01-01",
        "count": 50
      },
      {
        "date": "2024-01-02",
        "count": 45
      }
    ]
  },
  "message": "Statistics retrieved successfully",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 4. Export Audit Logs

**GET** `/api/v1/audit-logs/export`

Export audit logs ra file.

#### Query Parameters
- `format` (string, required): Format export (csv, json, xml)
- `userId` (string, optional): Filter theo user ID
- `action` (string, optional): Filter theo action
- `startDate` (string, optional): Ngày bắt đầu
- `endDate` (string, optional): Ngày kết thúc

#### Example Request
```
GET /api/v1/audit-logs/export?format=csv&startDate=2024-01-01T00:00:00Z&endDate=2024-01-31T23:59:59Z
```

#### Response
File download với Content-Type tương ứng:
- CSV: `text/csv`
- JSON: `application/json`
- XML: `application/xml`

## Admin API

### 1. Get System Health

**GET** `/api/v1/admin/health`

Kiểm tra tình trạng hệ thống.

#### Response
```json
{
  "success": true,
  "data": {
    "status": "UP",
    "components": {
      "database": {
        "status": "UP",
        "details": {
          "connection": "healthy"
        }
      },
      "disk": {
        "status": "UP",
        "details": {
          "free": "50GB",
          "total": "100GB"
        }
      }
    },
    "timestamp": "2024-01-15T10:30:00Z"
  },
  "message": "System is healthy",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 2. Get System Metrics

**GET** `/api/v1/admin/metrics`

Lấy metrics của hệ thống.

#### Response
```json
{
  "success": true,
  "data": {
    "jvm": {
      "memory": {
        "used": "512MB",
        "max": "1GB",
        "free": "488MB"
      },
      "threads": {
        "active": 25,
        "peak": 30
      }
    },
    "database": {
      "connections": {
        "active": 5,
        "max": 20,
        "idle": 15
      },
      "queries": {
        "total": 1500,
        "slow": 5
      }
    },
    "application": {
      "requests": {
        "total": 1000,
        "successful": 980,
        "failed": 20
      },
      "responseTime": {
        "average": "150ms",
        "p95": "300ms",
        "p99": "500ms"
      }
    }
  },
  "message": "Metrics retrieved successfully",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 3. Purge Old Audit Logs

**DELETE** `/api/v1/admin/audit-logs/purge`

Xóa audit logs cũ.

#### Request Body
```json
{
  "olderThan": "90d",
  "dryRun": false
}
```

#### Response
```json
{
  "success": true,
  "data": {
    "deletedCount": 15000,
    "dryRun": false,
    "olderThan": "90d"
  },
  "message": "Old audit logs purged successfully",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## Data Models

### AuditLogDto
```json
{
  "id": "string (UUID)",
  "timestamp": "string (ISO 8601)",
  "userId": "string (required)",
  "username": "string (optional)",
  "action": "string (required)",
  "resource": "string (optional)",
  "resourceId": "string (optional)",
  "requestPath": "string (optional)",
  "status": "string (optional)",
  "ipAddress": "string (optional)",
  "userAgent": "string (optional)",
  "requestId": "string (optional)",
  "additionalData": "object (optional)",
  "createdAt": "string (ISO 8601)"
}
```

### SearchCriteria
```json
{
  "userId": "string (optional)",
  "action": "string (optional)",
  "resource": "string (optional)",
  "status": "string (optional)",
  "startDate": "string (ISO 8601, optional)",
  "endDate": "string (ISO 8601, optional)",
  "ipAddress": "string (optional)",
  "page": "integer (optional, default: 0)",
  "size": "integer (optional, default: 20, max: 100)",
  "sort": "string (optional, default: 'timestamp,desc')"
}
```

## Error Codes

| Code | Description |
|------|-------------|
| `VALIDATION_ERROR` | Input validation failed |
| `AUTHENTICATION_ERROR` | Authentication failed |
| `AUTHORIZATION_ERROR` | Authorization failed |
| `RESOURCE_NOT_FOUND` | Resource not found |
| `DUPLICATE_RESOURCE` | Resource already exists |
| `DATABASE_ERROR` | Database operation failed |
| `INTERNAL_ERROR` | Internal server error |

## Rate Limiting

- **Standard API**: 1000 requests per minute
- **Admin API**: 100 requests per minute
- **Export API**: 10 requests per minute

## SDK Examples

### Java SDK
```java
AuditLogClient client = new AuditLogClient("https://audit.example.com", "your-api-key");

// Create audit log
AuditLogDto auditLog = AuditLogDto.builder()
    .userId("user123")
    .action("LOGIN")
    .status("SUCCESS")
    .timestamp(Instant.now())
    .build();

AuditLogDto created = client.createAuditLog(auditLog);

// Search audit logs
SearchCriteria criteria = SearchCriteria.builder()
    .userId("user123")
    .startDate(Instant.now().minus(Duration.ofDays(7)))
    .endDate(Instant.now())
    .build();

PageResponse<AuditLogDto> results = client.searchAuditLogs(criteria);
```

### JavaScript SDK
```javascript
const client = new AuditLogClient('https://audit.example.com', 'your-api-key');

// Create audit log
const auditLog = {
  userId: 'user123',
  action: 'LOGIN',
  status: 'SUCCESS',
  timestamp: new Date().toISOString()
};

const created = await client.createAuditLog(auditLog);

// Search audit logs
const criteria = {
  userId: 'user123',
  startDate: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString(),
  endDate: new Date().toISOString()
};

const results = await client.searchAuditLogs(criteria);
```

## Webhooks

### Audit Log Created Webhook
```json
{
  "event": "audit_log.created",
  "timestamp": "2024-01-15T10:30:00Z",
  "data": {
    "auditLog": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "userId": "user123",
      "action": "CREATE_USER",
      "status": "SUCCESS"
    }
  }
}
```

### Webhook Configuration
```json
{
  "url": "https://your-app.com/webhooks/audit-logs",
  "events": ["audit_log.created", "audit_log.updated"],
  "secret": "your-webhook-secret"
}
``` 