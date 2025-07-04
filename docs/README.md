# Tài liệu dự án Audit Logging System

## Tổng quan

Hệ thống Audit Logging System là một giải pháp microservice được thiết kế để ghi lại và quản lý các hoạt động audit trong các ứng dụng. Hệ thống bao gồm 4 module chính:

- **audit-log-agent**: Module thu thập và gửi audit logs
- **audit-log-worker**: Module xử lý và lưu trữ audit logs
- **audit-log-query**: Module truy vấn và hiển thị audit logs
- **audit-log-common**: Module chứa các thành phần dùng chung

## Cấu trúc tài liệu

### 📋 Tài liệu kiến trúc
- [Kiến trúc hệ thống](./architecture/system-architecture.md)
- [Thiết kế cơ sở dữ liệu](./architecture/database-design.md)
- [Sơ đồ luồng dữ liệu](./architecture/data-flow.md)

### 🔧 Tài liệu phát triển
- [Hướng dẫn phát triển](./development/development-guide.md)
- [Quy ước code](./development/coding-standards.md)
- [API Reference](./development/api-reference.md)

### 🚀 Tài liệu triển khai
- [Hướng dẫn triển khai](./deployment/deployment-guide.md)
- [Cấu hình Docker](./deployment/docker-configuration.md)
- [Cấu hình môi trường](./deployment/environment-configuration.md)

### 🔍 Tài liệu vận hành
- [Hướng dẫn vận hành](./operations/operations-guide.md)
- [Monitoring và Logging](./operations/monitoring.md)
- [Troubleshooting](./operations/troubleshooting.md)

### 🛡️ Tài liệu bảo mật
- [Chính sách bảo mật](./security/security-policy.md)
- [Hướng dẫn bảo mật](./security/security-guide.md)

### 🧪 Tài liệu kiểm thử
- [Chiến lược kiểm thử](./testing/testing-strategy.md)
- [Hướng dẫn kiểm thử](./testing/testing-guide.md)

## Công nghệ sử dụng

- **Backend**: Java 17, Spring Boot 3.5.3
- **Container**: Docker, Docker Compose
- **Build Tool**: Maven
- **Architecture**: Microservices

## Liên hệ

Để biết thêm thông tin về dự án, vui lòng liên hệ team phát triển. 