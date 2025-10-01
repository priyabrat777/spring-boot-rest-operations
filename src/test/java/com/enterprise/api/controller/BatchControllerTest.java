package com.enterprise.api.controller;

import com.enterprise.api.batch.config.JobParameterHandler;
import com.enterprise.api.batch.monitoring.BatchJobMonitoringService;
import com.enterprise.api.batch.monitoring.JobExecutionStatus;
import com.enterprise.api.batch.monitoring.JobStatistics;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for BatchController.
 * Tests batch job management endpoints.
 * 
 * Requirements: 6.4
 */
@WebMvcTest(BatchController.class)
class BatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JobLauncher jobLauncher;

    @MockBean
    private BatchJobMonitoringService monitoringService;

    @MockBean
    private JobParameterHandler parameterHandler;

    @MockBean
    private Map<String, Job> jobRegistry;

    @MockBean
    private Job testJob;

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
                testJobParameters
        );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testStartJob() throws Exception {
        // Given
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("outputDirectory", "test-output");

        when(jobRegistry.get("testJob")).thenReturn(testJob);
        when(monitoringService.isJobRunning("testJob")).thenReturn(false);
        when(parameterHandler.createParameters(eq("user"), anyMap())).thenReturn(testJobParameters);
        when(jobLauncher.run(testJob, testJobParameters)).thenReturn(testJobExecution);

        // When & Then
        mockMvc.perform(post("/api/v1/batch/jobs/testJob/start")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(parameters)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobExecutionId").value(1L))
                .andExpect(jsonPath("$.jobName").value("testJob"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testStartJobNotFound() throws Exception {
        // Given
        when(jobRegistry.get("nonExistentJob")).thenReturn(null);

        // When & Then
        mockMvc.perform(post("/api/v1/batch/jobs/nonExistentJob/start")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testStartJobAlreadyRunning() throws Exception {
        // Given
        when(jobRegistry.get("testJob")).thenReturn(testJob);
        when(monitoringService.isJobRunning("testJob")).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/v1/batch/jobs/testJob/start")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Job is already running"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testStartJobWithInvalidParameters() throws Exception {
        // Given
        when(jobRegistry.get("testJob")).thenReturn(testJob);
        when(monitoringService.isJobRunning("testJob")).thenReturn(false);
        when(parameterHandler.createParameters(eq("user"), anyMap())).thenReturn(testJobParameters);
        when(jobLauncher.run(testJob, testJobParameters))
                .thenThrow(new JobParametersInvalidException("Invalid parameters"));

        // When & Then
        mockMvc.perform(post("/api/v1/batch/jobs/testJob/start")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid job parameters: Invalid parameters"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testStopJobExecution() throws Exception {
        // Given
        when(monitoringService.stopJobExecution(1L)).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/v1/batch/executions/1/stop")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId").value(1L))
                .andExpect(jsonPath("$.stopped").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testStopJobExecutionNotFound() throws Exception {
        // Given
        when(monitoringService.stopJobExecution(999L)).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/api/v1/batch/executions/999/stop")
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.stopped").value(false))
                .andExpect(jsonPath("$.error").value("Job execution not found or not running"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testRestartJobExecution() throws Exception {
        // Given
        JobExecution newJobExecution = new JobExecution(testJobExecution.getJobInstance(), 2L, testJobParameters);
        newJobExecution.setStatus(BatchStatus.STARTED);
        newJobExecution.setStartTime(LocalDateTime.now());

        when(monitoringService.restartJobExecution(1L)).thenReturn(newJobExecution);

        // When & Then
        mockMvc.perform(post("/api/v1/batch/executions/1/restart")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalExecutionId").value(1L))
                .andExpect(jsonPath("$.newExecutionId").value(2L))
                .andExpect(jsonPath("$.status").value("STARTED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testRestartJobExecutionNotFound() throws Exception {
        // Given
        when(monitoringService.restartJobExecution(999L))
                .thenThrow(new NoSuchJobException("Job not found"));

        // When & Then
        mockMvc.perform(post("/api/v1/batch/executions/999/restart")
                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "BATCH_VIEWER")
    void testGetJobExecutionStatus() throws Exception {
        // Given
        when(monitoringService.getJobExecutionStatus(1L)).thenReturn(Optional.of(testJobExecutionStatus));

        // When & Then
        mockMvc.perform(get("/api/v1/batch/executions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId").value(1L))
                .andExpect(jsonPath("$.jobName").value("testJob"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser(roles = "BATCH_VIEWER")
    void testGetJobExecutionStatusNotFound() throws Exception {
        // Given
        when(monitoringService.getJobExecutionStatus(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/v1/batch/executions/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "BATCH_VIEWER")
    void testGetJobExecutions() throws Exception {
        // Given
        List<JobExecutionStatus> executions = List.of(testJobExecutionStatus);
        when(monitoringService.getJobExecutions("testJob", 10)).thenReturn(executions);

        // When & Then
        mockMvc.perform(get("/api/v1/batch/jobs/testJob/executions")
                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].jobName").value("testJob"));
    }

    @Test
    @WithMockUser(roles = "BATCH_VIEWER")
    void testGetRunningJobExecutions() throws Exception {
        // Given
        JobExecutionStatus runningStatus = new JobExecutionStatus(
                2L, "runningJob", BatchStatus.STARTED, ExitStatus.EXECUTING,
                LocalDateTime.now().minusMinutes(2), null, testJobParameters
        );
        List<JobExecutionStatus> runningExecutions = List.of(runningStatus);
        when(monitoringService.getRunningJobExecutions()).thenReturn(runningExecutions);

        // When & Then
        mockMvc.perform(get("/api/v1/batch/executions/running"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].status").value("STARTED"));
    }

    @Test
    @WithMockUser(roles = "BATCH_VIEWER")
    void testGetJobStatistics() throws Exception {
        // Given
        JobStatistics statistics = new JobStatistics("testJob", 10, 8, 1, 1, 5000.0);
        when(monitoringService.getJobStatistics("testJob")).thenReturn(statistics);

        // When & Then
        mockMvc.perform(get("/api/v1/batch/jobs/testJob/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobName").value("testJob"))
                .andExpect(jsonPath("$.totalExecutions").value(10))
                .andExpect(jsonPath("$.completedExecutions").value(8))
                .andExpect(jsonPath("$.failedExecutions").value(1))
                .andExpect(jsonPath("$.runningExecutions").value(1));
    }

    @Test
    @WithMockUser(roles = "BATCH_VIEWER")
    void testGetAvailableJobs() throws Exception {
        // Given
        Set<String> jobNames = Set.of("testJob", "anotherJob", "userDataProcessingJob");
        when(monitoringService.getAvailableJobNames()).thenReturn(jobNames);

        // When & Then
        mockMvc.perform(get("/api/v1/batch/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testUnauthorizedAccess() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/batch/jobs/testJob/start")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUnauthenticatedAccess() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/batch/jobs"))
                .andExpect(status().isUnauthorized());
    }
}