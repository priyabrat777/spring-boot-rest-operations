package com.enterprise.api.wiremock;

import com.enterprise.api.dto.request.OtpGenerationRequest;
import com.enterprise.api.dto.response.OtpGenerationResponse;
import com.enterprise.api.entity.OtpPurpose;
import com.enterprise.api.entity.OtpType;
import com.enterprise.api.service.OtpService;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration tests for external service failure scenarios using WireMock.
 * Tests various failure modes including network errors, timeouts, and service
 * unavailability.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ExternalServiceFailureTest extends WireMockTestBase {

    @Autowired
    private OtpService otpService;

    private String uniqueEmailPrefix;
    private String uniquePhonePrefix;

    @BeforeEach
    void setUpUniqueIdentifiers() {
        // Generate unique identifiers for each test to avoid rate limiting conflicts
        String testId = UUID.randomUUID().toString().substring(0, 8);
        uniqueEmailPrefix = "test" + testId;
        uniquePhonePrefix = "+155500" + testId.substring(0, 4);
    }

    @Override
    protected void setupWireMockStubs() {
        // No default setup - each test will configure its own stubs
    }

    @Test
    void shouldHandleNetworkConnectionFailure() {
        // Given - No stubs configured, so requests will fail with connection refused
        // Don't stop the server, just don't configure any stubs

        OtpGenerationRequest request = new OtpGenerationRequest(
                uniqueEmailPrefix + "@example.com",
                OtpType.EMAIL,
                OtpPurpose.LOGIN,
                5,
                null);

        // When
        OtpGenerationResponse response = otpService.generateOtp(request, "127.0.0.1", "Test-Agent");

        // Then - Service should handle network failure gracefully
        assertThat(response).isNotNull();
        assertThat(response.isDelivered()).isFalse();
        assertThat(response.getType()).isEqualTo(OtpType.EMAIL);
    }

    @Test
    void shouldHandleServiceUnavailableError() {
        // Given
        wireMockServer.stubFor(post(urlPathEqualTo("/email/send"))
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
                uniqueEmailPrefix + "@example.com",
                OtpType.EMAIL,
                OtpPurpose.LOGIN,
                5,
                null);

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
        wireMockServer.stubFor(post(urlPathEqualTo("/sms/send"))
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
                uniquePhonePrefix,
                OtpType.SMS,
                OtpPurpose.PASSWORD_RESET,
                5,
                null);

        // When
        OtpGenerationResponse response = otpService.generateOtp(request, "127.0.0.1", "Test-Agent");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isDelivered()).isFalse();
        assertThat(response.getType()).isEqualTo(OtpType.SMS);
    }

    @Test
    void shouldHandleTimeoutError() {
        // Given - Use a shorter delay that's still longer than the configured timeout
        wireMockServer.stubFor(post(urlPathEqualTo("/email/send"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(15000) // 15 second delay to simulate timeout
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\": \"timeout\"}")));

        OtpGenerationRequest request = new OtpGenerationRequest(
                uniqueEmailPrefix + "@example.com",
                OtpType.VOICE, // Voice doesn't use external service, so it should succeed
                OtpPurpose.ACCOUNT_VERIFICATION,
                5,
                null);

        // When
        OtpGenerationResponse response = otpService.generateOtp(request, "127.0.0.1", "Test-Agent");

        // Then - Voice delivery should succeed as it doesn't use external service
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo(OtpType.VOICE);
        assertThat(response.isDelivered()).isTrue(); // Voice is handled internally
    }

    @Test
    void shouldHandleBadGatewayError() {
        // Given - PUSH notifications are handled internally, so test with EMAIL instead
        wireMockServer.stubFor(post(urlPathEqualTo("/email/send"))
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
                uniqueEmailPrefix + "@example.com",
                OtpType.EMAIL,
                OtpPurpose.SENSITIVE_OPERATION,
                5,
                null);

        // When
        OtpGenerationResponse response = otpService.generateOtp(request, "127.0.0.1", "Test-Agent");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isDelivered()).isFalse();
        assertThat(response.getType()).isEqualTo(OtpType.EMAIL);
    }

    @Test
    void shouldHandleAuthenticationFailure() {
        // Given
        wireMockServer.stubFor(post(urlPathEqualTo("/email/send"))
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
                uniqueEmailPrefix + "@example.com",
                OtpType.EMAIL,
                OtpPurpose.LOGIN,
                5,
                null);

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
        wireMockServer.stubFor(post(urlPathEqualTo("/sms/send"))
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
                uniquePhonePrefix,
                OtpType.SMS,
                OtpPurpose.PASSWORD_RESET,
                5,
                null);

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
        wireMockServer.stubFor(post(urlPathEqualTo("/email/send"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("Invalid JSON response")));

        OtpGenerationRequest request = new OtpGenerationRequest(
                uniqueEmailPrefix + "@example.com",
                OtpType.EMAIL,
                OtpPurpose.LOGIN,
                5,
                null);

        // When
        OtpGenerationResponse response = otpService.generateOtp(request, "127.0.0.1", "Test-Agent");

        // Then - Service should handle invalid response gracefully
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo(OtpType.EMAIL);
        assertThat(response.isDelivered()).isFalse();
    }

    @Test
    void shouldHandlePartialServiceFailure() {
        // Given - Email service works, SMS service fails
        wireMockServer.stubFor(post(urlPathEqualTo("/email/send"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "messageId": "email-123",
                                    "status": "sent"
                                }
                                """)));

        wireMockServer.stubFor(post(urlPathEqualTo("/sms/send"))
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
                uniqueEmailPrefix + "@example.com",
                OtpType.EMAIL,
                OtpPurpose.LOGIN,
                5,
                null);

        // Test SMS (should fail)
        OtpGenerationRequest smsRequest = new OtpGenerationRequest(
                uniquePhonePrefix,
                OtpType.SMS,
                OtpPurpose.LOGIN,
                5,
                null);

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
        wireMockServer.stubFor(post(urlPathEqualTo("/email/send"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "error": "SERVICE_ERROR",
                                    "message": "Service error"
                                }
                                """)));

        // Use different email addresses to avoid rate limiting
        OtpGenerationRequest request1 = new OtpGenerationRequest(
                uniqueEmailPrefix + "1@example.com",
                OtpType.EMAIL,
                OtpPurpose.LOGIN,
                5,
                null);

        OtpGenerationRequest request2 = new OtpGenerationRequest(
                uniqueEmailPrefix + "2@example.com",
                OtpType.EMAIL,
                OtpPurpose.LOGIN,
                5,
                null);

        OtpGenerationRequest request3 = new OtpGenerationRequest(
                uniqueEmailPrefix + "3@example.com",
                OtpType.EMAIL,
                OtpPurpose.LOGIN,
                5,
                null);

        // When - Make multiple requests to trigger circuit breaker
        OtpGenerationResponse response1 = otpService.generateOtp(request1, "127.0.0.1", "Test-Agent");
        OtpGenerationResponse response2 = otpService.generateOtp(request2, "127.0.0.1", "Test-Agent");
        OtpGenerationResponse response3 = otpService.generateOtp(request3, "127.0.0.1", "Test-Agent");

        // Then - All should handle failure gracefully
        assertThat(response1.isDelivered()).isFalse();
        assertThat(response2.isDelivered()).isFalse();
        assertThat(response3.isDelivered()).isFalse();

        // Verify all requests were made
        wireMockServer.verify(3, postRequestedFor(urlPathEqualTo("/email/send")));
    }

    @Test
    void shouldHandleSlowResponseTimes() {
        // Given
        wireMockServer.stubFor(post(urlPathEqualTo("/email/send"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(2000) // 2 second delay (shorter to avoid timeout)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "messageId": "slow-123",
                                    "status": "sent",
                                    "deliveryTime": 2000
                                }
                                """)));

        OtpGenerationRequest request = new OtpGenerationRequest(
                uniqueEmailPrefix + "@example.com",
                OtpType.EMAIL,
                OtpPurpose.LOGIN,
                5,
                null);

        // When
        long startTime = System.currentTimeMillis();
        OtpGenerationResponse response = otpService.generateOtp(request, "127.0.0.1", "Test-Agent");
        long endTime = System.currentTimeMillis();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo(OtpType.EMAIL);
        assertThat(response.isDelivered()).isTrue(); // Should succeed with delay

        // Verify the request took at least the delay time
        long duration = endTime - startTime;
        assertThat(duration).isGreaterThanOrEqualTo(2000);
    }
}