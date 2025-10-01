package com.enterprise.api.batch.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Base configuration class for Spring Batch jobs.
 * Provides common functionality and utilities for job configuration.
 * 
 * This class provides:
 * - Common job building patterns
 * - Parameter validation utilities
 * - Step configuration helpers
 * - Error handling patterns
 * 
 * Requirements: 6.1, 6.5
 */
public abstract class BaseJobConfig {

    protected final JobRepository jobRepository;
    protected final PlatformTransactionManager transactionManager;

    protected BaseJobConfig(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
    }

    /**
     * Creates a job builder with common configuration.
     * 
     * @param jobName the name of the job
     * @return JobBuilder with base configuration
     */
    protected JobBuilder createJobBuilder(String jobName) {
        return new JobBuilder(jobName, jobRepository)
                .validator(createJobParametersValidator());
    }

    /**
     * Creates a step builder with common configuration.
     * 
     * @param stepName the name of the step
     * @return StepBuilder with base configuration
     */
    protected StepBuilder createStepBuilder(String stepName) {
        return new StepBuilder(stepName, jobRepository);
    }

    /**
     * Creates a job with restart capability.
     * 
     * @param jobName the name of the job
     * @param steps the steps to include in the job
     * @return Job configured with restart capability
     */
    protected Job createRestartableJob(String jobName, Step... steps) {
        if (steps.length == 0) {
            throw new IllegalArgumentException("At least one step is required for a job");
        }
        
        var jobBuilder = new JobBuilder(jobName, jobRepository)
                .validator(createJobParametersValidator())
                .start(steps[0]);
        
        for (int i = 1; i < steps.length; i++) {
            jobBuilder = jobBuilder.next(steps[i]);
        }
        
        return jobBuilder.build();
    }

    /**
     * Creates a simple job that prevents restart.
     * 
     * @param jobName the name of the job
     * @param steps the steps to include in the job
     * @return Job configured to prevent restart
     */
    protected Job createSimpleJob(String jobName, Step... steps) {
        if (steps.length == 0) {
            throw new IllegalArgumentException("At least one step is required for a job");
        }
        
        var jobBuilder = new JobBuilder(jobName, jobRepository)
                .validator(createJobParametersValidator())
                .preventRestart()
                .start(steps[0]);
        
        for (int i = 1; i < steps.length; i++) {
            jobBuilder = jobBuilder.next(steps[i]);
        }
        
        return jobBuilder.build();
    }

    /**
     * Creates a default job parameters validator.
     * Can be overridden by subclasses for custom validation.
     * 
     * @return JobParametersValidator implementation
     */
    protected JobParametersValidator createJobParametersValidator() {
        return new DefaultJobParametersValidator();
    }

    /**
     * Default implementation of JobParametersValidator.
     * Validates common parameters like timestamp and user.
     */
    protected static class DefaultJobParametersValidator implements JobParametersValidator {
        
        @Override
        public void validate(JobParameters parameters) throws JobParametersInvalidException {
            // Validate timestamp parameter if present
            if (parameters.getParameters().containsKey("timestamp")) {
                Long timestamp = parameters.getLong("timestamp");
                if (timestamp == null || timestamp <= 0) {
                    throw new JobParametersInvalidException("Invalid timestamp parameter");
                }
            }
            
            // Validate user parameter if present
            if (parameters.getParameters().containsKey("user")) {
                String user = parameters.getString("user");
                if (user == null || user.trim().isEmpty()) {
                    throw new JobParametersInvalidException("User parameter cannot be empty");
                }
            }
            
            // Additional validation can be added here
            validateCustomParameters(parameters);
        }
        
        /**
         * Hook for subclasses to add custom parameter validation.
         * 
         * @param parameters the job parameters to validate
         * @throws JobParametersInvalidException if validation fails
         */
        protected void validateCustomParameters(JobParameters parameters) throws JobParametersInvalidException {
            // Override in subclasses for custom validation
        }
    }
}