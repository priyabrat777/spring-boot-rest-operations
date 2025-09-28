package com.enterprise.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Basic integration test to verify the Spring Boot application context loads correctly.
 */
@SpringBootTest
@ActiveProfiles("test")
class EnterpriseApiApplicationTests {

    @Test
    void contextLoads() {
        // This test verifies that the Spring application context loads successfully
        // with all configurations and dependencies properly wired
    }
}