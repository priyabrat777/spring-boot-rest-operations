# Enterprise Spring Boot API

Welcome to the Enterprise Spring Boot API project - a comprehensive workspace demonstrating enterprise-level REST API development with Spring Boot 3.x and Java 21.

## Overview

This project showcases modern Java development practices, including:

- **Complete REST API Operations**: All HTTP methods (GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD)
- **Enterprise Security**: JWT-based authentication with RBAC authorization
- **Data Management**: Spring Data JPA with comprehensive audit trails
- **Batch Processing**: Spring Batch for large-scale data operations
- **Advanced Features**: File upload, CAPTCHA validation, OTP functionality
- **Quality Assurance**: 100% test coverage with unit and integration tests
- **Documentation**: OpenAPI 3.0 specification with Swagger UI

## Key Features

### 🔐 Security
- JWT token-based authentication
- Role-based access control (RBAC)
- Password encoding and validation
- Session management

### 📊 Data Management
- Comprehensive audit trails
- Soft delete functionality
- Optimistic locking
- Custom repository patterns

### 🚀 Performance
- Connection pooling with HikariCP
- Caching with Spring Cache
- Pagination support
- Query optimization

### 🧪 Testing
- Unit tests with 100% coverage
- Integration tests with H2 database
- WireMock for external service testing
- Performance benchmarks

### 📈 Monitoring
- Spring Boot Actuator endpoints
- Custom health indicators
- Application metrics
- Structured logging

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.8.0 or higher
- Git

### Quick Start

```bash
# Clone the repository
git clone https://github.com/enterprise/spring-boot-api.git
cd spring-boot-api

# Build the project
mvn clean compile

# Run tests
mvn test

# Start the application
mvn spring-boot:run
```

### API Documentation

Once the application is running, you can access:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/v3/api-docs
- **Actuator Health**: http://localhost:8080/actuator/health

## Architecture

The application follows a layered architecture with clear separation of concerns:

```
┌─────────────────┐
│   Controllers   │  ← REST API endpoints
├─────────────────┤
│    Services     │  ← Business logic
├─────────────────┤
│  Repositories   │  ← Data access layer
├─────────────────┤
│    Entities     │  ← JPA entities
└─────────────────┘
```

## Build Profiles

The project includes several Maven profiles for different environments:

- **dev** (default): Development environment with relaxed settings
- **test**: Test environment with full validation
- **prod**: Production environment with optimizations
- **ci**: Continuous integration with strict quality gates
- **release**: Release preparation with documentation generation

## Quality Metrics

This project maintains high quality standards:

- **Test Coverage**: 100% line coverage, 95% branch coverage
- **Code Quality**: Checkstyle, PMD, and SpotBugs validation
- **Security**: OWASP dependency checks
- **Performance**: Load testing and benchmarks

## Contributing

Please read our [Developer Guide](developer-guide.html) for details on our code of conduct and the process for submitting pull requests.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](https://www.apache.org/licenses/LICENSE-2.0) file for details.

## Support

For support and questions:

- **Documentation**: [User Guide](user-guide.html)
- **Issues**: [GitHub Issues](https://github.com/enterprise/spring-boot-api/issues)
- **Email**: dev@enterprise.com