package com.enterprise.api.performance;

import com.enterprise.api.dto.request.CreateUserRequest;
import com.enterprise.api.entity.User;
import com.enterprise.api.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive stress tests for the API under extreme conditions.
 * Tests system behavior at breaking points and resource exhaustion scenarios.
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class StressTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        executorService = Executors.newFixedThreadPool(50);
    }

    @Test
    @DisplayName("Test system behavior under extreme concurrent load")
    void testSystemBehaviorUnderExtremeConcurrentLoad() throws Exception {
        int threadCount = 25; // High concurrency
        int requestsPerThread = 4;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    for (int j = 0; j < requestsPerThread; j++) {
                        long startTime = System.currentTimeMillis();
                        
                        CreateUserRequest request = new CreateUserRequest();
                        request.setUsername("stressuser" + threadId + "_" + j);
                        request.setEmail("stress" + threadId + "_" + j + "@test.com");
                        request.setPassword("Password123!");

                        try {
                            mockMvc.perform(post("/api/users")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                                    .andDo(result -> {
                                        long endTime = System.currentTimeMillis();
                                        totalResponseTime.addAndGet(endTime - startTime);
                                        
                                        if (result.getResponse().getStatus() == 201) {
                                            successCount.incrementAndGet();
                                        } else {
                                            errorCount.incrementAndGet();
                                        }
                                    });
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                        }
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
        boolean completed = endLatch.await(180, TimeUnit.SECONDS); // Extended timeout

        assertTrue(completed, "Extreme load test should complete within extended timeout");

        int totalRequests = threadCount * requestsPerThread;
        double successRate = (double) successCount.get() / totalRequests * 100;
        double averageResponseTime = totalRequests > 0 ? (double) totalResponseTime.get() / totalRequests : 0;

        System.out.println("Extreme Concurrent Load Test Results:");
        System.out.println("Total Requests: " + totalRequests);
        System.out.println("Successful Requests: " + successCount.get());
        System.out.println("Failed Requests: " + errorCount.get());
        System.out.println("Success Rate: " + String.format("%.2f%%", successRate));
        System.out.println("Average Response Time: " + String.format("%.2f ms", averageResponseTime));

        // System should maintain some level of functionality under extreme load
        assertTrue(successRate > 30, "System should maintain >30% success rate under extreme load");
        assertTrue(averageResponseTime < 10000, "Average response time should be under 10 seconds");
    }

    @Test
    @DisplayName("Test memory exhaustion resistance")
    void testMemoryExhaustionResistance() throws Exception {
        // Monitor memory usage throughout the test
        long initialMemory = getUsedMemory();
        
        int threadCount = 15;
        int requestsPerThread = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger completedRequests = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    for (int j = 0; j < requestsPerThread; j++) {
                        // Create requests with large payloads to stress memory
                        CreateUserRequest request = new CreateUserRequest();
                        request.setUsername("memstress" + threadId + "_" + j);
                        request.setEmail("memstress" + threadId + "_" + j + "@test.com");
                        request.setPassword("Password123!" + "x".repeat(100)); // Larger password

                        try {
                            mockMvc.perform(post("/api/users")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                                    .andDo(result -> {
                                        completedRequests.incrementAndGet();
                                    });
                        } catch (Exception e) {
                            // Handle memory-related exceptions
                        }
                        
                        // Check memory usage periodically
                        if (j % 2 == 0) {
                            long currentMemory = getUsedMemory();
                            long memoryIncrease = currentMemory - initialMemory;
                            
                            // If memory usage is too high, break to prevent OOM
                            if (memoryIncrease > 200 * 1024 * 1024) { // 200MB limit
                                System.out.println("Memory limit reached, stopping thread " + threadId);
                                break;
                            }
                        }
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
        boolean completed = endLatch.await(120, TimeUnit.SECONDS);

        assertTrue(completed, "Memory exhaustion test should complete");

        long finalMemory = getUsedMemory();
        long memoryIncrease = finalMemory - initialMemory;

        System.out.println("Memory Exhaustion Test Results:");
        System.out.println("Completed Requests: " + completedRequests.get());
        System.out.println("Initial Memory: " + formatBytes(initialMemory));
        System.out.println("Final Memory: " + formatBytes(finalMemory));
        System.out.println("Memory Increase: " + formatBytes(memoryIncrease));

        // System should not consume excessive memory
        assertTrue(memoryIncrease < 300 * 1024 * 1024, "Memory increase should be under 300MB");
        assertTrue(completedRequests.get() > 0, "Some requests should complete successfully");
    }

    @Test
    @DisplayName("Test rapid burst traffic handling")
    void testRapidBurstTrafficHandling() throws Exception {
        int burstSize = 30; // Large burst of simultaneous requests
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(burstSize);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        // Create a burst of simultaneous requests
        for (int i = 0; i < burstSize; i++) {
            final int requestId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    CreateUserRequest request = new CreateUserRequest();
                    request.setUsername("burstuser" + requestId);
                    request.setEmail("burst" + requestId + "@test.com");
                    request.setPassword("Password123!");

                    mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                            .andDo(result -> {
                                int status = result.getResponse().getStatus();
                                if (status == 201) {
                                    successCount.incrementAndGet();
                                } else if (status == 429 || status == 503) { // Rate limited or service unavailable
                                    rejectedCount.incrementAndGet();
                                }
                            });
                } catch (Exception e) {
                    rejectedCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
                return null;
            });
        }

        startLatch.countDown(); // Release all threads simultaneously
        boolean completed = endLatch.await(60, TimeUnit.SECONDS);

        assertTrue(completed, "Burst traffic test should complete");

        System.out.println("Rapid Burst Traffic Test Results:");
        System.out.println("Burst Size: " + burstSize);
        System.out.println("Successful Requests: " + successCount.get());
        System.out.println("Rejected Requests: " + rejectedCount.get());
        System.out.println("Success Rate: " + String.format("%.2f%%", (double) successCount.get() / burstSize * 100));

        // System should handle burst traffic gracefully
        assertTrue(successCount.get() + rejectedCount.get() == burstSize, "All requests should be processed");
        assertTrue(successCount.get() > 0, "Some requests should succeed even in burst");
    }

    @Test
    @DisplayName("Test sustained high load over time")
    void testSustainedHighLoadOverTime() throws Exception {
        int threadCount = 10;
        int durationSeconds = 30; // Sustained load for 30 seconds
        AtomicInteger totalRequests = new AtomicInteger(0);
        AtomicInteger successfulRequests = new AtomicInteger(0);
        AtomicBoolean stopTest = new AtomicBoolean(false);

        // Start background threads that continuously make requests
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                int requestCounter = 0;
                while (!stopTest.get()) {
                    try {
                        CreateUserRequest request = new CreateUserRequest();
                        request.setUsername("sustained" + threadId + "_" + requestCounter);
                        request.setEmail("sustained" + threadId + "_" + requestCounter + "@test.com");
                        request.setPassword("Password123!");

                        mockMvc.perform(post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andDo(result -> {
                                    totalRequests.incrementAndGet();
                                    if (result.getResponse().getStatus() == 201) {
                                        successfulRequests.incrementAndGet();
                                    }
                                });

                        requestCounter++;
                        Thread.sleep(100); // Small delay between requests
                    } catch (Exception e) {
                        totalRequests.incrementAndGet();
                    }
                }
                return null;
            });
        }

        // Let the test run for the specified duration
        Thread.sleep(durationSeconds * 1000);
        stopTest.set(true);

        // Wait a bit for threads to finish current requests
        Thread.sleep(2000);

        double successRate = totalRequests.get() > 0 ? 
            (double) successfulRequests.get() / totalRequests.get() * 100 : 0;
        double requestsPerSecond = (double) totalRequests.get() / durationSeconds;

        System.out.println("Sustained High Load Test Results:");
        System.out.println("Test Duration: " + durationSeconds + " seconds");
        System.out.println("Total Requests: " + totalRequests.get());
        System.out.println("Successful Requests: " + successfulRequests.get());
        System.out.println("Success Rate: " + String.format("%.2f%%", successRate));
        System.out.println("Requests per Second: " + String.format("%.2f", requestsPerSecond));

        // System should maintain performance over sustained load
        assertTrue(totalRequests.get() > 0, "Should process requests during sustained load");
        assertTrue(successRate > 50, "Should maintain >50% success rate during sustained load");
        assertTrue(requestsPerSecond > 1, "Should maintain reasonable throughput");
    }

    @Test
    @DisplayName("Test resource cleanup under stress")
    void testResourceCleanupUnderStress() throws Exception {
        // Create initial data
        for (int i = 0; i < 50; i++) {
            User user = new User();
            user.setUsername("cleanupuser" + i);
            user.setEmail("cleanup" + i + "@test.com");
            user.setPassword("encoded_password");
            userRepository.save(user);
        }

        int threadCount = 15;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger operationsCompleted = new AtomicInteger(0);

        // Simulate mixed operations (create, read, update, delete)
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    // Perform various operations
                    for (int j = 0; j < 5; j++) {
                        try {
                            switch (j % 4) {
                                case 0: // Create
                                    CreateUserRequest createRequest = new CreateUserRequest();
                                    createRequest.setUsername("stresscleanup" + threadId + "_" + j);
                                    createRequest.setEmail("stresscleanup" + threadId + "_" + j + "@test.com");
                                    createRequest.setPassword("Password123!");
                                    
                                    mockMvc.perform(post("/api/users")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(createRequest)));
                                    break;
                                    
                                case 1: // Read
                                    mockMvc.perform(get("/api/users")
                                            .param("page", "0")
                                            .param("size", "10"));
                                    break;
                                    
                                case 2: // Update (would need authentication)
                                case 3: // Delete (would need authentication)
                                    // These operations require authentication, so just do reads
                                    mockMvc.perform(get("/api/users")
                                            .param("page", String.valueOf(j))
                                            .param("size", "5"));
                                    break;
                            }
                            operationsCompleted.incrementAndGet();
                        } catch (Exception e) {
                            // Handle exceptions
                        }
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
        boolean completed = endLatch.await(90, TimeUnit.SECONDS);

        assertTrue(completed, "Resource cleanup stress test should complete");

        System.out.println("Resource Cleanup Stress Test Results:");
        System.out.println("Operations Completed: " + operationsCompleted.get());
        System.out.println("Final User Count: " + userRepository.count());

        // System should handle mixed operations without resource leaks
        assertTrue(operationsCompleted.get() > 0, "Some operations should complete");
        
        // Force garbage collection and check memory
        System.gc();
        long finalMemory = getUsedMemory();
        System.out.println("Final Memory Usage: " + formatBytes(finalMemory));
    }

    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}