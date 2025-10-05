package com.enterprise.api.monitoring;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ActuatorAvailabilityTest {

    @Test
    void actuatorHealthClassesShouldBeAvailable() {
        try {
            Class<?> healthClass = Class.forName("org.springframework.boot.actuator.health.Health");
            Class<?> healthIndicatorClass = Class.forName("org.springframework.boot.actuator.health.HealthIndicator");
            
            assertThat(healthClass).isNotNull();
            assertThat(healthIndicatorClass).isNotNull();
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Actuator health classes not found: " + e.getMessage());
        }
    }
}