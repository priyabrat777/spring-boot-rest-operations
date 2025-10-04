# WireMock Integration Testing Setup

This directory contains WireMock-based integration tests for external service testing. WireMock allows us to mock external HTTP services and test various scenarios including success, failure, and edge cases.

## Overview

WireMock is used to mock external services that our application integrates with:

1. **OTP Delivery Services** - Email, SMS, Voice, and Push notification services
2. **File Storage Services** - Cloud storage APIs for file upload, download, and management
3. **External API Dependencies** - Any third-party services our application depends on

## Project Structure

```
src/test/java/com/enterprise/api/wiremock/
├── README.md                           # This documentation
├── WireMockConfig.java                 # WireMock configuration for Spring tests
├── WireMockTestBase.java              # Base class for WireMock integration tests
├── WireMockBasicTest.java             # Basic WireMock functionality tests
├── OtpDeliveryWireMockTest.java       # OTP service integration tests
├── FileStorageWireMockTest.java       # File storage service integration tests
├── ExternalServiceFailureTest.java    # External service failure scenario tests
├── WireMockIntegrationTest.java       # Comprehensive integration tests
└── stubs/
    ├── OtpDeliveryStubs.java          # OTP service mock stubs
    └── FileStorageStubs.java          # File storage service mock stubs

src/test/resources/wiremock/
└── mappings/                          # Predefined WireMock mappings
    ├── email-service-success.json
    ├── sms-service-success.json
    └── file-storage-upload-success.json
```

## Key Components

### 1. WireMockConfig.java
Spring Test Configuration that provides a WireMockServer bean for integration tests.

```java
@TestConfiguration
public class WireMockConfig {
    public static final int WIREMOCK_PORT = 8089;
    
    @Bean
    public WireMockServer wireMockServer() {
        return new WireMockServer(
            WireMockConfiguration.options().port(WIREMOCK_PORT)
        );
    }
}
```

### 2. WireMockTestBase.java
Abstract base class that handles WireMock server lifecycle and provides common setup.

```java
@SpringBootTest
@ContextConfiguration(classes = WireMockConfig.class)
public abstract class WireMockTestBase {
    @Autowired
    protected WireMockServer wireMockServer;
    
    @BeforeEach
    void setUp() {
        if (!wireMockServer.isRunning()) {
            wireMockServer.start();
        }
        wireMockServer.resetAll();
        setupWireMockStubs();
    }
    
    protected abstract void setupWireMockStubs();
}
```

### 3. Stub Classes
Utility classes that provide reusable mock configurations for different services.

## Test Scenarios Covered

### OTP Delivery Service Tests
- ✅ Successful email delivery
- ✅ Successful SMS delivery  
- ✅ Successful voice delivery
- ✅ Successful push notification delivery
- ✅ Email delivery failure
- ✅ SMS delivery failure
- ✅ Voice delivery failure
- ✅ Push notification failure
- ✅ Rate limiting scenarios
- ✅ Timeout handling
- ✅ Request verification and counting

### File Storage Service Tests
- ✅ Successful file upload
- ✅ Successful file download
- ✅ Successful file deletion
- ✅ File upload size limit exceeded
- ✅ Invalid file type rejection
- ✅ Storage quota exceeded
- ✅ File not found scenarios
- ✅ Access denied scenarios
- ✅ Service unavailability
- ✅ Timeout handling
- ✅ Request tracking and verification

### External Service Failure Tests
- ✅ Network connection failures
- ✅ Service unavailable (503) errors
- ✅ Internal server errors (500)
- ✅ Timeout scenarios
- ✅ Bad gateway (502) errors
- ✅ Authentication failures (401)
- ✅ Rate limiting (429) with retry-after
- ✅ Invalid response format handling
- ✅ Partial service failures
- ✅ Circuit breaker patterns
- ✅ Slow response time handling

## Usage Examples

### Basic Stub Creation

```java
@Test
void shouldMockEmailService() {
    // Setup stub
    wireMockServer.stubFor(post(urlPathEqualTo("/api/email/send"))
        .withHeader("Content-Type", containing("application/json"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("""
                {
                    "messageId": "email-123456",
                    "status": "sent",
                    "deliveryTime": 1500
                }
                """)));
    
    // Test your service that calls the external API
    OtpGenerationResponse response = otpService.generateOtp(request, "127.0.0.1", "Test-Agent");
    
    // Verify the external service was called
    wireMockServer.verify(postRequestedFor(urlPathEqualTo("/api/email/send"))
        .withHeader("Content-Type", containing("application/json")));
}
```

### Failure Scenario Testing

