package com.enterprise.api.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * WireMock configuration for testing external service integrations.
 * Provides mock servers for OTP delivery services and file storage services.
 */
@TestConfiguration
public class WireMockConfig {

    public static final int WIREMOCK_PORT = 8089;

    @Bean
    public WireMockServer wireMockServer() {
        WireMockServer wireMockServer = new WireMockServer(
            WireMockConfiguration.options()
                .port(WIREMOCK_PORT)
        );
        
        return wireMockServer;
    }
}