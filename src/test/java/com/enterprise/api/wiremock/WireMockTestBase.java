package com.enterprise.api.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * Base class for tests that require WireMock server integration.
 * Handles server lifecycle and provides common setup for external service mocking.
 */
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

    @AfterEach
    void tearDown() {
        if (wireMockServer.isRunning()) {
            wireMockServer.resetAll();
        }
    }

    /**
     * Override this method to set up specific WireMock stubs for your test.
     */
    protected abstract void setupWireMockStubs();

    /**
     * Get the base URL for WireMock server.
     */
    protected String getWireMockBaseUrl() {
        return "http://localhost:" + WireMockConfig.WIREMOCK_PORT;
    }
}