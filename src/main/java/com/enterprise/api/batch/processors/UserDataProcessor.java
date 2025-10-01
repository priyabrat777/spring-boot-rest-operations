package com.enterprise.api.batch.processors;

import com.enterprise.api.batch.dto.UserDataDto;
import com.enterprise.api.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * ItemProcessor for processing user data in batch jobs.
 * Transforms User entities into processed UserDataDto objects.
 * 
 * Requirements: 6.2, 6.4
 */
@Component
public class UserDataProcessor implements ItemProcessor<User, UserDataDto> {

    private static final Logger logger = LoggerFactory.getLogger(UserDataProcessor.class);
    private static final Random random = new Random();

    private String jobExecutionUser;
    private Long jobExecutionId;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.jobExecutionUser = stepExecution.getJobParameters().getString("user", "system");
        this.jobExecutionId = stepExecution.getJobExecution().getId();
        logger.info("Starting user data processing step. Job execution ID: {}, User: {}", 
                   jobExecutionId, jobExecutionUser);
    }

    @Override
    public UserDataDto process(User user) throws Exception {
        logger.debug("Processing user: {}", user.getUsername());

        // Simulate processing logic that might fail for some users
        if (shouldSkipUser(user)) {
            logger.warn("Skipping user {} due to processing rules", user.getUsername());
            return null; // Returning null will skip this item
        }

        // Convert User to UserDataDto
        UserDataDto dto = new UserDataDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.isEnabled()
        );

        // Simulate business logic processing
        processUserData(dto, user);

        // Set processing metadata
        dto.setProcessedBy(jobExecutionUser);
        dto.setProcessedAt(LocalDateTime.now());

        logger.debug("Successfully processed user: {} with status: {}", 
                    user.getUsername(), dto.getStatus());

        return dto;
    }

    /**
     * Processes user data and applies business logic.
     * 
     * @param dto the UserDataDto to process
     * @param user the original User entity
     */
    private void processUserData(UserDataDto dto, User user) {
        // Simulate user activity analysis
        simulateUserActivityAnalysis(dto);

        // Determine user status based on processing
        determineUserStatus(dto, user);

        // Simulate some processing that might throw exceptions
        simulateProcessingWithPotentialFailure(dto);
    }

    /**
     * Simulates user activity analysis.
     * 
     * @param dto the UserDataDto to update
     */
    private void simulateUserActivityAnalysis(UserDataDto dto) {
        // Simulate login count and last login date
        dto.setLoginCount(random.nextInt(100));
        
        if (dto.getLoginCount() > 0) {
            // Simulate last login within the last 30 days
            int daysBack = random.nextInt(30) + 1;
            dto.setLastLoginDate(LocalDateTime.now().minusDays(daysBack));
        }
    }

    /**
     * Determines user status based on activity and account state.
     * 
     * @param dto the UserDataDto to update
     * @param user the original User entity
     */
    private void determineUserStatus(UserDataDto dto, User user) {
        if (!user.isEnabled()) {
            dto.setStatus("DISABLED");
        } else if (dto.getLoginCount() == 0) {
            dto.setStatus("INACTIVE");
        } else if (dto.getLastLoginDate() != null && 
                   dto.getLastLoginDate().isBefore(LocalDateTime.now().minusDays(30))) {
            dto.setStatus("DORMANT");
        } else {
            dto.setStatus("ACTIVE");
        }
    }

    /**
     * Simulates processing that might fail for certain conditions.
     * This demonstrates skip logic in batch processing.
     * 
     * @param dto the UserDataDto being processed
     * @throws RuntimeException if processing should fail for this item
     */
    private void simulateProcessingWithPotentialFailure(UserDataDto dto) {
        // Simulate a 5% failure rate for demonstration
        if (random.nextDouble() < 0.05) {
            String errorMsg = "Simulated processing failure for user: " + dto.getUsername();
            logger.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        // Simulate processing delay
        try {
            Thread.sleep(10); // Small delay to simulate processing time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Processing interrupted", e);
        }
    }

    /**
     * Determines if a user should be skipped during processing.
     * 
     * @param user the User entity to check
     * @return true if user should be skipped, false otherwise
     */
    private boolean shouldSkipUser(User user) {
        // Skip users with invalid email formats (basic check)
        if (user.getEmail() == null || !user.getEmail().contains("@")) {
            return true;
        }

        // Skip users with specific username patterns (e.g., test users)
        if (user.getUsername() != null && user.getUsername().startsWith("test_")) {
            return true;
        }

        // Skip users that are not account non-expired
        if (!user.isAccountNonExpired()) {
            return true;
        }

        return false;
    }
}