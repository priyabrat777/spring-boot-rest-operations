package com.enterprise.api.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic WireMock test to verify setup and functionality.
 * This test demonstrates core WireMock capabilities without Spring context.
 */
class WireMockBasicTest {

    private WireMockServer wireMockServer;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().port(8089));
        wireMockServer.start();
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    @Test
    void shouldStartWireMockServer() {
        // Given
        assertThat(wireMockServer.isRunning()).isTrue();
        assertThat(wireMockServer.port()).isEqualTo(8089);
    }

    @Test
    void shouldCreateBasicStub() {
        // Given
        wireMockServer.stubFor(get(urlEqualTo("/api/test"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"message\": \"Hello WireMock\"}")));

        // When - In a real test, you would make an HTTP request here
        // For this basic test, we just verify the stub was created

        // Then
        assertThat(wireMockServer.getStubMappings()).hasSize(1);
    }

    @Test
    void shouldCreateOtpEmailDeliveryStub() {
        // Given
        wireMockServer.stubFor(post(urlPathEqualTo("/api/email/send"))
            .withHeader("Content-Type", containing("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "messageId": "email-123456",
                        "status": "sent",
                        "recipient": "test@example.com",
                        "timestamp": "2024-01-01T12:00:00Z",
                        "deliveryTime": 1500
                    }
                    """)));

        // Then
        assertThat(wireMockServer.getStubMappings()).hasSize(1);
    }

    @Test
    void shouldCreateFileUploadStub() {
        // Given
        wireMockServer.stubFor(post(urlPathEqualTo("/api/storage/upload"))
            .withHeader("Content-Type", containing("multipart/form-data"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "fileId": "file-123456789",
                        "fileName": "document.pdf",
                        "fileSize": 1048576,
                        "contentType": "application/pdf",
                        "uploadUrl": "https://storage.example.com/files/file-123456789",
                        "downloadUrl": "https://storage.example.com/download/file-123456789",
                        "checksum": "sha256:abcdef123456789",
                        "timestamp": "2024-01-01T12:00:00Z"
                    }
                    """)));

        // Then
        assertThat(wireMockServer.getStubMappings()).hasSize(1);
    }

    @Test
    void shouldCreateFailureStubs() {
        // Given - Service unavailable
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

        // Then
        assertThat(wireMockServer.getStubMappings()).hasSize(1);
    }

    @Test
    void shouldCreateTimeoutStub() {
        // Given
        wireMockServer.stubFor(get(urlPathEqualTo("/api/slow"))
            .willReturn(aResponse()
                .withStatus(200)
                .withFixedDelay(5000) // 5 second delay
                .withHeader("Content-Type", "application/json")
                .withBody("{\"status\": \"slow response\"}")));

        // Then
        assertThat(wireMockServer.getStubMappings()).hasSize(1);
    }

    @Test
    void shouldVerifyRequestsWereMade() {
        // Given
        wireMockServer.stubFor(post(urlPathEqualTo("/api/email/send"))
            .willReturn(aResponse().withStatus(200)));

        // When - Simulate making requests (in real test, actual HTTP calls would be made)
        // For this test, we'll just verify the verification methods work
        
        // Then - Verify no requests were made initially
        assertThat(wireMockServer.countRequestsMatching(
            postRequestedFor(urlPathEqualTo("/api/email/send")).build()
        ).getCount()).isEqualTo(0);
    }

    @Test
    void shouldResetStubs() {
        // Given
        wireMockServer.stubFor(get(urlEqualTo("/api/test1"))
            .willReturn(aResponse().withStatus(200)));
        
        wireMockServer.stubFor(get(urlEqualTo("/api/test2"))
            .willReturn(aResponse().withStatus(200)));

        assertThat(wireMockServer.getStubMappings()).hasSize(2);

        // When
        wireMockServer.resetAll();

        // Then
        assertThat(wireMockServer.getStubMappings()).isEmpty();
    }
}