package com.enterprise.api.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Test configuration that overrides external service RestTemplate beans
 * to point to WireMock server for integration testing.
 */
@TestConfiguration
@Profile({"test", "wiremock"})
public class WireMockExternalServiceConfig {

    @Bean("emailServiceRestTemplate")
    @Primary
    public RestTemplate emailServiceRestTemplate(WireMockServer wireMockServer, RestTemplateBuilder builder) {
        return builder
                .rootUri("http://localhost:" + wireMockServer.port())
                .setConnectTimeout(Duration.ofMillis(5000))
                .setReadTimeout(Duration.ofMillis(10000))
                .build();
    }

    @Bean("smsServiceRestTemplate")
    @Primary
    public RestTemplate smsServiceRestTemplate(WireMockServer wireMockServer, RestTemplateBuilder builder) {
        return builder
                .rootUri("http://localhost:" + wireMockServer.port())
                .setConnectTimeout(Duration.ofMillis(5000))
                .setReadTimeout(Duration.ofMillis(10000))
                .build();
    }

    @Bean("fileStorageServiceRestTemplate")
    @Primary
    public RestTemplate fileStorageServiceRestTemplate(WireMockServer wireMockServer, RestTemplateBuilder builder) {
        return builder
                .rootUri("http://localhost:" + wireMockServer.port())
                .setConnectTimeout(Duration.ofMillis(5000))
                .setReadTimeout(Duration.ofMillis(10000))
                .build();
    }
}