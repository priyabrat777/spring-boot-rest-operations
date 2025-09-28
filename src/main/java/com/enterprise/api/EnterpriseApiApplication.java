package com.enterprise.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main application class for the Enterprise Spring Boot API.
 * 
 * This application demonstrates enterprise-level REST API development with:
 * - Complete CRUD operations
 * - JWT-based security with RBAC
 * - Comprehensive auditing
 * - Spring Batch processing
 * - File upload/download capabilities
 * - CAPTCHA and OTP functionality
 * - Extensive testing coverage
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableTransactionManagement
public class EnterpriseApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnterpriseApiApplication.class, args);
    }
}