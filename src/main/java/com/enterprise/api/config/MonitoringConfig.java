package com.enterprise.api.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Configuration for monitoring and observability features.
 * 
 * <p>This configuration sets up custom metrics, health indicators,
 * and info contributors for comprehensive application monitoring.</p>
 */
@Configuration
public class MonitoringConfig {

    /**
     * Configure HTTP exchange repository for tracking HTTP requests.
     */
    @Bean
    public HttpExchangeRepository httpExchangeRepository() {
        InMemoryHttpExchangeRepository repository = new InMemoryHttpExchangeRepository();
        repository.setCapacity(1000); // Store last 1000 HTTP exchanges
        return repository;
    }

    /**
     * Custom info contributor for application metadata.
     */
    @Bean
    public InfoContributor customInfoContributor() {
        return builder -> builder
                .withDetail("application", Map.of(
                        "name", "Enterprise Spring Boot API",
                        "description", "Comprehensive enterprise-level REST API",
                        "version", "1.0.0-SNAPSHOT",
                        "startup-time", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                ))
                .withDetail("features", Map.of(
                        "authentication", "JWT-based authentication",
                        "authorization", "Role-based access control (RBAC)",
                        "auditing", "Comprehensive audit trail",
                        "batch-processing", "Spring Batch integration",
                        "file-management", "Secure file upload/download",
                        "security-features", "CAPTCHA and OTP support"
                ))
                .withDetail("monitoring", Map.of(
                        "health-checks", "Custom health indicators",
                        "metrics", "Business and technical metrics",
                        "logging", "Structured logging with correlation IDs",
                        "actuator", "Spring Boot Actuator endpoints"
                ));
    }

    /**
     * Register JVM metrics.
     */
    @Bean
    public JvmMemoryMetrics jvmMemoryMetrics() {
        return new JvmMemoryMetrics();
    }

    @Bean
    public JvmGcMetrics jvmGcMetrics() {
        return new JvmGcMetrics();
    }

    @Bean
    public JvmThreadMetrics jvmThreadMetrics() {
        return new JvmThreadMetrics();
    }

    @Bean
    public ClassLoaderMetrics classLoaderMetrics() {
        return new ClassLoaderMetrics();
    }

    /**
     * Register system metrics.
     */
    @Bean
    public UptimeMetrics uptimeMetrics() {
        return new UptimeMetrics();
    }

    @Bean
    public ProcessorMetrics processorMetrics() {
        return new ProcessorMetrics();
    }

    @Bean
    public FileDescriptorMetrics fileDescriptorMetrics() {
        return new FileDescriptorMetrics();
    }
}