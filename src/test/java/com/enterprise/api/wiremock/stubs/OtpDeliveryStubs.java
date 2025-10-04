package com.enterprise.api.wiremock.stubs;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * WireMock stubs for OTP delivery services.
 * Provides mock responses for email, SMS, voice, and push notification services.
 */
public class OtpDeliveryStubs {

    /**
     * Set up successful email delivery stubs.
     */
    public static void setupEmailDeliverySuccess(WireMockServer wireMockServer) {
        wireMockServer.stubFor(post(urlPathEqualTo("/api/email/send"))
            .withHeader("Content-Type", containing("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "messageId": "email-123456",
                        "status": "sent",
                        "recipient": "user@example.com",
                        "timestamp": "2024-01-01T12:00:00Z",
                        "deliveryTime": 1500
                    }
                    """)));
    }

    /**
     * Set up email delivery failure stubs.
     */
    public static void setupEmailDeliveryFailure(WireMockServer wireMockServer) {
        wireMockServer.stubFor(post(urlPathEqualTo("/api/email/send"))
            .withHeader("Content-Type", containing("application/json"))
            .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "error": "DELIVERY_FAILED",
                        "message": "Failed to deliver email",
                        "code": "EMAIL_SERVICE_UNAVAILABLE",
                        "timestamp": "2024-01-01T12:00:00Z"
                    }
                    """)));
    }

    /**
     * Set up email delivery timeout stubs.
     */
    public static void setupEmailDeliveryTimeout(WireMockServer wireMockServer) {
        wireMockServer.stubFor(post(urlPathEqualTo("/api/email/send"))
            .willReturn(aResponse()
                .withStatus(200)
                .withFixedDelay(30000) // 30 second delay to simulate timeout
                .withHeader("Content-Type", "application/json")
                .withBody("{\"status\": \"timeout\"}")));
    }

    /**
     * Set up successful SMS delivery stubs.
     */
    public static void setupSmsDeliverySuccess(WireMockServer wireMockServer) {
        wireMockServer.stubFor(post(urlPathEqualTo("/api/sms/send"))
            .withHeader("Content-Type", containing("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "messageId": "sms-789012",
                        "status": "sent",
                        "recipient": "+1234567890",
                        "timestamp": "2024-01-01T12:00:00Z",
                        "deliveryTime": 2000,
                        "cost": 0.05
                    }
                    """)));
    }

    /**
     * Set up SMS delivery failure stubs.
     */
    public static void setupSmsDeliveryFailure(WireMockServer wireMockServer) {
        wireMockServer.stubFor(post(urlPathEqualTo("/api/sms/send"))
            .withHeader("Content-Type", containing("application/json"))
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "error": "INVALID_PHONE_NUMBER",
                        "message": "The provided phone number is invalid",
                        "code": "SMS_INVALID_RECIPIENT",
                        "timestamp": "2024-01-01T12:00:00Z"
                    }
                    """)));
    }

    /**
     * Set up successful voice delivery stubs.
     */
    public static void setupVoiceDeliverySuccess(WireMockServer wireMockServer) {
        wireMockServer.stubFor(post(urlPathEqualTo("/api/voice/call"))
            .withHeader("Content-Type", containing("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "callId": "voice-345678",
                        "status": "completed",
                        "recipient": "+1234567890",
                        "timestamp": "2024-01-01T12:00:00Z",
                        "duration": 45,
                        "cost": 0.15
                    }
                    """)));
    }

    /**
     * Set up voice delivery failure stubs.
     */
    public static void setupVoiceDeliveryFailure(WireMockServer wireMockServer) {
        wireMockServer.stubFor(post(urlPathEqualTo("/api/voice/call"))
            .withHeader("Content-Type", containing("application/json"))
            .willReturn(aResponse()
                .withStatus(503)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "error": "SERVICE_UNAVAILABLE",
                        "message": "Voice service is temporarily unavailable",
                        "code": "VOICE_SERVICE_DOWN",
                        "timestamp": "2024-01-01T12:00:00Z"
                    }
                    """)));
    }

    /**
     * Set up successful push notification stubs.
     */
    public static void setupPushDeliverySuccess(WireMockServer wireMockServer) {
        wireMockServer.stubFor(post(urlPathEqualTo("/api/push/send"))
            .withHeader("Content-Type", containing("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "notificationId": "push-901234",
                        "status": "delivered",
                        "deviceToken": "device-token-123",
                        "timestamp": "2024-01-01T12:00:00Z",
                        "deliveryTime": 800
                    }
                    """)));
    }

    /**
     * Set up push notification failure stubs.
     */
    public static void setupPushDeliveryFailure(WireMockServer wireMockServer) {
        wireMockServer.stubFor(post(urlPathEqualTo("/api/push/send"))
            .withHeader("Content-Type", containing("application/json"))
            .willReturn(aResponse()
                .withStatus(410)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "error": "DEVICE_TOKEN_INVALID",
                        "message": "The device token is no longer valid",
                        "code": "PUSH_TOKEN_EXPIRED",
                        "timestamp": "2024-01-01T12:00:00Z"
                    }
                    """)));
    }

    /**
     * Set up rate limiting stubs for OTP services.
     */
    public static void setupRateLimitingStubs(WireMockServer wireMockServer) {
        wireMockServer.stubFor(post(urlMatching("/api/(email|sms|voice|push)/.*"))
            .withHeader("X-Rate-Limit-Remaining", equalTo("0"))
            .willReturn(aResponse()
                .withStatus(429)
                .withHeader("Content-Type", "application/json")
                .withHeader("X-Rate-Limit-Reset", "3600")
                .withBody("""
                    {
                        "error": "RATE_LIMIT_EXCEEDED",
                        "message": "Too many requests. Please try again later.",
                        "code": "RATE_LIMIT_EXCEEDED",
                        "retryAfter": 3600,
                        "timestamp": "2024-01-01T12:00:00Z"
                    }
                    """)));
    }

    /**
     * Verify that email delivery was called with correct parameters.
     */
    public static void verifyEmailDeliveryCall(WireMockServer wireMockServer, String recipient, String subject) {
        wireMockServer.verify(postRequestedFor(urlPathEqualTo("/api/email/send"))
            .withHeader("Content-Type", containing("application/json"))
            .withRequestBody(containing("\"recipient\":\"" + recipient + "\""))
            .withRequestBody(containing("\"subject\":\"" + subject + "\"")));
    }

    /**
     * Verify that SMS delivery was called with correct parameters.
     */
    public static void verifySmsDeliveryCall(WireMockServer wireMockServer, String phoneNumber) {
        wireMockServer.verify(postRequestedFor(urlPathEqualTo("/api/sms/send"))
            .withHeader("Content-Type", containing("application/json"))
            .withRequestBody(containing("\"recipient\":\"" + phoneNumber + "\"")));
    }

    /**
     * Verify that voice delivery was called with correct parameters.
     */
    public static void verifyVoiceDeliveryCall(WireMockServer wireMockServer, String phoneNumber) {
        wireMockServer.verify(postRequestedFor(urlPathEqualTo("/api/voice/call"))
            .withHeader("Content-Type", containing("application/json"))
            .withRequestBody(containing("\"recipient\":\"" + phoneNumber + "\"")));
    }

    /**
     * Verify that push notification was called with correct parameters.
     */
    public static void verifyPushDeliveryCall(WireMockServer wireMockServer, String deviceToken) {
        wireMockServer.verify(postRequestedFor(urlPathEqualTo("/api/push/send"))
            .withHeader("Content-Type", containing("application/json"))
            .withRequestBody(containing("\"deviceToken\":\"" + deviceToken + "\"")));
    }

    /**
     * Get the count of requests made to a specific endpoint.
     */
    public static int getRequestCount(WireMockServer wireMockServer, String endpoint) {
        return wireMockServer.countRequestsMatching(
            postRequestedFor(urlPathEqualTo(endpoint)).build()
        ).getCount();
    }
}