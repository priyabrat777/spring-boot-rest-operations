# Enterprise Spring Boot API

A comprehensive Spring Boot workspace demonstrating enterprise-level REST API development with complete CRUD operations, security, auditing, batch processing, and comprehensive testing.

## Features

- **Complete REST API Operations**: GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD
- **JWT-based Security**: Authentication and Role-Based Access Control (RBAC)
- **Comprehensive Auditing**: Full audit trail for all data operations
- **Spring Data JPA**: Advanced repository patterns and custom queries
- **Spring Batch**: Scheduled and manual batch processing
- **File Management**: Upload, download, and metadata tracking
- **CAPTCHA & OTP**: Security features for enhanced authentication
- **OpenAPI Documentation**: Comprehensive API documentation with Swagger UI
- **Extensive Testing**: Unit tests, integration tests, and performance tests
- **CI/CD Pipeline**: Jenkins-based automated build and deployment

## Technology Stack

- **Java 21**: Latest LTS version with modern language features
- **Spring Boot 3.2.0**: Latest Spring Boot with Spring Framework 6
- **Spring Security**: JWT authentication and RBAC authorization
- **Spring Data JPA**: Data access with Hibernate
- **Spring Batch**: Batch processing framework
- **H2 Database**: In-memory database for development and testing
- **MySQL**: Production database
- **Maven**: Build and dependency management
- **JUnit 5**: Testing framework
- **Testcontainers**: Integration testing with real databases
- **WireMock**: External service mocking
- **OpenAPI 3**: API documentation

## Quick Start

### Prerequisites

- Java 21 or higher
- Maven 3.8 or higher
- MySQL 8.0 (for production)

### Running the Application

1. **Development Mode** (uses H2 in-memory database):
   ```bash
   mvn spring-boot:run
   ```

2. **With specific profile**:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

3. **Build and run JAR**:
   ```bash
   mvn clean package
   java -jar target/spring-boot-api-1.0.0-SNAPSHOT.jar
   ```

### Accessing the Application

- **Application**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **H2 Console** (dev profile): http://localhost:8080/h2-console
- **Actuator Health**: http://localhost:8080/actuator/health

## Configuration

The application supports multiple environments through Spring profiles:

- **dev**: Development environment with H2 database and debug logging
- **test**: Test environment optimized for fast test execution
- **prod**: Production environment with MySQL and security hardening

### Environment Variables

Key environment variables for production:

```bash
# Database
DATABASE_URL=jdbc:mysql://localhost:3306/enterprise_api
DATABASE_USERNAME=api_user
DATABASE_PASSWORD=secure_password

# JWT Security
JWT_SECRET=your-256-bit-secret-key
JWT_EXPIRATION=3600000

# File Upload
FILE_UPLOAD_DIR=/var/app/uploads
FILE_MAX_SIZE=5242880

# CORS
CORS_ALLOWED_ORIGINS=https://yourdomain.com
```

## API Documentation

The application provides comprehensive API documentation through OpenAPI 3.0:

- **Interactive Documentation**: Available at `/swagger-ui.html`
- **OpenAPI Spec**: Available at `/api-docs`
- **Postman Collection**: Can be generated from the OpenAPI specification

## Testing

### Running Tests

```bash
# Run all tests
mvn test

# Run integration tests
mvn verify

# Run with coverage report
mvn clean test jacoco:report
```

### Test Coverage

The project maintains high test coverage standards:
- Service layer: 100% line coverage
- Controller layer: 95% coverage
- Repository layer: 90% coverage

## Build and Release

### Maven Profiles

- **dev**: Default development profile
- **test**: Test execution profile
- **prod**: Production build profile
- **release**: Release profile with source and javadoc generation

### Release Management

```bash
# Prepare release
mvn release:prepare

# Perform release
mvn release:perform

# Create release build
mvn clean package -Prelease
```

## Security

The application implements enterprise-grade security features:

- **JWT Authentication**: Stateless authentication with refresh tokens
- **RBAC Authorization**: Role-based access control with fine-grained permissions
- **Input Validation**: Comprehensive validation for all API inputs
- **CORS Configuration**: Configurable cross-origin resource sharing
- **Security Headers**: Standard security headers for web protection

## Monitoring

### Health Checks

The application provides comprehensive health monitoring:

- **Application Health**: Basic application status
- **Database Health**: Database connectivity status
- **Custom Health Indicators**: Business-specific health checks

### Metrics

Available through Spring Boot Actuator:

- **JVM Metrics**: Memory, CPU, garbage collection
- **Application Metrics**: Custom business metrics
- **HTTP Metrics**: Request/response statistics

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

For support and questions:

- Create an issue in the GitHub repository
- Check the [Wiki](../../wiki) for detailed documentation
- Review the API documentation at `/swagger-ui.html`