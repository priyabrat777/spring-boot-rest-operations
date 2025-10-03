package com.enterprise.api.batch.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DefaultJobParametersValidator.
 * Tests parameter validation logic with comprehensive coverage.
 */
class DefaultJobParametersValidatorTest {

    private BaseJobConfig.DefaultJobParametersValidator validator;

    @BeforeEach
    void setUp() {
        validator = new BaseJobConfig.DefaultJobParametersValidator();
    }

    @Test
    void validate_WithValidParameters_ShouldNotThrowException() {
        // Given
        JobParameters parameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("user", "testuser")
                .addString("otherParam", "value")
                .toJobParameters();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(parameters));
    }

    @Test
    void validate_WithEmptyParameters_ShouldNotThrowException() {
        // Given
        JobParameters parameters = new JobParametersBuilder().toJobParameters();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(parameters));
    }

    @Test
    void validate_WithValidTimestamp_ShouldNotThrowException() {
        // Given
        JobParameters parameters = new JobParametersBuilder()
                .addLong("timestamp", 1234567890L)
                .toJobParameters();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(parameters));
    }

    @Test
    void validate_WithNullTimestamp_ShouldThrowException() {
        // Given - Test with a custom JobParameters that returns null for getLong
        JobParameters parameters = new JobParameters() {
            @Override
            public Long getLong(String key) {
                if ("timestamp".equals(key)) {
                    return null;
                }
                return super.getLong(key);
            }
            
            @Override
            public Map<String, JobParameter<?>> getParameters() {
                Map<String, JobParameter<?>> params = new HashMap<>();
                params.put("timestamp", new JobParameter<>(1L, Long.class)); // Dummy parameter to make containsKey return true
                return params;
            }
        };

        // When & Then
        JobParametersInvalidException exception = assertThrows(
                JobParametersInvalidException.class,
                () -> validator.validate(parameters)
        );
        assertEquals("Invalid timestamp parameter", exception.getMessage());
    }

    @Test
    void validate_WithZeroTimestamp_ShouldThrowException() {
        // Given
        JobParameters parameters = new JobParametersBuilder()
                .addLong("timestamp", 0L)
                .toJobParameters();

        // When & Then
        JobParametersInvalidException exception = assertThrows(
                JobParametersInvalidException.class,
                () -> validator.validate(parameters)
        );
        assertEquals("Invalid timestamp parameter", exception.getMessage());
    }

    @Test
    void validate_WithNegativeTimestamp_ShouldThrowException() {
        // Given
        JobParameters parameters = new JobParametersBuilder()
                .addLong("timestamp", -1L)
                .toJobParameters();

        // When & Then
        JobParametersInvalidException exception = assertThrows(
                JobParametersInvalidException.class,
                () -> validator.validate(parameters)
        );
        assertEquals("Invalid timestamp parameter", exception.getMessage());
    }

    @Test
    void validate_WithValidUser_ShouldNotThrowException() {
        // Given
        JobParameters parameters = new JobParametersBuilder()
                .addString("user", "validuser")
                .toJobParameters();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(parameters));
    }

    @Test
    void validate_WithNullUser_ShouldThrowException() {
        // Given - Test with a custom JobParameters that returns null for getString
        JobParameters parameters = new JobParameters() {
            @Override
            public String getString(String key) {
                if ("user".equals(key)) {
                    return null;
                }
                return super.getString(key);
            }
            
            @Override
            public Map<String, JobParameter<?>> getParameters() {
                Map<String, JobParameter<?>> params = new HashMap<>();
                params.put("user", new JobParameter<>("dummy", String.class)); // Dummy parameter to make containsKey return true
                return params;
            }
        };

        // When & Then
        JobParametersInvalidException exception = assertThrows(
                JobParametersInvalidException.class,
                () -> validator.validate(parameters)
        );
        assertEquals("User parameter cannot be empty", exception.getMessage());
    }

    @Test
    void validate_WithEmptyUser_ShouldThrowException() {
        // Given
        JobParameters parameters = new JobParametersBuilder()
                .addString("user", "")
                .toJobParameters();

        // When & Then
        JobParametersInvalidException exception = assertThrows(
                JobParametersInvalidException.class,
                () -> validator.validate(parameters)
        );
        assertEquals("User parameter cannot be empty", exception.getMessage());
    }

    @Test
    void validate_WithWhitespaceOnlyUser_ShouldThrowException() {
        // Given
        JobParameters parameters = new JobParametersBuilder()
                .addString("user", "   ")
                .toJobParameters();

        // When & Then
        JobParametersInvalidException exception = assertThrows(
                JobParametersInvalidException.class,
                () -> validator.validate(parameters)
        );
        assertEquals("User parameter cannot be empty", exception.getMessage());
    }

    @Test
    void validate_WithUserContainingWhitespace_ShouldNotThrowException() {
        // Given
        JobParameters parameters = new JobParametersBuilder()
                .addString("user", "  validuser  ")
                .toJobParameters();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(parameters));
    }

    @Test
    void validate_WithBothInvalidTimestampAndUser_ShouldThrowTimestampException() {
        // Given - timestamp validation happens first
        JobParameters parameters = new JobParametersBuilder()
                .addLong("timestamp", -1L)
                .addString("user", "")
                .toJobParameters();

        // When & Then
        JobParametersInvalidException exception = assertThrows(
                JobParametersInvalidException.class,
                () -> validator.validate(parameters)
        );
        assertEquals("Invalid timestamp parameter", exception.getMessage());
    }

    @Test
    void validate_WithValidTimestampAndInvalidUser_ShouldThrowUserException() {
        // Given
        JobParameters parameters = new JobParametersBuilder()
                .addLong("timestamp", 1234567890L)
                .addString("user", "")
                .toJobParameters();

        // When & Then
        JobParametersInvalidException exception = assertThrows(
                JobParametersInvalidException.class,
                () -> validator.validate(parameters)
        );
        assertEquals("User parameter cannot be empty", exception.getMessage());
    }

    @Test
    void validate_WithOtherParametersOnly_ShouldNotThrowException() {
        // Given
        JobParameters parameters = new JobParametersBuilder()
                .addString("jobName", "testJob")
                .addString("environment", "test")
                .addLong("batchSize", 100L)
                .toJobParameters();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(parameters));
    }

    @Test
    void validate_WithMixedValidAndOtherParameters_ShouldNotThrowException() {
        // Given
        JobParameters parameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("user", "testuser")
                .addString("jobName", "testJob")
                .addString("environment", "test")
                .addLong("batchSize", 100L)
                .addDouble("version", 1.0)
                .toJobParameters();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(parameters));
    }

    /**
     * Test custom validator that extends DefaultJobParametersValidator
     */
    @Test
    void validate_WithCustomValidator_ShouldCallCustomValidation() {
        // Given
        CustomJobParametersValidator customValidator = new CustomJobParametersValidator();
        JobParameters parameters = new JobParametersBuilder()
                .addString("customParam", "invalidValue")
                .toJobParameters();

        // When & Then
        JobParametersInvalidException exception = assertThrows(
                JobParametersInvalidException.class,
                () -> customValidator.validate(parameters)
        );
        assertEquals("Custom validation failed", exception.getMessage());
    }

    /**
     * Custom validator for testing the extension mechanism
     */
    private static class CustomJobParametersValidator extends BaseJobConfig.DefaultJobParametersValidator {
        @Override
        protected void validateCustomParameters(JobParameters parameters) throws JobParametersInvalidException {
            if (parameters.getParameters().containsKey("customParam")) {
                String customParam = parameters.getString("customParam");
                if ("invalidValue".equals(customParam)) {
                    throw new JobParametersInvalidException("Custom validation failed");
                }
            }
        }
    }
}