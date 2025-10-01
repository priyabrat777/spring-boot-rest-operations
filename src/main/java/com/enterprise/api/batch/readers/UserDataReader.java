package com.enterprise.api.batch.readers;

import com.enterprise.api.batch.dto.UserDataDto;
import com.enterprise.api.entity.User;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * ItemReader for reading user data from the database in chunks.
 * Uses JPA pagination to efficiently process large datasets.
 * 
 * Requirements: 6.2, 6.3
 */
@Component
public class UserDataReader {

    private final EntityManagerFactory entityManagerFactory;

    public UserDataReader(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    /**
     * Creates a JPA paging item reader for User entities.
     * 
     * @param pageSize the number of items to read per page
     * @param enabledOnly whether to read only enabled users
     * @return configured JpaPagingItemReader
     */
    public JpaPagingItemReader<User> createUserReader(int pageSize, boolean enabledOnly) {
        String jpql = enabledOnly 
            ? "SELECT u FROM User u WHERE u.deleted = false AND u.enabled = true ORDER BY u.id"
            : "SELECT u FROM User u WHERE u.deleted = false ORDER BY u.id";

        return new JpaPagingItemReaderBuilder<User>()
                .name("userDataReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString(jpql)
                .pageSize(pageSize)
                .build();
    }

    /**
     * Creates a JPA paging item reader for User entities with date filter.
     * 
     * @param pageSize the number of items to read per page
     * @param daysBack number of days back to filter users by creation date
     * @return configured JpaPagingItemReader
     */
    public JpaPagingItemReader<User> createUserReaderWithDateFilter(int pageSize, int daysBack) {
        String jpql = "SELECT u FROM User u WHERE u.deleted = false " +
                     "AND u.createdDate >= :fromDate ORDER BY u.id";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("fromDate", java.time.LocalDateTime.now().minusDays(daysBack));

        return new JpaPagingItemReaderBuilder<User>()
                .name("userDataReaderWithDateFilter")
                .entityManagerFactory(entityManagerFactory)
                .queryString(jpql)
                .parameterValues(parameters)
                .pageSize(pageSize)
                .build();
    }

    /**
     * Creates a JPA paging item reader for User entities by role.
     * 
     * @param pageSize the number of items to read per page
     * @param roleName the role name to filter by
     * @return configured JpaPagingItemReader
     */
    public JpaPagingItemReader<User> createUserReaderByRole(int pageSize, String roleName) {
        String jpql = "SELECT DISTINCT u FROM User u JOIN u.roles r " +
                     "WHERE u.deleted = false AND r.name = :roleName ORDER BY u.id";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("roleName", roleName);

        return new JpaPagingItemReaderBuilder<User>()
                .name("userDataReaderByRole")
                .entityManagerFactory(entityManagerFactory)
                .queryString(jpql)
                .parameterValues(parameters)
                .pageSize(pageSize)
                .build();
    }

    /**
     * Converts User entity to UserDataDto for processing.
     * This method can be used in conjunction with an ItemProcessor.
     * 
     * @param user the User entity
     * @return UserDataDto for batch processing
     */
    public UserDataDto convertToDto(User user) {
        UserDataDto dto = new UserDataDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.isEnabled()
        );
        
        // Set additional fields that might be useful for processing
        dto.setStatus("PENDING");
        
        return dto;
    }
}