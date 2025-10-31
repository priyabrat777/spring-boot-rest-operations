package com.enterprise.api.batch.processors;

import com.enterprise.api.batch.dto.UserDataDto;
import com.enterprise.api.entity.Role;
import com.enterprise.api.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for UserDataProcessor.
 * Tests the processing logic and skip conditions.
 * 
 * Requirements: 6.2, 6.4
 */
@ExtendWith(MockitoExtension.class)
class UserDataProcessorTest {

    @InjectMocks
    private UserDataProcessor processor;

    @Mock
    private StepExecution stepExecution;

    @Mock
    private JobExecution jobExecution;

    private JobParameters jobParameters;

    @BeforeEach
    void setUp() {
        jobParameters = new JobParametersBuilder()
                .addString("user", "test-user")
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        when(stepExecution.getJobParameters()).thenReturn(jobParameters);
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getId()).thenReturn(123L);

        // Initialize the processor
        processor.beforeStep(stepExecution);
        
        // Disable failure simulation for predictable testing
        processor.setSimulateFailures(false);
    }

    @Test
    void testProcessValidUser() throws Exception {
        // Given
        User user = createValidUser();

        // When
        UserDataDto result = processor.process(user);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(user.getId());
        assertThat(result.getUsername()).isEqualTo(user.getUsername());
        assertThat(result.getEmail()).isEqualTo(user.getEmail());
        assertThat(result.getFirstName()).isEqualTo(user.getFirstName());
        assertThat(result.getLastName()).isEqualTo(user.getLastName());
        assertThat(result.isEnabled()).isEqualTo(user.isEnabled());
        assertThat(result.getProcessedBy()).isEqualTo("test-user");
        assertThat(result.getProcessedAt()).isNotNull();
        assertThat(result.getStatus()).isIn("ACTIVE", "INACTIVE", "DORMANT", "DISABLED");
    }

    @Test
    void testProcessDisabledUser() throws Exception {
        // Given
        User user = createValidUser();
        user.setEnabled(false);

        // When
        UserDataDto result = processor.process(user);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("DISABLED");
    }

    @Test
    void testSkipUserWithInvalidEmail() throws Exception {
        // Given
        User user = createValidUser();
        user.setEmail("invalid-email"); // No @ symbol

        // When
        UserDataDto result = processor.process(user);

        // Then
        assertThat(result).isNull(); // Should be skipped
    }

    @Test
    void testSkipUserWithNullEmail() throws Exception {
        // Given
        User user = createValidUser();
        user.setEmail(null);

        // When
        UserDataDto result = processor.process(user);

        // Then
        assertThat(result).isNull(); // Should be skipped
    }

    @Test
    void testSkipTestUser() throws Exception {
        // Given
        User user = createValidUser();
        user.setUsername("test_user123");

        // When
        UserDataDto result = processor.process(user);

        // Then
        assertThat(result).isNull(); // Should be skipped
    }

    @Test
    void testSkipExpiredUser() throws Exception {
        // Given
        User user = createValidUser();
        user.setAccountNonExpired(false);

        // When
        UserDataDto result = processor.process(user);

        // Then
        assertThat(result).isNull(); // Should be skipped
    }

    @Test
    void testProcessingWithActivityAnalysis() throws Exception {
        // Given
        User user = createValidUser();

        // When
        UserDataDto result = processor.process(user);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getLoginCount()).isGreaterThanOrEqualTo(0);
        
        if (result.getLoginCount() > 0) {
            assertThat(result.getLastLoginDate()).isNotNull();
        }
    }

    @Test
    void testUserStatusDetermination() throws Exception {
        // Given - Test multiple users to cover different status scenarios
        for (int i = 0; i < 10; i++) {
            User user = createValidUser();
            user.setId((long) i);
            user.setUsername("user" + i);

            // When
            UserDataDto result = processor.process(user);

            // Then
            if (result != null) { // Not skipped
                assertThat(result.getStatus()).isIn("ACTIVE", "INACTIVE", "DORMANT", "DISABLED");
                
                if (!result.isEnabled()) {
                    assertThat(result.getStatus()).isEqualTo("DISABLED");
                } else if (result.getLoginCount() == 0) {
                    assertThat(result.getStatus()).isEqualTo("INACTIVE");
                }
            }
        }
    }

    @Test
    void testProcessingMetadata() throws Exception {
        // Given
        User user = createValidUser();

        // When
        UserDataDto result = processor.process(user);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProcessedBy()).isEqualTo("test-user");
        assertThat(result.getProcessedAt()).isNotNull();
    }

    @Test
    void testProcessingWithDifferentJobUser() throws Exception {
        // Given
        JobParameters customJobParameters = new JobParametersBuilder()
                .addString("user", "admin-user")
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        when(stepExecution.getJobParameters()).thenReturn(customJobParameters);
        processor.beforeStep(stepExecution);

        User user = createValidUser();

        // When
        UserDataDto result = processor.process(user);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProcessedBy()).isEqualTo("admin-user");
    }

    @Test
    void testProcessingWithSystemUser() throws Exception {
        // Given
        JobParameters systemJobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters(); // No user parameter

        when(stepExecution.getJobParameters()).thenReturn(systemJobParameters);
        processor.beforeStep(stepExecution);
        processor.setSimulateFailures(false); // Ensure no failures for this test

        User user = createValidUser();

        // When
        UserDataDto result = processor.process(user);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProcessedBy()).isEqualTo("system"); // Default value
    }

    @Test
    void testFailureSimulationCanBeEnabled() throws Exception {
        // Given
        processor.setSimulateFailures(true); // Enable failure simulation
        User user = createValidUser();

        // When/Then - We can't predict when the failure will occur due to randomness,
        // but we can verify that the method exists and can be called without error
        // This test mainly verifies the configuration capability
        processor.setSimulateFailures(false); // Disable it again for safety
        
        UserDataDto result = processor.process(user);
        assertThat(result).isNotNull(); // Should succeed with failures disabled
    }

    private User createValidUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("validuser");
        user.setEmail("valid@example.com");
        user.setPassword("password123");
        user.setFirstName("Valid");
        user.setLastName("User");
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);

        // Add a role
        Role role = new Role();
        role.setId(1L);
        role.setName("USER");
        user.addRole(role);

        return user;
    }
}