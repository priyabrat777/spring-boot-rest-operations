package com.enterprise.api.wiremock;

import com.enterprise.api.dto.request.OtpGenerationRequest;
import com.enterprise.api.dto.response.OtpGenerationResponse;
import com.enterprise.api.entity.OtpPurpose;
import com.enterprise.api.entity.OtpType;
import com.enterprise.api.service.OtpService;
import com.enterprise.api.wiremock.stubs.FileStorageStubs;
import com.enterprise.api.wiremock.stubs.OtpDeliveryStubs;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for external service failure scenarios using WireMock.
 * Tests various failure modes including network errors, timeouts, and service unavailability.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ExternalServiceFailureTest extends WireMockTestBase {

    @Autowired
    private OtpService otpService;

    @Override
    protected void setupWireMockStubs() {
        // No default setup - each test will configure its own stubs
    }

    @Test
    void shouldHandleNetworkConnectionFailure() {
        // Given - No stubs configured, so requests will fail with connection refused
        wireMockServer.stop(); // Stop the server to simulate network failure
        
        OtpGenerationRequest request = new OtpGenerationRequest(
            "test@example.com",
            OtpType.EMAIL,
            OtpPurpose.LOGIN,
            5,
            null
        );

        // When
        OtpGenerationResponse response = otpService.generateOtp(request, "127.0.0.1", "Test-Agent");

        // Then - Service should handle network failure gracefully
        assertThat(response).isNotNull();
        assertThat(response.isDelivered()).isFalse();
        assertThat(response.getType()).isEqualTo(OtpType.EMAIL);
        
        // Restart server for other tests
        wireMockServer.start();
    }

    @Test
    void shouldHandleServiceUnavailableError() {
        // Given
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
        
        OtpGenerationRequest request = new OtpGenerationRequest(
            "test@example.com",
            OtpType.EMAIL,
            OtpPurpose.LOGIN,
            5,
            null
        );

        // When
        OtpGenerationResponse response = otpService.generateOtp(request, "127.0.0.1", "Test-Agent");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isDelivered()).isFalse();
        assertThat(response.getType()).isEqualTo(OtpType.EMAIL);
    }

    @Test
    void shouldHandleInternalServerError() {
        // Given
        wireMockServer.stubFor(any(urlMatching("/api/.*"))
            .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "error": "INTERNAL_SERVER_ERROR",
                        "message": "An unexpected error occurred"
                    }
                    """)));
        
        OtpGenerationRequest request = new OtpGenerationRequest(
            "test@example.com",
            OtpType.SMS,
            OtpPurpose.PASSWORD_RESET,
            5,
            null
        );

        // When
        OtpGenerationResponse response = otpService.generateOtp(request, "127.0.0.1", "Test-Agent");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isDelivered()).isFalse();
        assertThat(response.getType()).isEqualTo(OtpType.SMS);
    }

    @Test
    void shouldHandleTimeoutError() {
        // Given
        wireMockServer.stubFor(any(urlMatching("/api/.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withFixedDelay(30000) // 30 second delay to simulate timeout
                .withHeader("Content-Type", "application/json")
                .withBody("{\"status\": \"timeout\"}")));
        
        OtpGenerationRequest request = new OtpGenerationRequest(
            "test@example.com",
            OtpType.VOICE,
            OtpPurpose.ACCOUNT_VERIFICATION,
            5,
            null
        );

        // When
        OtpGenerationResponse response = otpService.generateOtp(request, "127.0.0.1", "Test-Agent");

        // Then - Service should handle timeout gracefully
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo(OtpType.VOICE);
        // Note: Delivery status depends on timeout handling implementation
    }

    @Test
    void shouldHandleBadGatewayError() {
        // Given
        wireMockServer.stubFor(any(urlMatching("/api/.*"))
            .willReturn(aResponse()
                .withStatus(502)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "error": "BAD_GATEWAY",
                        "message": "Bad gateway error from upstream service"
                    }
                    """)));
        
        OtpGenerationRequest request = new OtpGenerationRequest(
            "device-token-123",
            OtpType.PUSH,
            OtpPurpose.SENSITIVE_OPERATION,
            5,
            null
        );

        // When
        OtpGenerationResponse response = otpService.generateOtp(request, "127.0.0.1", "Test-Agent");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isDelivered()).isFalse();
        assertThat(response.getType()).isEqualTo(OtpType.PUSH);
    }

    @Test
    void shouldHandleAuthenticationFailure() {
        // Given
        wireMockServer.stubFor(any(urlMatching("/api/.*"))
            .willReturn(aResponse()
                .withStatus(401)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "error": "UNAUTHORIZED",
                        "message": "Invalid API credentials"
                    }
                    """)));
        
        OtpGenerationRequest request = new OtpGenerationRequest(
            "test@example.com",
            OtpType.EMAIL,
            OtpPurpose.LOGIN,
            5,
            null
        );

        // When
        OtpGenerationResponse response = otpService.generateOtp(request, "127.0.0.1", "Test-Agent");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isDelivered()).isFalse();
        assertThat(response.getType()).isEqualTo(OtpType.EMAIL);
    }

    @Test
    void shouldHandleRateLimitingWithRetryAfter() {
        // Given
        wireMockServer.stubFor(any(urlMatching("/api/.*"))
            .willReturn(aResponse()
                .withStatus(429)
                .withHeader("Content-Type", "application/json")
                .withHeader("Retry-After", "3600")
                .withHeader("X-RateLimit-Limit", "100")
                .withHeader("X-RateLimit-Remaining", "0")
                .withHeader("X-RateLimit-Reset", "1640995200")
                .withBody("""
                    {
                        "error": "RATE_LIMIT_EXCEEDED",
                        "message": "API rate limit exceeded",
                        "retryAfter": 3600
                    }
                    """)));
        
        OtpGenerationRequest request = new OtpGenerationRequest(
            "+1234567890",
            OtpType.SMS,
            OtpPurpose.PASSWORD_RESET,
            5,
            null
        );

        // When
        OtpGenerationResponse response = otpService.generateOtp(request, "127.0.0.1", "Test-Agent");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isDelivered()).isFalse();
        assertThat(response.getType()).isEqualTo(OtpType.SMS);
    }

    @Test
    void shouldHandleInvalidResponseFormat() {
        // Given
        wireMockServer.stubFor(any(urlMatching("/api/.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("Invalid JSON response")));
        
        OtpGenerationRequest request = new OtpGenerationRequest(
            "test@example.com",
            OtpType.EMAIL,
            OtpPurpose.LOGIN,
            5,
            null
        );

        // When
        OtpGenerationResponse response = otpService.generateOtp(request, "127.0.0.1", "Test-Agent");

        // Then - Service should handle invalid response gracefully
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo(OtpType.EMAIL);
    }

    @Test
    void shouldHandlePartialServiceFailure() {
        // Given - Email service works, SMS service fails
        wireMockServer.stubFor(post(urlPathEqualTo("/api/email/send"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "messageId": "email-123",
                        "status": "sent"
                    }
                    """)));
        
        wireMockServer.stubFor(post(urlPathEqualTo("/api/sms/send"))
            .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "error": "SMS_SERVICE_DOWN",
                        "message": "SMS service is currently unavailable"
                    }
                    """)));
        
        // Test email (should succeed)
        OtpGenerationRequest emailRequest = new OtpGenerationRequest(
            "test@example.com",
            OtpType.EMAIL,
            OtpPurpose.LOGIN,
            5,
            null
        );
        
        // Test SMS (should fail)
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
        assertThat(emailResponse.isDelivered()).isTrue();
        assertThat(smsResponse.isDelivered()).isFalse();
    }

    @Test
    void shouldHandleCircuitBreakerPattern() {
        // Given - Multiple consecutive failures to trigger circuit breaker
        wireMockServer.stubFor(any(urlMatching("/api/.*"))
            .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "error": "SERVICE_ERROR",
                        "message": "Service error"
                    }
                    """)));
        
        OtpGenerationRequest request = new OtpGenerationRequest(
            "test@example.com",
            OtpType.EMAIL,
            OtpPurpose.LOGIN,
            5,
            null
        );

        // When - Make multiple requests to trigger circuit breaker
        OtpGenerationResponse response1 = otpService.generateOtp(request, "127.0.0.1", "Test-Agent");
        OtpGenerationResponse response2 = otpService.generateOtp(request, "127.0.0.1", "Test-Agent");
        OtpGenerationResponse response3 = otpService.generateOtp(request, "127.0.0.1", "Test-Agent");

        // Then - All should handle failure gracefully
        assertThat(response1.isDelivered()).isFalse();
        assertThat(response2.isDelivered()).isFalse();
        assertThat(response3.isDelivered()).isFalse();
        
        // Verify all requests were made
        wireMockServer.verify(3, postRequestedFor(urlPathEqualTo("/api/email/send")));
    }

    @Test
    void shouldHandleSlowResponseTimes() {
        // Given
        wireMockServer.stubFor(any(urlMatching("/api/.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withFixedDelay(5000) // 5 second delay
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "messageId": "slow-123",
                        "status": "sent",
                        "deliveryTime": 5000
                    }
                    """)));
        
        OtpGenerationRequest request = new OtpGenerationRequest(
            "test@example.com",
            OtpType.EMAIL,
            OtpPurpose.LOGIN,
            5,
            null
        );

        // When
        long startTime = System.currentTimeMillis();
        OtpGenerationResponse response = otpService.generateOtp(request, "127.0.0.1", "Test-Agent");
        long endTime = System.currentTimeMillis();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo(OtpType.EMAIL);
        
        // Verify the request took at least the delay time
        long duration = endTime - startTime;
        assertThat(duration).isGreaterThanOrEqualTo(5000);
    }
}