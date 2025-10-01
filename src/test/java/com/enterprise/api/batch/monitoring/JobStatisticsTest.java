package com.enterprise.api.batch.monitoring;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for JobStatistics record.
 * Tests job statistics calculations and formatting.
 * 
 * Requirements: 6.6
 */
class JobStatisticsTest {

    @Test
    void shouldCalculateSuccessRate() {
        // Given
        JobStatistics statistics = new JobStatistics(
                "testJob", 10, 8, 2, 0, 5000.0
        );

        // When
        double successRate = statistics.getSuccessRate();

        // Then
        assertThat(successRate).isEqualTo(80.0);
    }

    @Test
    void shouldCalculateFailureRate() {
        // Given
        JobStatistics statistics = new JobStatistics(
                "testJob", 10, 8, 2, 0, 5000.0
        );

        // When
        double failureRate = statistics.getFailureRate();

        // Then
        assertThat(failureRate).isEqualTo(20.0);
    }

    @Test
    void shouldHandleZeroTotalExecutions() {
        // Given
        JobStatistics statistics = new JobStatistics(
                "testJob", 0, 0, 0, 0, 0.0
        );

        // When & Then
        assertThat(statistics.getSuccessRate()).isEqualTo(0.0);
        assertThat(statistics.getFailureRate()).isEqualTo(0.0);
    }

    @Test
    void shouldCalculateFinishedExecutions() {
        // Given
        JobStatistics statistics = new JobStatistics(
                "testJob", 15, 10, 3, 2, 5000.0
        );

        // When
        long finishedExecutions = statistics.getFinishedExecutions();

        // Then
        assertThat(finishedExecutions).isEqualTo(13); // 10 completed + 3 failed
    }

    @Test
    void shouldFormatAverageExecutionTimeInMilliseconds() {
        // Given
        JobStatistics statistics = new JobStatistics(
                "testJob", 10, 8, 2, 0, 500.0
        );

        // When
        String formattedTime = statistics.getFormattedAverageExecutionTime();

        // Then
        assertThat(formattedTime).isEqualTo("500 ms");
    }

    @Test
    void shouldFormatAverageExecutionTimeInSeconds() {
        // Given
        JobStatistics statistics = new JobStatistics(
                "testJob", 10, 8, 2, 0, 5500.0
        );

        // When
        String formattedTime = statistics.getFormattedAverageExecutionTime();

        // Then
        assertThat(formattedTime).isEqualTo("5.5 seconds");
    }

    @Test
    void shouldFormatAverageExecutionTimeInMinutes() {
        // Given
        JobStatistics statistics = new JobStatistics(
                "testJob", 10, 8, 2, 0, 125000.0 // 2 minutes 5 seconds
        );

        // When
        String formattedTime = statistics.getFormattedAverageExecutionTime();

        // Then
        assertThat(formattedTime).isEqualTo("2.1 minutes");
    }

    @Test
    void shouldCreateSummaryString() {
        // Given
        JobStatistics statistics = new JobStatistics(
                "testJob", 10, 8, 2, 1, 5500.0
        );

        // When
        String summary = statistics.getSummary();

        // Then
        assertThat(summary).contains("Job: testJob");
        assertThat(summary).contains("Total: 10");
        assertThat(summary).contains("Completed: 8 (80.0%)");
        assertThat(summary).contains("Failed: 2 (20.0%)");
        assertThat(summary).contains("Running: 1");
        assertThat(summary).contains("Avg Time: 5.5 seconds");
    }

    @Test
    void shouldHandleZeroAverageExecutionTime() {
        // Given
        JobStatistics statistics = new JobStatistics(
                "testJob", 5, 3, 2, 0, 0.0
        );

        // When
        String formattedTime = statistics.getFormattedAverageExecutionTime();

        // Then
        assertThat(formattedTime).isEqualTo("0 ms");
    }

    @Test
    void shouldHandlePerfectSuccessRate() {
        // Given
        JobStatistics statistics = new JobStatistics(
                "testJob", 5, 5, 0, 0, 1000.0
        );

        // When & Then
        assertThat(statistics.getSuccessRate()).isEqualTo(100.0);
        assertThat(statistics.getFailureRate()).isEqualTo(0.0);
    }

    @Test
    void shouldHandlePerfectFailureRate() {
        // Given
        JobStatistics statistics = new JobStatistics(
                "testJob", 5, 0, 5, 0, 1000.0
        );

        // When & Then
        assertThat(statistics.getSuccessRate()).isEqualTo(0.0);
        assertThat(statistics.getFailureRate()).isEqualTo(100.0);
    }
}