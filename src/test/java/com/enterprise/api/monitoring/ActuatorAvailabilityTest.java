package com.enterprise.api.monitoring;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to verify that Spring Boot Actuator classes are available on the
 * classpath.
 * This is a simple unit test that doesn't require Spring context loading.
 */
class ActuatorAvailabilityTest {

    @Test
    void actuatorHealthClassesShouldBeAvailable() {
        try {
            Class<?> healthClass = Class.forName("org.springframework.boot.actuate.health.Health");
            Class<?> healthIndicatorClass = Class.forName("org.springframework.boot.actuate.health.HealthIndicator");

            assertThat(healthClass).isNotNull();
            assertThat(healthIndicatorClass).isNotNull();
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Actuator health classes not found: " + e.getMessage());
        }
    }
}