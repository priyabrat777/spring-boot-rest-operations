package com.enterprise.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for external service clients.
 * Provides RestTemplate beans configured for different external services.
 */
@Configuration
@Profile("!test")
public class ExternalServiceConfig {

    @Value("${app.external.email.base-url:http://localhost:8080}")
    private String emailServiceBaseUrl;

    @Value("${app.external.sms.base-url:http://localhost:8080}")
    private String smsServiceBaseUrl;

    @Value("${app.external.file-storage.base-url:http://localhost:8080}")
    private String fileStorageServiceBaseUrl;

    @Value("${app.external.timeout.connect:5000}")
    private int connectTimeout;

    @Value("${app.external.timeout.read:10000}")
    private int readTimeout;

    @Bean("emailServiceRestTemplate")
    public RestTemplate emailServiceRestTemplate(RestTemplateBuilder builder) {
        return builder
                .rootUri(emailServiceBaseUrl)
                .setConnectTimeout(Duration.ofMillis(connectTimeout))
                .setReadTimeout(Duration.ofMillis(readTimeout))
                .build();
    }

    @Bean("smsServiceRestTemplate")
    public RestTemplate smsServiceRestTemplate(RestTemplateBuilder builder) {
        return builder
                .rootUri(smsServiceBaseUrl)
                .setConnectTimeout(Duration.ofMillis(connectTimeout))
                .setReadTimeout(Duration.ofMillis(readTimeout))
                .build();
    }

    @Bean("fileStorageServiceRestTemplate")
    public RestTemplate fileStorageServiceRestTemplate(RestTemplateBuilder builder) {
        return builder
                .rootUri(fileStorageServiceBaseUrl)
                .setConnectTimeout(Duration.ofMillis(connectTimeout))
                .setReadTimeout(Duration.ofMillis(readTimeout))
                .build();
    }
}