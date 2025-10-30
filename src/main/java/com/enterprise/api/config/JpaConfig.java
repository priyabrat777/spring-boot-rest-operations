package com.enterprise.api.config;

import com.enterprise.api.audit.AuditAware;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;

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
     * Primary transaction manager for JPA operations.
     * This is the default transaction manager used by @Transactional.
     * 
     * @param entityManagerFactory the entity manager factory
     * @return JPA transaction manager
     */
    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}