package com.enterprise.api.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * Test configuration to provide required beans for testing.
 */
@TestConfiguration
@Profile("test")
public class TestConfig {
    
    /**
     * Test auditor that provides a fixed user for audit fields in tests.
     */
    @Bean
    public AuditorAware<String> testAuditorAware() {
        return () -> Optional.of("TEST_USER");
    }

    /**
     * Mock RestTemplate for email service in tests.
     */
    @Bean("emailServiceRestTemplate")
    public RestTemplate emailServiceRestTemplate(RestTemplateBuilder builder) {
        return builder
                .rootUri("http://localhost:8080")
                .setConnectTimeout(Duration.ofMillis(5000))
                .setReadTimeout(Duration.ofMillis(10000))
                .build();
    }

    /**
     * Mock RestTemplate for SMS service in tests.
     */
    @Bean("smsServiceRestTemplate")
    public RestTemplate smsServiceRestTemplate(RestTemplateBuilder builder) {
        return builder
                .rootUri("http://localhost:8080")
                .setConnectTimeout(Duration.ofMillis(5000))
                .setReadTimeout(Duration.ofMillis(10000))
                .build();
    }

    /**
     * Mock RestTemplate for file storage service in tests.
     */
    @Bean("fileStorageServiceRestTemplate")
    public RestTemplate fileStorageServiceRestTemplate(RestTemplateBuilder builder) {
        return builder
                .rootUri("http://localhost:8080")
                .setConnectTimeout(Duration.ofMillis(5000))
                .setReadTimeout(Duration.ofMillis(10000))
                .build();
    }

    /**
     * Test-specific CORS configuration that allows all origins without credentials.
     */
    @Bean("testCorsConfigurationSource")
    public CorsConfigurationSource testCorsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow all origins for tests
        configuration.setAllowedOrigins(Arrays.asList("*"));
        
        // Allow all methods
        configuration.setAllowedMethods(Arrays.asList("*"));
        
        // Allow all headers
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Don't allow credentials to avoid CORS conflicts
        configuration.setAllowCredentials(false);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }

    /**
     * Test security configuration that permits all requests.
     * Note: This is commented out to allow proper security testing.
     * Individual tests can override security as needed.
     */
    // @Bean
    // @Primary
    // public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
    //     return http
    //             .csrf(AbstractHttpConfigurer::disable)
    //             .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
    //             .build();
    // }
}