```java
@Test
void shouldHandleServiceUnavailable() {
    // Setup failure stub
    wireMockServer.stubFor(any(urlMatching("/api/.*"))
        .willReturn(aResponse()
            .withStatus(503)
            .withHeader("Content-Type", "application/json")
            .withBody("""
                {
                    "error": "SERVICE_UNAVAILABLE",
                    "message": "Service is temporarily unavailable",
                    "retryAfter": 300
                }
                """)));
    
    // Test that your service handles the failure gracefully
    OtpGenerationResponse response = otpService.generateOtp(request, "127.0.0.1", "Test-Agent");
    assertThat(response.isDelivered()).isFalse();
}
```

### Advanced Scenarios

```java
@Test
void shouldHandleRetryScenario() {
    // Setup scenario-based stubs
    wireMockServer.stubFor(post(urlPathEqualTo("/api/email/send"))
        .inScenario("Retry Scenario")
        .whenScenarioStateIs("Started")
        .willReturn(aResponse().withStatus(500))
        .willSetStateTo("First Retry"));
        
    wireMockServer.stubFor(post(urlPathEqualTo("/api/email/send"))
        .inScenario("Retry Scenario")
        .whenScenarioStateIs("First Retry")
        .willReturn(aResponse().withStatus(200)));
}
```

## Running the Tests

### Run all WireMock tests:
```bash
mvn test -Dtest="*WireMock*"
```

### Run specific test class:
```bash
mvn test -Dtest="OtpDeliveryWireMockTest"
```

### Run with specific profile:
```bash
mvn test -Dtest="*WireMock*" -Dspring.profiles.active=test
```

## Configuration

### Application Properties for Testing
```yaml
# src/test/resources/application-test.yml
app:
  external:
    otp:
      email:
        url: http://localhost:8089
      sms:
        url: http://localhost:8089
      voice:
        url: http://localhost:8089
      push:
        url: http://localhost:8089
    file:
      storage:
        url: http://localhost:8089
```

### WireMock Server Configuration
- **Port**: 8089 (configurable in WireMockConfig.WIREMOCK_PORT)
- **Reset**: Server is reset before each test
- **Lifecycle**: Managed by Spring Test Context

## Best Practices

### 1. Test Organization
- Use descriptive test method names that clearly indicate the scenario
- Group related tests in the same test class
- Use setup methods to configure common stubs

### 2. Stub Management
- Reset WireMock server before each test to avoid interference
- Use specific URL patterns to avoid stub conflicts
- Provide realistic response bodies and headers

### 3. Verification
- Always verify that external services were called as expected
- Check request parameters, headers, and body content
- Verify the number of calls made to external services

### 4. Error Handling
- Test both success and failure scenarios
- Include edge cases like timeouts, rate limiting, and service unavailability
- Verify that your application handles failures gracefully

### 5. Performance Testing
- Use delays to simulate slow external services
- Test timeout handling and circuit breaker patterns
- Verify that your application doesn't hang on slow responses

## Troubleshooting

### Common Issues

1. **Port Conflicts**: If port 8089 is in use, change WIREMOCK_PORT in WireMockConfig
2. **Stub Not Matching**: Check URL patterns, HTTP methods, and headers
3. **Context Loading Issues**: Ensure WireMockConfig is included in test configuration
4. **Verification Failures**: Check that stubs are set up before making service calls

### Debug Tips

1. **Enable WireMock Logging**:
   ```java
   WireMockServer wireMockServer = new WireMockServer(
       WireMockConfiguration.options()
           .port(8089)
           .withRootDirectory("src/test/resources/wiremock")
   );
   ```

2. **Check Stub Mappings**:
   ```java
   System.out.println("Active stubs: " + wireMockServer.getStubMappings().size());
   ```

3. **Verify Requests**:
   ```java
   List<LoggedRequest> requests = wireMockServer.findAll(anyRequestedFor(anyUrl()));
   requests.forEach(request -> System.out.println(request.getUrl()));
   ```

## Integration with CI/CD

These tests are designed to run in CI/CD pipelines:

- **No External Dependencies**: All external services are mocked
- **Fast Execution**: Tests run quickly without network calls
- **Deterministic**: Results are consistent across environments
- **Isolated**: Each test is independent and can run in parallel

## Requirements Addressed

This WireMock setup addresses the following requirements:

- **10.3**: External service mocking for comprehensive testing
- **10.4**: Edge case and failure scenario testing
- **2.1**: File upload and management system testing
- **2.2-2.5**: OTP delivery service testing
- **4.4-4.5**: Error handling and response validation

## Future Enhancements

Potential improvements to the WireMock setup:

1. **Dynamic Port Allocation**: Use random ports to avoid conflicts
2. **Request Recording**: Record real API calls for stub generation
3. **Performance Metrics**: Add response time tracking and analysis
4. **Contract Testing**: Integrate with Pact or similar tools
5. **Chaos Engineering**: Add random failures and network issues