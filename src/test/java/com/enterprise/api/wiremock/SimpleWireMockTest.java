package com.enterprise.api.wiremock;

import com.enterprise.api.dto.request.OtpGenerationRequest;
import com.enterprise.api.entity.OtpPurpose;
import com.enterprise.api.entity.OtpType;
import com.enterprise.api.service.OtpService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple WireMock integration test to verify external service mocking works.
 */
class SimpleWireMockTest extends WireMockTestBase {

    @Autowired
    private OtpService otpService;

    @Override
    protected void setupWireMockStubs() {
        stubSuccessfulEmailDelivery();
        stubSuccessfulSmsDelivery();
    }

    @Test
    void shouldGenerateOtpWithMockedEmailService() {
        // Arrange
        OtpGenerationRequest request = new OtpGenerationRequest(
            "test@example.com",
            OtpType.EMAIL,
            OtpPurpose.LOGIN
        );

        // Act
        var response = otpService.generateOtp(request, "127.0.0.1", "Test-Agent");

        // Assert
        assertNotNull(response);
        assertTrue(response.isDelivered());
        assertEquals(OtpType.EMAIL, response.getType());
        assertEquals(OtpPurpose.LOGIN, response.getPurpose());
        
        // Verify WireMock was called
        verify(postRequestedFor(urlMatching("/email/send.*")));
    }

    @Test
    void shouldGenerateOtpWithMockedSmsService() {
        // Arrange
        OtpGenerationRequest request = new OtpGenerationRequest(
            "+1234567890",
            OtpType.SMS,
            OtpPurpose.LOGIN
        );

        // Act
        var response = otpService.generateOtp(request, "127.0.0.1", "Test-Agent");

        // Assert
        assertNotNull(response);
        assertTrue(response.isDelivered());
        assertEquals(OtpType.SMS, response.getType());
        assertEquals(OtpPurpose.LOGIN, response.getPurpose());
        
        // Verify WireMock was called
        verify(postRequestedFor(urlMatching("/sms/send.*")));
    }

    @Test
    void shouldHandleServiceFailure() {
        // Arrange - Override with failure stub
        wireMockServer.resetAll();
        stubServiceFailure("/email/send.*", 500);
        
        OtpGenerationRequest request = new OtpGenerationRequest(
            "test@example.com",
            OtpType.EMAIL,
            OtpPurpose.LOGIN
        );

        // Act
        var response = otpService.generateOtp(request, "127.0.0.1", "Test-Agent");

        // Assert
        assertNotNull(response);
        assertFalse(response.isDelivered()); // Should fail due to service error
        
        // Verify WireMock was called
        verify(postRequestedFor(urlMatching("/email/send.*")));
    }
}