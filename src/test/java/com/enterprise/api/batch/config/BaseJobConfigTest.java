package com.enterprise.api.batch.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * Unit tests for BaseJobConfig.
 * Tests the base job configuration functionality.
 * 
 * Requirements: 6.1, 6.5
 */
@ExtendWith(MockitoExtension.class)
class BaseJobConfigTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private Step mockStep;

    private TestBaseJobConfig baseJobConfig;

    @BeforeEach
    void setUp() {
        baseJobConfig = new TestBaseJobConfig(jobRepository, transactionManager);
    }

    @Test
    void shouldCreateJobBuilder() {
        // When
        JobBuilder jobBuilder = baseJobConfig.createJobBuilder("testJob");

        // Then
        assertNotNull(jobBuilder);
    }

    @Test
    void shouldCreateStepBuilder() {
        // When
        StepBuilder stepBuilder = baseJobConfig.createStepBuilder("testStep");

        // Then
        assertNotNull(stepBuilder);
    }

    @Test
    void shouldCreateRestartableJob() {
        // When
        Job job = baseJobConfig.createRestartableJob("restartableJob", mockStep);

        // Then
        assertNotNull(job);
        assertThat(job.getName()).isEqualTo("restartableJob");
        assertThat(job.isRestartable()).isTrue();
    }

    @Test
    void shouldCreateSimpleJob() {
        // When
        Job job = baseJobConfig.createSimpleJob("simpleJob", mockStep);

        // Then
        assertNotNull(job);
        assertThat(job.getName()).isEqualTo("simpleJob");
        assertThat(job.isRestartable()).isFalse();
    }

    @Test
    void shouldCreateJobParametersValidator() {
        // When
        JobParametersValidator validator = baseJobConfig.createJobParametersValidator();

        // Then
        assertNotNull(validator);
        assertThat(validator).isInstanceOf(BaseJobConfig.DefaultJobParametersValidator.class);
    }

    @Test
    void shouldValidateValidParameters() throws JobParametersInvalidException {
        // Given
        JobParametersValidator validator = baseJobConfig.createJobParametersValidator();
        JobParameters parameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("user", "testUser")
                .toJobParameters();

        // When & Then - should not throw exception
        validator.validate(parameters);
    }

    @Test
    void shouldRejectInvalidTimestamp() {
        // Given
        JobParametersValidator validator = baseJobConfig.createJobParametersValidator();
        JobParameters parameters = new JobParametersBuilder()
                .addLong("timestamp", -1L)
                .toJobParameters();

        // When & Then
        assertThatThrownBy(() -> validator.validate(parameters))
                .isInstanceOf(JobParametersInvalidException.class)
                .hasMessageContaining("Invalid timestamp parameter");
    }

    @Test
    void shouldRejectEmptyUser() {
        // Given
        JobParametersValidator validator = baseJobConfig.createJobParametersValidator();
        JobParameters parameters = new JobParametersBuilder()
                .addString("user", "")
                .toJobParameters();

        // When & Then
        assertThatThrownBy(() -> validator.validate(parameters))
                .isInstanceOf(JobParametersInvalidException.class)
                .hasMessageContaining("User parameter cannot be empty");
    }

    @Test
    void shouldRejectNullUser() {
        // Given
        JobParametersValidator validator = baseJobConfig.createJobParametersValidator();
        // In Spring Batch 5.x, we can't add null values, so we test with empty string
        JobParameters parameters = new JobParametersBuilder()
                .addString("user", "")
                .toJobParameters();

        // When & Then
        assertThatThrownBy(() -> validator.validate(parameters))
                .isInstanceOf(JobParametersInvalidException.class)
                .hasMessageContaining("User parameter cannot be empty");
    }

    /**
     * Test implementation of BaseJobConfig for testing purposes.
     */
    private static class TestBaseJobConfig extends BaseJobConfig {
        
        public TestBaseJobConfig(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
            super(jobRepository, transactionManager);
        }

        // Expose protected methods for testing
        @Override
        public JobBuilder createJobBuilder(String jobName) {
            return super.createJobBuilder(jobName);
        }

        @Override
        public StepBuilder createStepBuilder(String stepName) {
            return super.createStepBuilder(stepName);
        }

        @Override
        public Job createRestartableJob(String jobName, Step... steps) {
            return super.createRestartableJob(jobName, steps);
        }

        @Override
        public Job createSimpleJob(String jobName, Step... steps) {
            return super.createSimpleJob(jobName, steps);
        }

        @Override
        public JobParametersValidator createJobParametersValidator() {
            return super.createJobParametersValidator();
        }
    }
}