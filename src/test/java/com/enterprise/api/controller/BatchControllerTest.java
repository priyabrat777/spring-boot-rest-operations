package com.enterprise.api.controller;

import com.enterprise.api.batch.config.JobParameterHandler;
import com.enterprise.api.batch.monitoring.BatchJobMonitoringService;
import com.enterprise.api.batch.monitoring.JobExecutionStatus;
import com.enterprise.api.batch.monitoring.JobStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for BatchController.
 * Tests batch job management endpoints.
 * 
 * Requirements: 6.4
 */
@ExtendWith(MockitoExtension.class)
class BatchControllerTest {

        @Mock
        private JobLauncher jobLauncher;

        @Mock
        private BatchJobMonitoringService monitoringService;

        @Mock
        private JobParameterHandler parameterHandler;

        @Mock
        private Map<String, Job> jobRegistry;

        @Mock
        private Job testJob;

        @Mock
        private Authentication authentication;

        @InjectMocks
        private BatchController batchController;

        private JobParameters testJobParameters;
        private JobExecution testJobExecution;
        private JobExecutionStatus testJobExecutionStatus;

        @BeforeEach
        void setUp() {
                testJobParameters = new JobParametersBuilder()
                                .addString("user", "test-user")
                                .addLong("timestamp", System.currentTimeMillis())
                                .toJobParameters();

                JobInstance jobInstance = new JobInstance(1L, "testJob");
                testJobExecution = new JobExecution(jobInstance, 1L, testJobParameters);
                testJobExecution.setStatus(BatchStatus.COMPLETED);
                testJobExecution.setStartTime(LocalDateTime.now().minusMinutes(5));
                testJobExecution.setEndTime(LocalDateTime.now());

                testJobExecutionStatus = new JobExecutionStatus(
                                1L,
                                "testJob",
                                BatchStatus.COMPLETED,
                                ExitStatus.COMPLETED,
                                LocalDateTime.now().minusMinutes(5),
                                LocalDateTime.now(),
                                testJobParameters);
        }

