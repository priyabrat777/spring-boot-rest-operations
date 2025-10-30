package com.enterprise.api.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Base class for WireMock integration tests.
 * Provides common setup and teardown for external service mocking.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "wiremock"})
@Import({WireMockConfig.class, WireMockExternalServiceConfig.class})
public abstract class WireMockTestBase {

    @Autowired
    protected WireMockServer wireMockServer;

    @BeforeEach
    void setUpWireMock() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.resetAll();
            WireMock.configureFor("localhost", wireMockServer.port());
            setupWireMockStubs();
        }
    }

    @AfterEach
    void tearDownWireMock() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.resetAll();
        }
    }

    /**
     * Override this method to set up specific WireMock stubs for each test class.
     */
    protected abstract void setupWireMockStubs();

    /**
     * Helper method to create successful email delivery stub.
     */
    protected void stubSuccessfulEmailDelivery() {
        stubFor(post(urlMatching("/email/send.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"messageId\": \"email-12345\", \"status\": \"sent\"}")
                )
        );
    }

    /**
     * Helper method to create successful SMS delivery stub.
     */
    protected void stubSuccessfulSmsDelivery() {
        stubFor(post(urlMatching("/sms/send.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"messageId\": \"sms-12345\", \"status\": \"sent\"}")
                )
        );
    }

    /**
     * Helper method to create successful file storage stub.
     */
    protected void stubSuccessfulFileStorage() {
        stubFor(post(urlMatching("/files/upload.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"fileId\": \"file-12345\", \"status\": \"uploaded\"}")
                )
        );
    }

    /**
     * Helper method to create service failure stub.
     */
    protected void stubServiceFailure(String urlPattern, int statusCode) {
        stubFor(post(urlMatching(urlPattern))
                .willReturn(aResponse()
                        .withStatus(statusCode)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Service temporarily unavailable\"}")
                )
        );
    }
}