package com.enterprise.api.wiremock;

import com.enterprise.api.dto.request.OtpGenerationRequest;
import com.enterprise.api.dto.request.OtpValidationRequest;
import com.enterprise.api.dto.response.OtpGenerationResponse;
import com.enterprise.api.dto.response.OtpValidationResponse;
import com.enterprise.api.entity.OtpPurpose;
import com.enterprise.api.entity.OtpType;
import com.enterprise.api.service.OtpService;
import com.enterprise.api.wiremock.stubs.OtpDeliveryStubs;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for OTP delivery services using WireMock.
 * Tests external service integration scenarios including success and failure cases.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OtpDeliveryWireMockTest extends WireMockTestBase {

    @Autowired
    private OtpService otpService;

    @Override
    protected void setupWireMockStubs() {
        // Default setup - can be overridden in individual tests
        OtpDeliveryStubs.setupEmailDeliverySuccess(wireMockServer);
        OtpDeliveryStubs.setupSmsDeliverySuccess(wireMockServer);
        OtpDeliveryStubs.setupVoiceDeliverySuccess(wireMockServer);
        OtpDeliveryStubs.setupPushDeliverySuccess(wireMockServer);
    }

    @Test
    void shouldGenerateOtpWithSuccessfulEmailDelivery() {
        // Given
        OtpDeliveryStubs.setupEmailDeliverySuccess(wireMockServer);
        
        OtpGenerationRequest request = new OtpGenerationRequest(
            "test@example.com",
            OtpType.EMAIL,
            OtpPurpose.LOGIN,
            5,
            "Your login verification code"
        );

        // When
        OtpGenerationResponse response = otpService.generateOtp(request, "127.0.0.1", "Test-Agent");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isDelivered()).isTrue();
        assertThat(response.getMessage()).isEqualTo("OTP generated successfully");
        assertThat(response.getType()).isEqualTo(OtpType.EMAIL);
        assertThat(response.getPurpose()).isEqualTo(OtpPurpose.LOGIN);
        assertThat(response.getMaxAttempts()).isEqualTo(3);

        // Verify external service was called
        OtpDeliveryStubs.verifyEmailDeliveryCall(wireMockServer, "test@example.com", "Your login verification code");
    }

    @Test
    void shouldGenerateOtpWithSuccessfulSmsDelivery() {
        // Given
        OtpDeliveryStubs.setupSmsDeliverySuccess(wireMockServer);
        
        OtpGenerationRequest request = new OtpGenerationRequest(
            "+1234567890",
            OtpType.SMS,
            OtpPurpose.PASSWORD_RESET,
            10,
            null
        );

        // When
        OtpGenerationResponse response = otpService.generateOtp(request, "127.0.0.1", "Test-Agent");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isDelivered()).isTrue();
        assertThat(response.getType()).isEqualTo(OtpType.SMS);
        assertThat(response.getPurpose()).isEqualTo(OtpPurpose.PASSWORD_RESET);

        // Verify external service was called
        OtpDeliveryStubs.verifySmsDeliveryCall(wireMockServer, "+1234567890");
    }

    @Test
    void shouldGenerateOtpWithSuccessfulVoiceDelivery() {
        // Given
        OtpDeliveryStubs.setupVoiceDeliverySuccess(wireMockServer);
        
        OtpGenerationRequest request = new OtpGenerationRequest(
            "+1234567890",
            OtpType.VOICE,
            OtpPurpose.ACCOUNT_VERIFICATION,
            15,
            null
        );

        // When
        OtpGenerationResponse response = otpService.generateOtp(request, "127.0.0.1", "Test-Agent");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isDelivered()).isTrue();
        assertThat(response.getType()).isEqualTo(OtpType.VOICE);
        assertThat(response.getPurpose()).isEqualTo(OtpPurpose.ACCOUNT_VERIFICATION);

        // Verify external service was called
        OtpDeliveryStubs.verifyVoiceDeliveryCall(wireMockServer, "+1234567890");
    }

    @Test
    void shouldGenerateOtpWithSuccessfulPushDelivery() {
        // Given
        OtpDeliveryStubs.setupPushDeliverySuccess(wireMockServer);
        
        OtpGenerationRequest request = new OtpGenerationRequest(
            "device-token-123",
            OtpType.PUSH,
            OtpPurpose.SENSITIVE_OPERATION,
            3,
            null
        );

        // When
        OtpGenerationResponse response = otpService.generateOtp(request, "127.0.0.1", "Test-Agent");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isDelivered()).isTrue();
        assertThat(response.getType()).isEqualTo(OtpType.PUSH);
        assertThat(response.getPurpose()).isEqualTo(OtpPurpose.SENSITIVE_OPERATION);

        // Verify external service was called
        OtpDeliveryStubs.verifyPushDeliveryCall(wireMockServer, "device-token-123");
    }

    @Test
    void shouldHandleEmailDeliveryFailure() {
        // Given
        OtpDeliveryStubs.setupEmailDeliveryFailure(wireMockServer);
        
        OtpGenerationRequest request = new OtpGenerationRequest(
            "invalid@example.com",
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

        // Verify external service was called
        OtpDeliveryStubs.verifyEmailDeliveryCall(wireMockServer, "invalid@example.com", "Your OTP Code - Login");
    }

    @Test
    void shouldHandleSmsDeliveryFailure() {
        // Given
        OtpDeliveryStubs.setupSmsDeliveryFailure(wireMockServer);
        
        OtpGenerationRequest request = new OtpGenerationRequest(
            "invalid-phone",
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

        // Verify external service was called
        OtpDeliveryStubs.verifySmsDeliveryCall(wireMockServer, "invalid-phone");
    }

    @Test
    void shouldHandleVoiceDeliveryFailure() {
        // Given
        OtpDeliveryStubs.setupVoiceDeliveryFailure(wireMockServer);
        
        OtpGenerationRequest request = new OtpGenerationRequest(
            "+1234567890",
            OtpType.VOICE,
            OtpPurpose.ACCOUNT_VERIFICATION,
            5,
            null
        );

        // When
        OtpGenerationResponse response = otpService.generateOtp(request, "127.0.0.1", "Test-Agent");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isDelivered()).isFalse();
        assertThat(response.getType()).isEqualTo(OtpType.VOICE);

        // Verify external service was called
        OtpDeliveryStubs.verifyVoiceDeliveryCall(wireMockServer, "+1234567890");
    }

    @Test
    void shouldHandlePushDeliveryFailure() {
        // Given
        OtpDeliveryStubs.setupPushDeliveryFailure(wireMockServer);
        
        OtpGenerationRequest request = new OtpGenerationRequest(
            "expired-device-token",
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

        // Verify external service was called
        OtpDeliveryStubs.verifyPushDeliveryCall(wireMockServer, "expired-device-token");
    }

    @Test
    void shouldHandleRateLimitingFromExternalService() {
        // Given
        OtpDeliveryStubs.setupRateLimitingStubs(wireMockServer);
        
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
    void shouldHandleEmailDeliveryTimeout() {
        // Given
        OtpDeliveryStubs.setupEmailDeliveryTimeout(wireMockServer);
        
        OtpGenerationRequest request = new OtpGenerationRequest(
            "test@example.com",
            OtpType.EMAIL,
            OtpPurpose.LOGIN,
            5,
            null
        );

        // When & Then - This should timeout and handle gracefully
        // Note: In a real implementation, you might want to configure shorter timeouts for testing
        OtpGenerationResponse response = otpService.generateOtp(request, "127.0.0.1", "Test-Agent");
        
        // The service should handle timeout gracefully
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo(OtpType.EMAIL);
    }

    @Test
    void shouldValidateOtpSuccessfully() {
        // Given - First generate an OTP
        OtpDeliveryStubs.setupEmailDeliverySuccess(wireMockServer);
        
        OtpGenerationRequest genRequest = new OtpGenerationRequest(
            "test@example.com",
            OtpType.EMAIL,
            OtpPurpose.LOGIN,
            5,
            null
        );
        
        OtpGenerationResponse genResponse = otpService.generateOtp(genRequest, "127.0.0.1", "Test-Agent");
        
        // Get the test OTP code (in real implementation, this would come from the user)
        String testOtpCode = otpService.generateTestOtp("test@example.com", OtpType.EMAIL, OtpPurpose.LOGIN);
        
        OtpValidationRequest valRequest = new OtpValidationRequest(
            "test@example.com",
            testOtpCode,
            OtpPurpose.LOGIN,
            true
        );

        // When
        OtpValidationResponse valResponse = otpService.validateOtp(valRequest, "127.0.0.1");

        // Then
        assertThat(valResponse).isNotNull();
        assertThat(valResponse.isValid()).isTrue();
        assertThat(valResponse.isConsumed()).isTrue();
    }

    @Test
    void shouldTrackExternalServiceCallCounts() {
        // Given
        OtpDeliveryStubs.setupEmailDeliverySuccess(wireMockServer);
        
        OtpGenerationRequest request1 = new OtpGenerationRequest(
            "user1@example.com",
            OtpType.EMAIL,
            OtpPurpose.LOGIN,
            5,
            null
        );
        
        OtpGenerationRequest request2 = new OtpGenerationRequest(
            "user2@example.com",
            OtpType.EMAIL,
            OtpPurpose.PASSWORD_RESET,
            5,
            null
        );

        // When
        otpService.generateOtp(request1, "127.0.0.1", "Test-Agent");
        otpService.generateOtp(request2, "127.0.0.1", "Test-Agent");

        // Then
        int emailCallCount = OtpDeliveryStubs.getRequestCount(wireMockServer, "/api/email/send");
        assertThat(emailCallCount).isEqualTo(2);
    }
}