        @Test
        void testStartJob() throws Exception {
                // Given
                Map<String, Object> parameters = new HashMap<>();
                parameters.put("outputDirectory", "test-output");

                when(authentication.getName()).thenReturn("test-user");
                when(jobRegistry.get("testJob")).thenReturn(testJob);
                when(monitoringService.isJobRunning("testJob")).thenReturn(false);
                when(parameterHandler.createParameters(eq("test-user"), anyMap())).thenReturn(testJobParameters);
                when(jobLauncher.run(testJob, testJobParameters)).thenReturn(testJobExecution);

                // When
                ResponseEntity<Map<String, Object>> response = batchController.startJob("testJob", parameters,
                                authentication);

                // Then
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());
                assertEquals(1L, response.getBody().get("jobExecutionId"));
                assertEquals("testJob", response.getBody().get("jobName"));
                assertEquals("COMPLETED", response.getBody().get("status"));
        }

        @Test
        void testStartJobNotFound() throws Exception {
                // Given
                when(authentication.getName()).thenReturn("test-user");
                when(jobRegistry.get("nonExistentJob")).thenReturn(null);

                // When
                ResponseEntity<Map<String, Object>> response = batchController.startJob("nonExistentJob",
                                new HashMap<>(), authentication);

                // Then
                assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        void testStartJobAlreadyRunning() throws Exception {
                // Given
                when(authentication.getName()).thenReturn("test-user");
                when(jobRegistry.get("testJob")).thenReturn(testJob);
                when(monitoringService.isJobRunning("testJob")).thenReturn(true);

                // When
                ResponseEntity<Map<String, Object>> response = batchController.startJob("testJob", new HashMap<>(),
                                authentication);

                // Then
                assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
                assertNotNull(response.getBody());
                assertEquals("Job is already running", response.getBody().get("error"));
        }

        @Test
        void testStartJobWithInvalidParameters() throws Exception {
                // Given
                when(authentication.getName()).thenReturn("test-user");
                when(jobRegistry.get("testJob")).thenReturn(testJob);
                when(monitoringService.isJobRunning("testJob")).thenReturn(false);
                when(parameterHandler.createParameters(eq("test-user"), anyMap())).thenReturn(testJobParameters);
                when(jobLauncher.run(testJob, testJobParameters))
                                .thenThrow(new JobParametersInvalidException("Invalid parameters"));

                // When
                ResponseEntity<Map<String, Object>> response = batchController.startJob("testJob", new HashMap<>(),
                                authentication);

                // Then
                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                assertNotNull(response.getBody());
                assertEquals("Invalid job parameters: Invalid parameters", response.getBody().get("error"));
        }

        @Test
        void testStopJobExecution() throws Exception {
                // Given
                when(authentication.getName()).thenReturn("test-user");
                when(monitoringService.stopJobExecution(1L)).thenReturn(true);

                // When
                ResponseEntity<Map<String, Object>> response = batchController.stopJobExecution(1L, authentication);

                // Then
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());
                assertEquals(1L, response.getBody().get("executionId"));
                assertEquals(true, response.getBody().get("stopped"));
        }

        @Test
        void testStopJobExecutionNotFound() throws Exception {
                // Given
                when(authentication.getName()).thenReturn("test-user");
                when(monitoringService.stopJobExecution(999L)).thenReturn(false);

                // When
                ResponseEntity<Map<String, Object>> response = batchController.stopJobExecution(999L, authentication);

                // Then
                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                assertNotNull(response.getBody());
                assertEquals(false, response.getBody().get("stopped"));
                assertEquals("Job execution not found or not running", response.getBody().get("error"));
        }

        @Test
        void testRestartJobExecution() throws Exception {
                // Given
                when(authentication.getName()).thenReturn("test-user");
                JobExecution newJobExecution = new JobExecution(testJobExecution.getJobInstance(), 2L,
                                testJobParameters);
                newJobExecution.setStatus(BatchStatus.STARTED);
                newJobExecution.setStartTime(LocalDateTime.now());

                when(monitoringService.restartJobExecution(1L)).thenReturn(newJobExecution);

                // When
                ResponseEntity<Map<String, Object>> response = batchController.restartJobExecution(1L, authentication);

                // Then
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());
                assertEquals(1L, response.getBody().get("originalExecutionId"));
                assertEquals(2L, response.getBody().get("newExecutionId"));
                assertEquals("STARTED", response.getBody().get("status"));
        }

        @Test
        void testRestartJobExecutionNotFound() throws Exception {
                // Given
                when(authentication.getName()).thenReturn("test-user");
                when(monitoringService.restartJobExecution(999L))
                                .thenThrow(new NoSuchJobException("Job not found"));

                // When
                ResponseEntity<Map<String, Object>> response = batchController.restartJobExecution(999L,
                                authentication);

                // Then
                assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        void testGetJobExecutionStatus() throws Exception {
                // Given
                when(monitoringService.getJobExecutionStatus(1L)).thenReturn(Optional.of(testJobExecutionStatus));

                // When
                ResponseEntity<JobExecutionStatus> response = batchController.getJobExecutionStatus(1L);

                // Then
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());
                assertEquals(1L, response.getBody().executionId());
                assertEquals("testJob", response.getBody().jobName());
                assertEquals(BatchStatus.COMPLETED, response.getBody().status());
        }

        @Test
        void testGetJobExecutionStatusNotFound() throws Exception {
                // Given
                when(monitoringService.getJobExecutionStatus(999L)).thenReturn(Optional.empty());

                // When
                ResponseEntity<JobExecutionStatus> response = batchController.getJobExecutionStatus(999L);

                // Then
                assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        void testGetJobExecutions() throws Exception {
                // Given
                List<JobExecutionStatus> executions = List.of(testJobExecutionStatus);
                when(monitoringService.getJobExecutions("testJob", 10)).thenReturn(executions);

                // When
                ResponseEntity<List<JobExecutionStatus>> response = batchController.getJobExecutions("testJob", 10);

                // Then
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());
                assertEquals(1, response.getBody().size());
                assertEquals("testJob", response.getBody().get(0).jobName());
        }

        @Test
        void testGetRunningJobExecutions() throws Exception {
                // Given
                JobExecutionStatus runningStatus = new JobExecutionStatus(
                                2L, "runningJob", BatchStatus.STARTED, ExitStatus.EXECUTING,
                                LocalDateTime.now().minusMinutes(2), null, testJobParameters);
                List<JobExecutionStatus> runningExecutions = List.of(runningStatus);
                when(monitoringService.getRunningJobExecutions()).thenReturn(runningExecutions);

                // When
                ResponseEntity<List<JobExecutionStatus>> response = batchController.getRunningJobExecutions();

                // Then
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());
                assertEquals(1, response.getBody().size());
                assertEquals(BatchStatus.STARTED, response.getBody().get(0).status());
        }

        @Test
        void testGetJobStatistics() throws Exception {
                // Given
                JobStatistics statistics = new JobStatistics("testJob", 10, 8, 1, 1, 5000.0);
                when(monitoringService.getJobStatistics("testJob")).thenReturn(statistics);

                // When
                ResponseEntity<JobStatistics> response = batchController.getJobStatistics("testJob");

                // Then
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());
                assertEquals("testJob", response.getBody().jobName());
                assertEquals(10, response.getBody().totalExecutions());
                assertEquals(8, response.getBody().completedExecutions());
                assertEquals(1, response.getBody().failedExecutions());
                assertEquals(1, response.getBody().runningExecutions());
        }

        @Test
        void testGetAvailableJobs() throws Exception {
                // Given
                Set<String> jobNames = Set.of("testJob", "anotherJob", "userDataProcessingJob");
                when(monitoringService.getAvailableJobNames()).thenReturn(jobNames);

                // When
                ResponseEntity<Set<String>> response = batchController.getAvailableJobs();

                // Then
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());
                assertEquals(3, response.getBody().size());
                assertTrue(response.getBody().contains("testJob"));
                assertTrue(response.getBody().contains("anotherJob"));
                assertTrue(response.getBody().contains("userDataProcessingJob"));
        }
}