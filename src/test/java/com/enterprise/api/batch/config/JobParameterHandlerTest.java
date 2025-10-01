package com.enterprise.api.batch.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for JobParameterHandler.
 * Tests job parameter creation, validation, and extraction functionality.
 * 
 * Requirements: 6.5
 */
class JobParameterHandlerTest {

    private JobParameterHandler jobParameterHandler;

    @BeforeEach
    void setUp() {
        jobParameterHandler = new JobParameterHandler();
    }

    @Test
    void shouldCreateDefaultParameters() {
        // Given
        String user = "testUser";

        // When
        JobParameters parameters = jobParameterHandler.createDefaultParameters(user);

        // Then
        assertNotNull(parameters);
        assertThat(parameters.getString("user")).isEqualTo(user);
        assertThat(parameters.getLong("timestamp")).isNotNull();
        assertThat(parameters.getString("executionId")).isNotNull();
        assertThat(parameters.getString("executionId")).startsWith("exec_");
    }

    @Test
    void shouldCreateParametersWithCustomParams() {
        // Given
        String user = "testUser";
        Map<String, Object> customParams = new HashMap<>();
        customParams.put("batchSize", 100L);
        customParams.put("fileName", "test.csv");
        customParams.put("processingDate", LocalDateTime.now());
        customParams.put("threshold", 95.5);

        // When
        JobParameters parameters = jobParameterHandler.createParameters(user, customParams);

        // Then
        assertNotNull(parameters);
        assertThat(parameters.getString("user")).isEqualTo(user);
        assertThat(parameters.getLong("timestamp")).isNotNull();
        assertThat(parameters.getString("executionId")).isNotNull();
        assertThat(parameters.getLong("batchSize")).isEqualTo(100L);
        assertThat(parameters.getString("fileName")).isEqualTo("test.csv");
        assertThat(parameters.getDouble("threshold")).isEqualTo(95.5);
        assertThat(parameters.getLong("processingDate")).isNotNull();
    }

    @Test
    void shouldCreateParametersWithNullCustomParams() {
        // Given
        String user = "testUser";

        // When
        JobParameters parameters = jobParameterHandler.createParameters(user, null);

        // Then
        assertNotNull(parameters);
        assertThat(parameters.getString("user")).isEqualTo(user);
        assertThat(parameters.getLong("timestamp")).isNotNull();
        assertThat(parameters.getString("executionId")).isNotNull();
    }

    @Test
    void shouldValidateRequiredParameters() throws JobParametersInvalidException {
        // Given
        JobParameters parameters = new JobParametersBuilder()
                .addString("user", "testUser")
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        // When & Then - should not throw exception
        jobParameterHandler.validateRequiredParameters(parameters, "user", "timestamp");
    }

    @Test
    void shouldThrowExceptionForMissingRequiredParameter() {
        // Given
        JobParameters parameters = new JobParametersBuilder()
                .addString("user", "testUser")
                .toJobParameters();

        // When & Then
        assertThatThrownBy(() -> jobParameterHandler.validateRequiredParameters(parameters, "user", "timestamp"))
                .isInstanceOf(JobParametersInvalidException.class)
                .hasMessageContaining("Required parameter 'timestamp' is missing");
    }

    @Test
    void shouldGetStringParameterWithDefault() {
        // Given
        JobParameters parameters = new JobParametersBuilder()
                .addString("existingParam", "value")
                .toJobParameters();

        // When & Then
        assertThat(jobParameterHandler.getStringParameter(parameters, "existingParam", "default"))
                .isEqualTo("value");
        assertThat(jobParameterHandler.getStringParameter(parameters, "missingParam", "default"))
                .isEqualTo("default");
    }

    @Test
    void shouldGetLongParameterWithDefault() {
        // Given
        JobParameters parameters = new JobParametersBuilder()
                .addLong("existingParam", 123L)
                .toJobParameters();

        // When & Then
        assertThat(jobParameterHandler.getLongParameter(parameters, "existingParam", 456L))
                .isEqualTo(123L);
        assertThat(jobParameterHandler.getLongParameter(parameters, "missingParam", 456L))
                .isEqualTo(456L);
    }

    @Test
    void shouldGetDoubleParameterWithDefault() {
        // Given
        JobParameters parameters = new JobParametersBuilder()
                .addDouble("existingParam", 12.34)
                .toJobParameters();

        // When & Then
        assertThat(jobParameterHandler.getDoubleParameter(parameters, "existingParam", 56.78))
                .isEqualTo(12.34);
        assertThat(jobParameterHandler.getDoubleParameter(parameters, "missingParam", 56.78))
                .isEqualTo(56.78);
    }

    @Test
    void shouldCheckParameterExistence() {
        // Given
        JobParameters parameters = new JobParametersBuilder()
                .addString("existingParam", "value")
                .toJobParameters();

        // When & Then
        assertTrue(jobParameterHandler.hasParameter(parameters, "existingParam"));
        assertFalse(jobParameterHandler.hasParameter(parameters, "missingParam"));
    }

    @Test
    void shouldConvertParametersToString() {
        // Given
        JobParameters parameters = new JobParametersBuilder()
                .addString("user", "testUser")
                .addLong("timestamp", 123456789L)
                .addDouble("threshold", 95.5)
                .toJobParameters();

        // When
        String result = jobParameterHandler.parametersToString(parameters);

        // Then
        assertNotNull(result);
        assertThat(result).contains("user=testUser");
        assertThat(result).contains("timestamp=123456789");
        assertThat(result).contains("threshold=95.5");
    }

    @Test
    void shouldHandleEmptyParametersToString() {
        // Given
        JobParameters parameters = new JobParametersBuilder().toJobParameters();

        // When
        String result = jobParameterHandler.parametersToString(parameters);

        // Then
        assertThat(result).isEmpty();
    }
}