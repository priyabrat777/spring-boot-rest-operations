package com.enterprise.api.performance;

import com.enterprise.api.entity.User;
import com.enterprise.api.repository.UserRepository;
import com.enterprise.api.util.PaginationUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests for database operations and caching.
 * 
 * These tests verify:
 * - Database connection pooling performance
 * - Pagination performance with large datasets
 * - Entity graph performance vs N+1 queries
 * - Caching effectiveness
 * 
 * Requirements addressed:
 * - 5.3: Performance optimization with caching and pagination
 * - 5.5: Query optimization with entity graphs
 */
@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.jpa.show-sql=false",
    "logging.level.org.hibernate.SQL=WARN"
})
class PerformanceTest {

    @Autowired
    private UserRepository userRepository;

    private List<User> testUsers;

    @BeforeEach
    void setUp() {
        // Clear existing data to avoid unique constraint violations
        userRepository.deleteAll();
        userRepository.flush();
        
        // Create test users for performance testing
        testUsers = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            User user = new User();
            user.setUsername("testuser" + i);
            user.setEmail("testuser" + i + "@example.com");
            user.setPassword("password123");
            user.setFirstName("Test");
            user.setLastName("User" + i);
            testUsers.add(user);
        }
        userRepository.saveAll(testUsers);
        userRepository.flush();
    }

    @Test
    void testPaginationPerformance() {
        // Test different pagination strategies
        Instant start = Instant.now();
        
        // Standard pagination
        Pageable pageable = PaginationUtil.createPageable(0, 20);
        Page<User> page = userRepository.findAllActive(pageable);
        
        Duration standardPagination = Duration.between(start, Instant.now());
        
        assertNotNull(page);
        assertTrue(page.hasContent());
        assertEquals(20, page.getSize());
        
        // Test optimized pagination
        start = Instant.now();
        pageable = PaginationUtil.createOptimizedPageable(0, 20, "id");
        page = userRepository.findActiveOptimized(pageable);
        
        Duration optimizedPagination = Duration.between(start, Instant.now());
        
        assertNotNull(page);
        assertTrue(page.hasContent());
        
        // Log performance metrics
        System.out.println("Standard pagination time: " + standardPagination.toMillis() + "ms");
        System.out.println("Optimized pagination time: " + optimizedPagination.toMillis() + "ms");
        
        // Performance should be reasonable (less than 1 second for test data)
        assertTrue(standardPagination.toMillis() < 1000, "Standard pagination took too long");
        assertTrue(optimizedPagination.toMillis() < 1000, "Optimized pagination took too long");
    }

    @Test
    void testSlicePaginationPerformance() {
        // Test Slice vs Page performance
        Instant start = Instant.now();
        
        Pageable pageable = PaginationUtil.createPageable(0, 20);
        Page<User> page = userRepository.findAllActive(pageable);
        
        Duration pageTime = Duration.between(start, Instant.now());
        
        start = Instant.now();
        Slice<User> slice = userRepository.findActiveSlice(pageable);
        
        Duration sliceTime = Duration.between(start, Instant.now());
        
        assertNotNull(page);
        assertNotNull(slice);
        assertTrue(page.hasContent());
        assertTrue(slice.hasContent());
        
        System.out.println("Page query time: " + pageTime.toMillis() + "ms");
        System.out.println("Slice query time: " + sliceTime.toMillis() + "ms");
        
        // Slice should be faster as it doesn't calculate total count
        // Note: With small test data, the difference might not be significant
        assertTrue(sliceTime.toMillis() <= pageTime.toMillis() + 50, "Slice should be faster or similar to Page");
    }

    @Test
    void testCursorBasedPaginationPerformance() {
        // Test cursor-based pagination for large datasets
        Instant start = Instant.now();
        
        Pageable pageable = PaginationUtil.createCursorPageable(20);
        List<User> firstPage = userRepository.findActiveCursorBased(null, pageable);
        
        Duration firstPageTime = Duration.between(start, Instant.now());
        
        assertNotNull(firstPage);
        assertFalse(firstPage.isEmpty());
        assertTrue(firstPage.size() <= 20);
        
        // Get next page using cursor
        if (!firstPage.isEmpty()) {
            Long lastId = firstPage.get(firstPage.size() - 1).getId();
            
            start = Instant.now();
            List<User> secondPage = userRepository.findActiveCursorBased(lastId, pageable);
            Duration secondPageTime = Duration.between(start, Instant.now());
            
            assertNotNull(secondPage);
            System.out.println("First page cursor time: " + firstPageTime.toMillis() + "ms");
            System.out.println("Second page cursor time: " + secondPageTime.toMillis() + "ms");
            
            // Cursor-based pagination should maintain consistent performance
            assertTrue(Math.abs(firstPageTime.toMillis() - secondPageTime.toMillis()) < 100,
                "Cursor pagination performance should be consistent");
        }
    }

    @Test
    void testEntityGraphPerformance() {
        // Test entity graph vs regular query performance
        String username = testUsers.get(0).getUsername();
        
        // Regular query (may cause N+1 problem)
        Instant start = Instant.now();
        Optional<User> userRegular = userRepository.findByUsernameActive(username);
        Duration regularTime = Duration.between(start, Instant.now());
        
        // Entity graph query
        start = Instant.now();
        Optional<User> userWithRoles = userRepository.findByUsernameWithRoles(username);
        Duration entityGraphTime = Duration.between(start, Instant.now());
        
        assertTrue(userRegular.isPresent());
        assertTrue(userWithRoles.isPresent());
        
        System.out.println("Regular query time: " + regularTime.toMillis() + "ms");
        System.out.println("Entity graph query time: " + entityGraphTime.toMillis() + "ms");
        
        // Both should complete quickly
        assertTrue(regularTime.toMillis() < 1000, "Regular query took too long");
        assertTrue(entityGraphTime.toMillis() < 1000, "Entity graph query took too long");
    }

    @Test
    void testBatchOperationPerformance() {
        // Test batch operations vs individual operations
        List<User> newUsers = new ArrayList<>();
        for (int i = 100; i < 120; i++) {
            User user = new User();
            user.setUsername("batchuser" + i);
            user.setEmail("batchuser" + i + "@example.com");
            user.setPassword("password123");
            newUsers.add(user);
        }
        
        // Individual saves
        Instant start = Instant.now();
        for (User user : newUsers.subList(0, 10)) {
            userRepository.save(user);
        }
        userRepository.flush();
        Duration individualTime = Duration.between(start, Instant.now());
        
        // Batch save
        start = Instant.now();
        userRepository.saveAll(newUsers.subList(10, 20));
        userRepository.flush();
        Duration batchTime = Duration.between(start, Instant.now());
        
        System.out.println("Individual saves time: " + individualTime.toMillis() + "ms");
        System.out.println("Batch save time: " + batchTime.toMillis() + "ms");
        
        // Batch operations should be more efficient
        assertTrue(batchTime.toMillis() <= individualTime.toMillis(),
            "Batch operations should be more efficient than individual operations");
    }

    @Test
    void testLargeDatasetPagination() {
        // Test pagination performance with different page sizes
        int[] pageSizes = {10, 50, 100, 500};
        
        for (int pageSize : pageSizes) {
            Instant start = Instant.now();
            
            Pageable pageable = PaginationUtil.createPageable(0, pageSize);
            Page<User> page = userRepository.findAllActive(pageable);
            
            Duration queryTime = Duration.between(start, Instant.now());
            
            assertNotNull(page);
            System.out.println("Page size " + pageSize + " query time: " + queryTime.toMillis() + "ms");
            
            // Larger page sizes should not cause exponential performance degradation
            assertTrue(queryTime.toMillis() < 2000, 
                "Query with page size " + pageSize + " took too long: " + queryTime.toMillis() + "ms");
        }
    }

    @Test
    void testQueryOptimizationWithIndexes() {
        // Test queries that should benefit from database indexes
        String username = testUsers.get(0).getUsername();
        String email = testUsers.get(0).getEmail();
        
        // Username lookup (should use index)
        Instant start = Instant.now();
        Optional<User> userByUsername = userRepository.findByUsernameActive(username);
        Duration usernameTime = Duration.between(start, Instant.now());
        
        // Email lookup (should use index)
        start = Instant.now();
        Optional<User> userByEmail = userRepository.findByEmailActive(email);
        Duration emailTime = Duration.between(start, Instant.now());
        
        assertTrue(userByUsername.isPresent());
        assertTrue(userByEmail.isPresent());
        
        System.out.println("Username lookup time: " + usernameTime.toMillis() + "ms");
        System.out.println("Email lookup time: " + emailTime.toMillis() + "ms");
        
        // Indexed queries should be fast
        assertTrue(usernameTime.toMillis() < 100, "Username lookup should be fast with index");
        assertTrue(emailTime.toMillis() < 100, "Email lookup should be fast with index");
    }

    @Test
    void testConnectionPoolPerformance() {
        // Test multiple concurrent database operations
        List<Runnable> operations = new ArrayList<>();
        
        for (int i = 0; i < 10; i++) {
            operations.add(() -> {
                // Simulate database operations
                userRepository.findAllActive(PaginationUtil.createPageable(0, 10));
                userRepository.countByDeletedFalse();
            });
        }
        
        Instant start = Instant.now();
        
        // Execute operations (simulating concurrent access)
        operations.forEach(Runnable::run);
        
        Duration totalTime = Duration.between(start, Instant.now());
        
        System.out.println("10 concurrent operations time: " + totalTime.toMillis() + "ms");
        
        // Connection pool should handle concurrent operations efficiently
        assertTrue(totalTime.toMillis() < 5000, 
            "Connection pool performance is poor: " + totalTime.toMillis() + "ms");
    }
}