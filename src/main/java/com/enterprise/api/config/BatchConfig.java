package com.enterprise.api.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring Batch configuration class that sets up the batch processing infrastructure.
 * 
 * This configuration:
 * - Enables Spring Batch processing
 * - Configures job repository and transaction manager
 * - Sets up job launcher with async execution
 * - Provides base configuration for batch jobs
 * 
 * Requirements: 6.1, 6.5, 6.6
 */
@Configuration
@EnableBatchProcessing
public class BatchConfig extends DefaultBatchConfiguration {

    private final DataSource dataSource;

    public BatchConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Configures the transaction manager for batch operations.
     * Uses DataSourceTransactionManager for database transactions.
     * 
     * @return PlatformTransactionManager for batch operations
     */
    @Override
    @Bean("batchTransactionManager")
    public PlatformTransactionManager getTransactionManager() {
        return new DataSourceTransactionManager(dataSource);
    }

    /**
     * Configures the job launcher with async task executor.
     * This allows batch jobs to run asynchronously.
     * 
     * @param jobRepository the job repository
     * @return JobLauncher configured for async execution
     * @throws Exception if configuration fails
     */
    @Bean
    public JobLauncher asyncJobLauncher(JobRepository jobRepository) throws Exception {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(batchTaskExecutor());
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }

    /**
     * Configures the task executor for batch job execution.
     * Uses SimpleAsyncTaskExecutor for asynchronous processing.
     * 
     * @return TaskExecutor for batch jobs
     */
    @Bean
    public TaskExecutor batchTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setConcurrencyLimit(5); // Limit concurrent batch jobs
        executor.setThreadNamePrefix("batch-");
        return executor;
    }

    /**
     * Creates a job registry map that contains all available batch jobs.
     * This registry is used by the BatchController and BatchJobMonitoringService
     * to look up jobs by name.
     * 
     * @param userDataProcessingJob the user data processing job
     * @param userDataProcessingByRoleJob the user data processing by role job
     * @param recentUserDataProcessingJob the recent user data processing job
     * @return Map of job names to Job instances
     */
    @Bean("customJobRegistry")
    public Map<String, Job> customJobRegistry(
            Job userDataProcessingJob,
            Job userDataProcessingByRoleJob,
            Job recentUserDataProcessingJob) {
        Map<String, Job> registry = new HashMap<>();
        registry.put("userDataProcessingJob", userDataProcessingJob);
        registry.put("userDataProcessingByRoleJob", userDataProcessingByRoleJob);
        registry.put("recentUserDataProcessingJob", recentUserDataProcessingJob);
        return registry;
    }
}