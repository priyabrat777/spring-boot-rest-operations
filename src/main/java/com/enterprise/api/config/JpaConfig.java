package com.enterprise.api.config;

import com.enterprise.api.audit.AuditAware;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * JPA configuration class that enables auditing and transaction management.
 * 
 * This configuration:
 * - Enables JPA auditing with automatic timestamp and auditor population
 * - Configures the base package for JPA repositories
 * - Enables transaction management
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditAware")
@EnableJpaRepositories(basePackages = "com.enterprise.api.repository")
@EnableTransactionManagement
public class JpaConfig {

    /**
     * Configures the AuditorAware bean for JPA auditing.
     * This bean provides the current user information for audit fields.
     * 
     * @return AuditorAware implementation
     */
    @Bean
    public AuditorAware<String> auditAware() {
        return new AuditAware();
    }

    /**
     * Configures ObjectMapper for JSON serialization in audit logs.
     * 
     * @return ObjectMapper instance
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}