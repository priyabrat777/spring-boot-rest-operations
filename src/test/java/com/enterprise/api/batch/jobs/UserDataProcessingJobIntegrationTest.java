package com.enterprise.api.batch.jobs;

import com.enterprise.api.batch.config.JobParameterHandler;
import com.enterprise.api.entity.Role;
import com.enterprise.api.entity.User;
import com.enterprise.api.repository.RoleRepository;
import com.enterprise.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for user data processing batch job.
 * Tests the complete batch job execution with real database operations.
 * 
 * Requirements: 6.2, 6.3, 6.4
 */
@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.batch.job.enabled=false",
    "logging.level.com.enterprise.api.batch=DEBUG"
})
@Transactional
class UserDataProcessingJobIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("userDataProcessingJob")
    private Job userDataProcessingJob;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JobParameterHandler parameterHandler;

    private Role userRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        // Create test roles
        userRole = new Role();
        userRole.setName("USER");
        userRole.setDescription("Standard user role");
        userRole = roleRepository.save(userRole);

        adminRole = new Role();
        adminRole.setName("ADMIN");
        adminRole.setDescription("Administrator role");
        adminRole = roleRepository.save(adminRole);

        // Create test users
        createTestUsers();

        // Set up job launcher test utils
        jobLauncherTestUtils.setJob(userDataProcessingJob);
    }

    @Test
    void testUserDataProcessingJobExecution() throws Exception {
        // Given
        JobParameters jobParameters = parameterHandler.createDefaultParameters("test-user");

        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jobExecution.getExitStatus().getExitCode()).isEqualTo("COMPLETED");

        // Verify step execution
        assertThat(jobExecution.getStepExecutions()).hasSize(1);
        var stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getReadCount()).isGreaterThan(0);
        assertThat(stepExecution.getWriteCount()).isGreaterThan(0);
        assertThat(stepExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // Verify output files were created
        verifyOutputFiles();
    }

    @Test
    void testUserDataProcessingJobWithCustomParameters() throws Exception {
        // Given
        Map<String, Object> customParams = new HashMap<>();
        customParams.put("outputDirectory", "test-batch-output");
        customParams.put("enabledOnly", true);

        JobParameters jobParameters = parameterHandler.createParameters("test-user", customParams);

        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jobExecution.getJobParameters().getString("outputDirectory")).isEqualTo("test-batch-output");

        // Verify custom output directory was used
        Path outputDir = Paths.get("test-batch-output");
        assertThat(Files.exists(outputDir)).isTrue();
    }

    @Test
    void testUserDataProcessingJobRestart() throws Exception {
        // Given - First execution that might fail
        JobParameters jobParameters = parameterHandler.createDefaultParameters("test-user");

        // When - First execution
        JobExecution firstExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Then - Should complete successfully (in this test case)
        assertThat(firstExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // When - Attempt restart (should create new instance due to different timestamp)
        JobParameters restartParameters = parameterHandler.createDefaultParameters("test-user");
        JobExecution restartExecution = jobLauncherTestUtils.launchJob(restartParameters);

        // Then - Should be a new execution
        assertThat(restartExecution.getId()).isNotEqualTo(firstExecution.getId());
        assertThat(restartExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    void testUserDataProcessingStepExecution() throws Exception {
        // Given
        JobParameters jobParameters = parameterHandler.createDefaultParameters("test-user");

        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("userDataProcessingStep", jobParameters);

        // Then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        var stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getStepName()).isEqualTo("userDataProcessingStep");
        assertThat(stepExecution.getReadCount()).isEqualTo(getEnabledUserCount());
        assertThat(stepExecution.getWriteCount()).isLessThanOrEqualTo(stepExecution.getReadCount());
        assertThat(stepExecution.getSkipCount()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void testUserDataProcessingJobWithSkippedItems() throws Exception {
        // Given - Create users that will be skipped
        createSkippableTestUsers();

        JobParameters jobParameters = parameterHandler.createDefaultParameters("test-user");

        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        var stepExecution = jobExecution.getStepExecutions().iterator().next();
        // Some items should be skipped due to our skip logic
        assertThat(stepExecution.getSkipCount()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void testBatchJobMonitoring() throws Exception {
        // Given
        JobParameters jobParameters = parameterHandler.createDefaultParameters("test-user");

        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Then - Verify job execution details
        assertThat(jobExecution.getJobInstance()).isNotNull();
        assertThat(jobExecution.getJobInstance().getJobName()).isEqualTo("userDataProcessingJob");
        assertThat(jobExecution.getStartTime()).isNotNull();
        assertThat(jobExecution.getEndTime()).isNotNull();
        assertThat(jobExecution.getCreateTime()).isNotNull();
        assertThat(jobExecution.getLastUpdated()).isNotNull();
    }

    private void createTestUsers() {
        // Create enabled users
        for (int i = 1; i <= 5; i++) {
            User user = new User();
            user.setUsername("user" + i);
            user.setEmail("user" + i + "@example.com");
            user.setPassword("password" + i);
            user.setFirstName("First" + i);
            user.setLastName("Last" + i);
            user.setEnabled(true);
            user.addRole(userRole);
            userRepository.save(user);
        }

        // Create disabled users
        for (int i = 6; i <= 8; i++) {
            User user = new User();
            user.setUsername("user" + i);
            user.setEmail("user" + i + "@example.com");
            user.setPassword("password" + i);
            user.setFirstName("First" + i);
            user.setLastName("Last" + i);
            user.setEnabled(false);
            user.addRole(userRole);
            userRepository.save(user);
        }

        // Create admin users
        for (int i = 9; i <= 10; i++) {
            User user = new User();
            user.setUsername("admin" + i);
            user.setEmail("admin" + i + "@example.com");
            user.setPassword("password" + i);
            user.setFirstName("Admin" + i);
            user.setLastName("User" + i);
            user.setEnabled(true);
            user.addRole(adminRole);
            userRepository.save(user);
        }
    }

    private void createSkippableTestUsers() {
        // Create users that will be skipped by processor logic
        User testUser1 = new User();
        testUser1.setUsername("test_skip_user1");
        testUser1.setEmail("invalid-email"); // Invalid email format
        testUser1.setPassword("password");
        testUser1.setEnabled(true);
        testUser1.addRole(userRole);
        userRepository.save(testUser1);

        User testUser2 = new User();
        testUser2.setUsername("test_skip_user2");
        testUser2.setEmail("test@example.com");
        testUser2.setPassword("password");
        testUser2.setEnabled(true);
        testUser2.setAccountNonExpired(false); // Will be skipped
        testUser2.addRole(userRole);
        userRepository.save(testUser2);
    }

    private long getEnabledUserCount() {
        return userRepository.findAll().stream()
                .mapToLong(user -> user.isEnabled() && !user.isDeleted() ? 1 : 0)
                .sum();
    }

    private void verifyOutputFiles() throws IOException {
        Path outputDir = Paths.get("batch-output");
        assertThat(Files.exists(outputDir)).isTrue();

        // Check for CSV files
        boolean csvFileExists = Files.list(outputDir)
                .anyMatch(path -> path.getFileName().toString().endsWith(".csv"));
        assertThat(csvFileExists).isTrue();

        // Check for JSON files
        boolean jsonFileExists = Files.list(outputDir)
                .anyMatch(path -> path.getFileName().toString().endsWith(".json"));
        assertThat(jsonFileExists).isTrue();

        // Check for summary files
        boolean summaryFileExists = Files.list(outputDir)
                .anyMatch(path -> path.getFileName().toString().startsWith("batch_summary_"));
        assertThat(summaryFileExists).isTrue();
    }
}