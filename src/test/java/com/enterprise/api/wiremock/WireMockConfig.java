package com.enterprise.api.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

/**
 * WireMock configuration for testing external service integrations.
 * Provides mock servers for OTP delivery services and file storage services.
 */
@TestConfiguration
@Profile({"test", "wiremock"})
public class WireMockConfig {

    @Bean(destroyMethod = "stop")
    public WireMockServer wireMockServer() {
        WireMockServer wireMockServer = new WireMockServer(
            WireMockConfiguration.options()
                .dynamicPort() // Use dynamic port to avoid conflicts
                .usingFilesUnderClasspath("wiremock")
                // Reduce noise in logs by using console notifier with minimal output
        );
        
        if (!wireMockServer.isRunning()) {
            wireMockServer.start(); // Start the server only if not running
        }
        return wireMockServer;
    }
}