package com.enterprise.api.wiremock;

import com.enterprise.api.dto.request.FileUploadRequest;
import com.enterprise.api.dto.request.OtpGenerationRequest;
import com.enterprise.api.dto.response.FileUploadResponse;
import com.enterprise.api.dto.response.OtpGenerationResponse;
import com.enterprise.api.entity.OtpPurpose;
import com.enterprise.api.entity.OtpType;
import com.enterprise.api.entity.User;
import com.enterprise.api.repository.UserRepository;
import com.enterprise.api.service.FileService;
import com.enterprise.api.service.OtpService;
import com.enterprise.api.wiremock.stubs.FileStorageStubs;
import com.enterprise.api.wiremock.stubs.OtpDeliveryStubs;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive integration test demonstrating WireMock capabilities for external service testing.
 * This test showcases various scenarios including success, failure, and edge cases.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WireMockIntegrationTest extends WireMockTestBase {

    @Autowired
    private OtpService otpService;

    @Autowired
    private FileService fileService;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void setupWireMockStubs() {
        // Setup default successful responses
        OtpDeliveryStubs.setupEmailDeliverySuccess(wireMockServer);
        OtpDeliveryStubs.setupSmsDeliverySuccess(wireMockServer);
        FileStorageStubs.setupFileUploadSuccess(wireMockServer);
        FileStorageStubs.setupFileDownloadSuccess(wireMockServer);
    }

    @Test
    void shouldDemonstrateCompleteOtpWorkflowWithExternalServices() {
        // Given
        OtpGenerationRequest emailRequest = new OtpGenerationRequest(
            "user@example.com",
            OtpType.EMAIL,
            OtpPurpose.LOGIN,
            5,
            "Your secure login code"
        );

        OtpGenerationRequest smsRequest = new OtpGenerationRequest(
            "+1234567890",
            OtpType.SMS,
            OtpPurpose.PASSWORD_RESET,
            10,
            null
        );

        // When
        OtpGenerationResponse emailResponse = otpService.generateOtp(emailRequest, "192.168.1.100", "Mozilla/5.0");
        OtpGenerationResponse smsResponse = otpService.generateOtp(smsRequest, "192.168.1.100", "Mozilla/5.0");

        // Then
        assertThat(emailResponse.isDelivered()).isTrue();
        assertThat(emailResponse.getType()).isEqualTo(OtpType.EMAIL);
        assertThat(emailResponse.getPurpose()).isEqualTo(OtpPurpose.LOGIN);

        assertThat(smsResponse.isDelivered()).isTrue();
        assertThat(smsResponse.getType()).isEqualTo(OtpType.SMS);
        assertThat(smsResponse.getPurpose()).isEqualTo(OtpPurpose.PASSWORD_RESET);

        // Verify external service calls
        OtpDeliveryStubs.verifyEmailDeliveryCall(wireMockServer, "user@example.com", "Your secure login code");
        OtpDeliveryStubs.verifySmsDeliveryCall(wireMockServer, "+1234567890");

        // Verify call counts
        assertThat(OtpDeliveryStubs.getRequestCount(wireMockServer, "/api/email/send")).isEqualTo(1);
        assertThat(OtpDeliveryStubs.getRequestCount(wireMockServer, "/api/sms/send")).isEqualTo(1);
    }

    @Test
    void shouldDemonstrateCompleteFileWorkflowWithExternalServices() {
        // Given
        MultipartFile file = new MockMultipartFile(
            "file",
            "integration-test.pdf",
            "application/pdf",
            "PDF content for integration test".getBytes()
        );

        FileUploadRequest uploadRequest = new FileUploadRequest(
            "Integration test document",
            true
        );

        User user = createTestUser();

        // When
        FileUploadResponse uploadResponse = fileService.uploadFile(file, uploadRequest, user);

        // Then
        assertThat(uploadResponse).isNotNull();
        assertThat(uploadResponse.getOriginalFileName()).isEqualTo("integration-test.pdf");
        assertThat(uploadResponse.getContentType()).isEqualTo("application/pdf");
        assertThat(uploadResponse.getCategory()).isNotNull();

        // Verify external service calls
        FileStorageStubs.verifyFileUploadCall(wireMockServer, "integration-test.pdf");

        // Verify call counts
        assertThat(FileStorageStubs.getUploadRequestCount(wireMockServer)).isEqualTo(1);
    }

    @Test
    void shouldDemonstrateFailoverScenario() {
        // Given - Primary email service fails, fallback to SMS
        wireMockServer.resetAll();
        
        // Setup email service failure
        wireMockServer.stubFor(post(urlPathEqualTo("/api/email/send"))
            .willReturn(aResponse()
                .withStatus(503)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "error": "SERVICE_UNAVAILABLE",
                        "message": "Email service is down"
                    }
                    """)));

        // Setup SMS service success
        OtpDeliveryStubs.setupSmsDeliverySuccess(wireMockServer);

        OtpGenerationRequest emailRequest = new OtpGenerationRequest(
            "user@example.com",
            OtpType.EMAIL,
            OtpPurpose.LOGIN,
            5,
            null
        );

        OtpGenerationRequest smsRequest = new OtpGenerationRequest(
            "+1234567890",
            OtpType.SMS,
            OtpPurpose.LOGIN,
            5,
            null
        );

        // When
        OtpGenerationResponse emailResponse = otpService.generateOtp(emailRequest, "127.0.0.1", "Test-Agent");
        OtpGenerationResponse smsResponse = otpService.generateOtp(smsRequest, "127.0.0.1", "Test-Agent");

        // Then
        assertThat(emailResponse.isDelivered()).isFalse(); // Email failed
        assertThat(smsResponse.isDelivered()).isTrue();    // SMS succeeded

        // Verify both services were called
        wireMockServer.verify(postRequestedFor(urlPathEqualTo("/api/email/send")));
        wireMockServer.verify(postRequestedFor(urlPathEqualTo("/api/sms/send")));
    }

    @Test
    void shouldDemonstrateLoadBalancingScenario() {
        // Given - Multiple service instances with different response times
        wireMockServer.resetAll();
        
        // Fast email service
        wireMockServer.stubFor(post(urlPathEqualTo("/api/email/send"))
            .withHeader("X-Service-Instance", equalTo("instance-1"))
            .willReturn(aResponse()
                .withStatus(200)
                .withFixedDelay(100)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "messageId": "fast-email-123",
                        "status": "sent",
                        "instance": "instance-1",
                        "responseTime": 100
                    }
                    """)));

        // Slow email service
        wireMockServer.stubFor(post(urlPathEqualTo("/api/email/send"))
            .withHeader("X-Service-Instance", equalTo("instance-2"))
            .willReturn(aResponse()
                .withStatus(200)
                .withFixedDelay(2000)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "messageId": "slow-email-456",
                        "status": "sent",
                        "instance": "instance-2",
                        "responseTime": 2000
                    }
                    """)));

        OtpGenerationRequest request = new OtpGenerationRequest(
            "user@example.com",
            OtpType.EMAIL,
            OtpPurpose.LOGIN,
            5,
            null
        );

        // When - Simulate load balancer choosing fast instance
        long startTime = System.currentTimeMillis();
        OtpGenerationResponse response = otpService.generateOtp(request, "127.0.0.1", "Test-Agent");
        long endTime = System.currentTimeMillis();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo(OtpType.EMAIL);
        
        long responseTime = endTime - startTime;
        // Response time should be closer to fast instance (100ms) than slow instance (2000ms)
        assertThat(responseTime).isLessThan(1500);
    }

    @Test
    void shouldDemonstrateRetryMechanism() {
        // Given - Service fails first two times, succeeds on third attempt
        wireMockServer.resetAll();
        
        wireMockServer.stubFor(post(urlPathEqualTo("/api/email/send"))
            .inScenario("Retry Scenario")
            .whenScenarioStateIs("Started")
            .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\": \"TEMPORARY_ERROR\"}"))
            .willSetStateTo("First Retry"));

        wireMockServer.stubFor(post(urlPathEqualTo("/api/email/send"))
            .inScenario("Retry Scenario")
            .whenScenarioStateIs("First Retry")
            .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\": \"TEMPORARY_ERROR\"}"))
            .willSetStateTo("Second Retry"));

        wireMockServer.stubFor(post(urlPathEqualTo("/api/email/send"))
            .inScenario("Retry Scenario")
            .whenScenarioStateIs("Second Retry")
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "messageId": "retry-success-123",
                        "status": "sent",
                        "attempt": 3
                    }
                    """)));

        OtpGenerationRequest request = new OtpGenerationRequest(
            "user@example.com",
            OtpType.EMAIL,
            OtpPurpose.LOGIN,
            5,
            null
        );

        // When - Make multiple requests to trigger retry scenario
        OtpGenerationResponse response1 = otpService.generateOtp(request, "127.0.0.1", "Test-Agent");
        OtpGenerationResponse response2 = otpService.generateOtp(request, "127.0.0.1", "Test-Agent");
        OtpGenerationResponse response3 = otpService.generateOtp(request, "127.0.0.1", "Test-Agent");

        // Then
        assertThat(response1.isDelivered()).isFalse(); // First attempt failed
        assertThat(response2.isDelivered()).isFalse(); // Second attempt failed
        assertThat(response3.isDelivered()).isTrue();  // Third attempt succeeded

        // Verify all three calls were made
        wireMockServer.verify(3, postRequestedFor(urlPathEqualTo("/api/email/send")));
    }

    @Test
    void shouldDemonstrateRequestResponseLogging() {
        // Given
        wireMockServer.resetAll();
        
        wireMockServer.stubFor(post(urlPathEqualTo("/api/email/send"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withHeader("X-Request-ID", "{{request.headers.X-Request-ID}}")
                .withHeader("X-Correlation-ID", "correlation-{{randomValue type='UUID'}}")
                .withBody("""
                    {
                        "messageId": "logged-email-{{randomValue type='NUMERIC' length=6}}",
                        "status": "sent",
                        "requestId": "{{request.headers.X-Request-ID}}",
                        "timestamp": "{{now format='yyyy-MM-dd HH:mm:ss'}}",
                        "processingTime": "{{randomValue type='NUMERIC' lower=50 upper=500}}"
                    }
                    """)
                .withTransformers("response-template")));

        OtpGenerationRequest request = new OtpGenerationRequest(
            "user@example.com",
            OtpType.EMAIL,
            OtpPurpose.LOGIN,
            5,
            null
        );

        // When
        OtpGenerationResponse response = otpService.generateOtp(request, "127.0.0.1", "Test-Agent");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo(OtpType.EMAIL);

        // Verify request was logged with proper headers
        wireMockServer.verify(postRequestedFor(urlPathEqualTo("/api/email/send"))
            .withHeader("Content-Type", containing("application/json")));
    }

    @Test
    void shouldDemonstrateConditionalStubbing() {
        // Given - Different responses based on request content
        wireMockServer.resetAll();
        
        // VIP user gets priority delivery
        wireMockServer.stubFor(post(urlPathEqualTo("/api/email/send"))
            .withRequestBody(containing("vip@example.com"))
            .willReturn(aResponse()
                .withStatus(200)
                .withFixedDelay(50) // Faster delivery
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "messageId": "vip-email-123",
                        "status": "sent",
                        "priority": "high",
                        "deliveryTime": 50
                    }
                    """)));

        // Regular user gets standard delivery
        wireMockServer.stubFor(post(urlPathEqualTo("/api/email/send"))
            .withRequestBody(containing("regular@example.com"))
            .willReturn(aResponse()
                .withStatus(200)
                .withFixedDelay(500) // Standard delivery
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "messageId": "regular-email-456",
                        "status": "sent",
                        "priority": "normal",
                        "deliveryTime": 500
                    }
                    """)));

        OtpGenerationRequest vipRequest = new OtpGenerationRequest(
            "vip@example.com",
            OtpType.EMAIL,
            OtpPurpose.LOGIN,
            5,
            null
        );

        OtpGenerationRequest regularRequest = new OtpGenerationRequest(
            "regular@example.com",
            OtpType.EMAIL,
            OtpPurpose.LOGIN,
            5,
            null
        );

        // When
        long vipStart = System.currentTimeMillis();
        OtpGenerationResponse vipResponse = otpService.generateOtp(vipRequest, "127.0.0.1", "Test-Agent");
        long vipEnd = System.currentTimeMillis();

        long regularStart = System.currentTimeMillis();
        OtpGenerationResponse regularResponse = otpService.generateOtp(regularRequest, "127.0.0.1", "Test-Agent");
        long regularEnd = System.currentTimeMillis();

        // Then
        assertThat(vipResponse.isDelivered()).isTrue();
        assertThat(regularResponse.isDelivered()).isTrue();

        // VIP should be faster than regular
        long vipTime = vipEnd - vipStart;
        long regularTime = regularEnd - regularStart;
        assertThat(vipTime).isLessThan(regularTime);
    }

    // Helper methods

    private User createTestUser() {
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("encoded-password");
        // Save to database to avoid foreign key constraint violations
        return userRepository.save(user);
    }
}