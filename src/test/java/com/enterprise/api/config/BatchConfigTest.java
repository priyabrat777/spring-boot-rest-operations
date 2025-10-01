package com.enterprise.api.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for BatchConfig.
 * Tests the Spring Batch infrastructure configuration.
 * 
 * Requirements: 6.1, 6.5, 6.6
 */
@ExtendWith(MockitoExtension.class)
class BatchConfigTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private JobRepository jobRepository;

    @Test
    void shouldCreateBatchConfig() {
        // Given
        BatchConfig batchConfig = new BatchConfig(dataSource);

        // When & Then
        assertNotNull(batchConfig);
    }

    @Test
    void shouldCreateTransactionManager() {
        // Given
        BatchConfig batchConfig = new BatchConfig(dataSource);

        // When
        PlatformTransactionManager transactionManager = batchConfig.getTransactionManager();

        // Then
        assertNotNull(transactionManager);
        assertThat(transactionManager).isInstanceOf(org.springframework.jdbc.datasource.DataSourceTransactionManager.class);
    }

    @Test
    void shouldCreateAsyncJobLauncher() throws Exception {
        // Given
        BatchConfig batchConfig = new BatchConfig(dataSource);

        // When
        JobLauncher jobLauncher = batchConfig.asyncJobLauncher(jobRepository);

        // Then
        assertNotNull(jobLauncher);
        assertThat(jobLauncher).isInstanceOf(TaskExecutorJobLauncher.class);
    }

    @Test
    void shouldCreateBatchTaskExecutor() {
        // Given
        BatchConfig batchConfig = new BatchConfig(dataSource);

        // When
        TaskExecutor taskExecutor = batchConfig.batchTaskExecutor();

        // Then
        assertNotNull(taskExecutor);
        assertThat(taskExecutor).isInstanceOf(org.springframework.core.task.SimpleAsyncTaskExecutor.class);
    }
}