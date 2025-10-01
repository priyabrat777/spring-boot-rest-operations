package com.enterprise.api.batch.config;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * Utility class for handling job parameters in Spring Batch jobs.
 * Provides methods for creating, validating, and extracting job parameters.
 * 
 * This class handles:
 * - Parameter creation and validation
 * - Type-safe parameter extraction
 * - Default parameter generation
 * - Parameter conversion utilities
 * 
 * Requirements: 6.5
 */
@Component
public class JobParameterHandler {

    /**
     * Creates job parameters with timestamp and user information.
     * 
     * @param user the user executing the job
     * @return JobParameters with timestamp and user
     */
    public JobParameters createDefaultParameters(String user) {
        return new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("user", user)
                .addString("executionId", generateExecutionId())
                .toJobParameters();
    }

    /**
     * Creates job parameters with custom parameters.
     * 
     * @param user the user executing the job
     * @param customParams additional custom parameters
     * @return JobParameters with default and custom parameters
     */
    public JobParameters createParameters(String user, Map<String, Object> customParams) {
        JobParametersBuilder builder = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("user", user)
                .addString("executionId", generateExecutionId());

        // Add custom parameters
        if (customParams != null) {
            customParams.forEach((key, value) -> {
                if (value instanceof String) {
                    builder.addString(key, (String) value);
                } else if (value instanceof Long) {
                    builder.addLong(key, (Long) value);
                } else if (value instanceof Double) {
                    builder.addDouble(key, (Double) value);
                } else if (value instanceof LocalDateTime) {
                    builder.addLong(key, ((LocalDateTime) value).toEpochSecond(ZoneOffset.UTC));
                } else {
                    builder.addString(key, value.toString());
                }
            });
        }

        return builder.toJobParameters();
    }

    /**
     * Validates required parameters are present.
     * 
     * @param parameters the job parameters to validate
     * @param requiredParams array of required parameter names
     * @throws JobParametersInvalidException if required parameters are missing
     */
    public void validateRequiredParameters(JobParameters parameters, String... requiredParams) 
            throws JobParametersInvalidException {
        for (String param : requiredParams) {
            if (!parameters.getParameters().containsKey(param)) {
                throw new JobParametersInvalidException("Required parameter '" + param + "' is missing");
            }
        }
    }

    /**
     * Extracts a string parameter with default value.
     * 
     * @param parameters the job parameters
     * @param key the parameter key
     * @param defaultValue the default value if parameter is not found
     * @return the parameter value or default value
     */
    public String getStringParameter(JobParameters parameters, String key, String defaultValue) {
        String value = parameters.getString(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Extracts a long parameter with default value.
     * 
     * @param parameters the job parameters
     * @param key the parameter key
     * @param defaultValue the default value if parameter is not found
     * @return the parameter value or default value
     */
    public Long getLongParameter(JobParameters parameters, String key, Long defaultValue) {
        Long value = parameters.getLong(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Extracts a double parameter with default value.
     * 
     * @param parameters the job parameters
     * @param key the parameter key
     * @param defaultValue the default value if parameter is not found
     * @return the parameter value or default value
     */
    public Double getDoubleParameter(JobParameters parameters, String key, Double defaultValue) {
        Double value = parameters.getDouble(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Checks if a parameter exists.
     * 
     * @param parameters the job parameters
     * @param key the parameter key
     * @return true if parameter exists, false otherwise
     */
    public boolean hasParameter(JobParameters parameters, String key) {
        return parameters.getParameters().containsKey(key);
    }

    /**
     * Converts job parameters to a readable string format.
     * 
     * @param parameters the job parameters
     * @return formatted string representation
     */
    public String parametersToString(JobParameters parameters) {
        StringBuilder sb = new StringBuilder();
        parameters.getParameters().forEach((key, value) -> {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(key).append("=").append(value.getValue());
        });
        return sb.toString();
    }

    /**
     * Generates a unique execution ID for job instances.
     * 
     * @return unique execution ID
     */
    private String generateExecutionId() {
        return "exec_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId();
    }
}