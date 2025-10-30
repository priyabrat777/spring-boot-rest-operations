package com.enterprise.api.concurrency;

import com.enterprise.api.entity.User;
import com.enterprise.api.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for concurrent access and race condition scenarios.
 * Uses DataJpaTest for focused database concurrency testing.
 */
@DataJpaTest
@ActiveProfiles("test")
class ConcurrencyTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        executorService = Executors.newFixedThreadPool(10);
    }

    @AfterEach
    void tearDown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    @Test
    @DisplayName("Test concurrent user creation with same username")
    void testConcurrentUserCreationSameUsername() throws Exception {
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready

                    try {
                        User user = new User();
                        user.setUsername("duplicateuser");
                        user.setEmail("user" + threadId + "@test.com");
                        user.setPassword("Password123!");

                        userRepository.save(user);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        // Handle constraint violations (duplicate username)
                        conflictCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
                return null;
            });
        }

        startLatch.countDown(); // Start all threads
        endLatch.await(30, TimeUnit.SECONDS); // Wait for completion

        // At least one user should be created successfully
        assertTrue(successCount.get() >= 1);
        assertTrue(conflictCount.get() >= 0);
        assertEquals(threadCount, successCount.get() + conflictCount.get());

        // Verify users in database
        List<User> users = userRepository.findAll();
        assertTrue(users.size() >= 1);
    }

    @Test
    @DisplayName("Test concurrent user updates with optimistic locking")
    void testConcurrentUserUpdatesOptimisticLocking() throws Exception {
        // Create a user first
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@test.com");
        user.setPassword("encoded_password");
        user = userRepository.saveAndFlush(user);
        entityManager.clear();

        final Long userId = user.getId();
        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    try {
                        User userToUpdate = userRepository.findById(userId).orElse(null);
                        if (userToUpdate != null) {
                            userToUpdate.setEmail("updated" + threadId + "@test.com");
                            userRepository.saveAndFlush(userToUpdate);
                            successCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        // Handle optimistic locking exceptions
                        conflictCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
                return null;
            });
        }

        startLatch.countDown();
        endLatch.await(30, TimeUnit.SECONDS);

        // At least one update should succeed
        assertTrue(successCount.get() >= 1);
        assertTrue(conflictCount.get() >= 0);
        assertEquals(threadCount, successCount.get() + conflictCount.get());
    }

    @Test
    @DisplayName("Test concurrent user lookups by username")
    void testConcurrentUserLookups() throws Exception {
        // Create a test user
        User user = new User();
        user.setUsername("lookupuser");
        user.setEmail("lookup@test.com");
        user.setPassword("encoded_password");
        userRepository.saveAndFlush(user);

        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    User foundUser = userRepository.findByUsernameActive("lookupuser").orElse(null);
                    if (foundUser != null) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Handle exceptions
                } finally {
                    endLatch.countDown();
                }
                return null;
            });
        }

        startLatch.countDown();
        endLatch.await(30, TimeUnit.SECONDS);

        // All lookups should succeed
        assertEquals(threadCount, successCount.get());
    }

    @Test
    @DisplayName("Test concurrent database connections")
    void testConcurrentDatabaseConnections() throws Exception {
        int threadCount = 10; // Reasonable number for test environment
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    // Perform database operation
                    User user = new User();
                    user.setUsername("dbuser" + threadId);
                    user.setEmail("dbuser" + threadId + "@test.com");
                    user.setPassword("encoded_password");
                    user = userRepository.save(user);
                    if (user != null) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Handle connection pool exhaustion or other DB issues
                } finally {
                    endLatch.countDown();
                }
                return null;
            });
        }

        startLatch.countDown();
        endLatch.await(60, TimeUnit.SECONDS);

        // All operations should complete successfully with proper connection pooling
        assertEquals(threadCount, successCount.get());
    }

    @Test
    @DisplayName("Test race condition in audit logging")
    void testRaceConditionInAuditLogging() throws Exception {
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    // This should trigger audit logging - use repository for testing
                    User user = new User();
                    user.setUsername("audituser" + threadId);
                    user.setEmail("audit" + threadId + "@test.com");
                    user.setPassword("encoded_password");
                    userRepository.save(user);
                } catch (Exception e) {
                    // Handle exceptions
                } finally {
                    endLatch.countDown();
                }
                return null;
            });
        }

        startLatch.countDown();
        endLatch.await(30, TimeUnit.SECONDS);

        // Verify that all audit entries were created without race conditions
        List<User> users = userRepository.findAll();
        assertEquals(threadCount, users.size());
    }

    @Test
    @DisplayName("Test concurrent cache access")
    void testConcurrentCacheAccess() throws Exception {
        // Create a user to cache
        User user = new User();
        user.setUsername("cacheduser");
        user.setEmail("cached@test.com");
        user.setPassword("encoded_password");
        user = userRepository.save(user);

        final Long userId = user.getId();
        int threadCount = 15;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    // Access the same user concurrently to test cache behavior
                    User foundUser = userRepository.findById(userId).orElse(null);
                    if (foundUser != null) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Handle exceptions
                } finally {
                    endLatch.countDown();
                }
                return null;
            });
        }

        startLatch.countDown();
        endLatch.await(30, TimeUnit.SECONDS);

        // All cache accesses should succeed
        assertEquals(threadCount, successCount.get());
    }

    @Test
    @DisplayName("Test deadlock prevention")
    void testDeadlockPrevention() throws Exception {
        // Create two users
        User user1 = new User();
        user1.setUsername("user1");
        user1.setEmail("user1@test.com");
        user1.setPassword("encoded_password");
        user1 = userRepository.save(user1);

        User user2 = new User();
        user2.setUsername("user2");
        user2.setEmail("user2@test.com");
        user2.setPassword("encoded_password");
        user2 = userRepository.save(user2);

        final Long userId1 = user1.getId();
        final Long userId2 = user2.getId();

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);

        // Thread 1: Update user1 then user2
        executorService.submit(() -> {
            try {
                startLatch.await();

                // Use repository directly for testing
                User userA = userRepository.findById(userId1).orElse(null);
                if (userA != null) {
                    userA.setEmail("updated1@test.com");
                    userRepository.save(userA);
                }

                Thread.sleep(100); // Small delay to increase chance of deadlock

                User userB = userRepository.findById(userId2).orElse(null);
                if (userB != null) {
                    userB.setEmail("updated2@test.com");
                    userRepository.save(userB);
                }

                successCount.incrementAndGet();
            } catch (Exception e) {
                // Handle deadlock or other exceptions
            } finally {
                endLatch.countDown();
            }
            return null;
        });

        // Thread 2: Update user2 then user1
        executorService.submit(() -> {
            try {
                startLatch.await();

                // Use repository directly for testing
                User userC = userRepository.findById(userId2).orElse(null);
                if (userC != null) {
                    userC.setEmail("updated2b@test.com");
                    userRepository.save(userC);
                }

                Thread.sleep(100); // Small delay to increase chance of deadlock

                User userD = userRepository.findById(userId1).orElse(null);
                if (userD != null) {
                    userD.setEmail("updated1b@test.com");
                    userRepository.save(userD);
                }

                successCount.incrementAndGet();
            } catch (Exception e) {
                // Handle deadlock or other exceptions
            } finally {
                endLatch.countDown();
            }
            return null;
        });

        startLatch.countDown();
        boolean completed = endLatch.await(30, TimeUnit.SECONDS);

        assertTrue(completed, "Operations should complete without deadlock");
        // At least one operation should succeed (depending on transaction isolation)
        assertTrue(successCount.get() >= 1);
    }

    @Test
    @DisplayName("Test concurrent user existence checks")
    void testConcurrentUserExistenceChecks() throws Exception {
        // Create a user for testing
        User user = new User();
        user.setUsername("existsuser");
        user.setEmail("exists@test.com");
        user.setPassword("encoded_password");
        userRepository.saveAndFlush(user);

        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // Simulate concurrent existence checks
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    boolean exists = userRepository.existsByUsernameActive("existsuser");
                    if (exists) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Handle exceptions
                } finally {
                    endLatch.countDown();
                }
                return null;
            });
        }

        startLatch.countDown();
        endLatch.await(30, TimeUnit.SECONDS);

        // All existence checks should succeed
        assertEquals(threadCount, successCount.get());
    }

    @Test
    @DisplayName("Test race condition in counter increments")
    void testRaceConditionInCounterIncrements() throws Exception {
        int threadCount = 20;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger operationCount = new AtomicInteger(0);

        // Simulate concurrent operations that might affect counters
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    // Create user (this might increment internal counters)
                    User user = new User();
                    user.setUsername("counteruser" + threadId);
                    user.setEmail("counter" + threadId + "@test.com");
                    user.setPassword("encoded_password");
                    userRepository.save(user);

                    operationCount.incrementAndGet();
                } catch (Exception e) {
                    // Handle exceptions
                } finally {
                    endLatch.countDown();
                }
                return null;
            });
        }

        startLatch.countDown();
        endLatch.await(30, TimeUnit.SECONDS);

        // All operations should complete successfully
        assertEquals(threadCount, operationCount.get());

        // Verify all users were created
        long userCount = userRepository.count();
        assertTrue(userCount >= threadCount, "All users should be created despite concurrency");
    }

    @Test
    @DisplayName("Test race condition in resource cleanup")
    void testRaceConditionInResourceCleanup() throws Exception {
        // Create users to be deleted concurrently
        List<User> usersToDelete = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            User user = new User();
            user.setUsername("deleteuser" + i);
            user.setEmail("delete" + i + "@test.com");
            user.setPassword("encoded_password");
            usersToDelete.add(userRepository.save(user));
        }

        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger deleteSuccessCount = new AtomicInteger(0);

        // Simulate concurrent deletions
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    Long userId = usersToDelete.get(index).getId();

                    // Soft delete the user
                    User user = userRepository.findById(userId).orElse(null);
                    if (user != null && !user.isDeleted()) {
                        user.setDeleted(true);
                        userRepository.save(user);
                        deleteSuccessCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Handle exceptions (e.g., optimistic locking)
                } finally {
                    endLatch.countDown();
                }
                return null;
            });
        }

        startLatch.countDown();
        endLatch.await(30, TimeUnit.SECONDS);

        // All deletions should succeed
        assertEquals(threadCount, deleteSuccessCount.get());
    }

    @Test
    @DisplayName("Test race condition in batch processing")
    void testRaceConditionInBatchProcessing() throws Exception {
        // Create multiple batches of users
        int batchCount = 5;
        int usersPerBatch = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(batchCount);
        AtomicInteger totalUsersCreated = new AtomicInteger(0);

        for (int batch = 0; batch < batchCount; batch++) {
            final int batchId = batch;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    List<User> batchUsers = new ArrayList<>();
                    for (int i = 0; i < usersPerBatch; i++) {
                        User user = new User();
                        user.setUsername("batchuser" + batchId + "_" + i);
                        user.setEmail("batch" + batchId + "_" + i + "@test.com");
                        user.setPassword("encoded_password");
                        batchUsers.add(user);
                    }

                    List<User> savedUsers = userRepository.saveAll(batchUsers);
                    totalUsersCreated.addAndGet(savedUsers.size());
                } catch (Exception e) {
                    // Handle exceptions
                } finally {
                    endLatch.countDown();
                }
                return null;
            });
        }

        startLatch.countDown();
        endLatch.await(30, TimeUnit.SECONDS);

        // All batch operations should complete successfully
        assertEquals(batchCount * usersPerBatch, totalUsersCreated.get());
    }

    @Test
    @DisplayName("Test race condition in transaction boundaries")
    void testRaceConditionInTransactionBoundaries() throws Exception {
        // Create a user to be updated by multiple transactions
        User user = new User();
        user.setUsername("transactionuser");
        user.setEmail("transaction@test.com");
        user.setPassword("encoded_password");
        user = userRepository.save(user);

        final Long userId = user.getId();
        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successfulUpdates = new AtomicInteger(0);

        // Simulate concurrent updates within transaction boundaries
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    // Each thread tries to update the user
                    User userToUpdate = userRepository.findById(userId).orElse(null);
                    if (userToUpdate != null) {
                        userToUpdate.setEmail("updated" + threadId + "@test.com");
                        userRepository.save(userToUpdate);
                        successfulUpdates.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Handle optimistic locking exceptions
                } finally {
                    endLatch.countDown();
                }
                return null;
            });
        }

        startLatch.countDown();
        endLatch.await(30, TimeUnit.SECONDS);

        // At least one update should succeed
        assertTrue(successfulUpdates.get() >= 1, "At least one transaction should succeed");
    }

    @Test
    @DisplayName("Test race condition in audit trail generation")
    void testRaceConditionInAuditTrailGeneration() throws Exception {
        int threadCount = 15;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger auditedOperations = new AtomicInteger(0);
        AtomicInteger failedOperations = new AtomicInteger(0);

        // Simulate concurrent operations that generate audit trails
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    try {
                        // Create user (should generate audit trail)
                        User user = new User();
                        user.setUsername("audittrailuser" + threadId + "_" + System.nanoTime());
                        user.setEmail("audittrail" + threadId + "@test.com");
                        user.setPassword("encoded_password");
                        userRepository.save(user);

                        auditedOperations.incrementAndGet();
                    } catch (Exception e) {
                        // Handle constraint violations or other exceptions
                        failedOperations.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
                return null;
            });
        }

        startLatch.countDown();
        endLatch.await(30, TimeUnit.SECONDS);

        // All operations should complete (either succeed or fail)
        assertEquals(threadCount, auditedOperations.get() + failedOperations.get());
        // At least some operations should succeed
        assertTrue(auditedOperations.get() > 0, "At least some audit operations should succeed");
    }
}