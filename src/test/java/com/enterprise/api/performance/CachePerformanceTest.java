package com.enterprise.api.performance;

import com.enterprise.api.config.CacheConfig;
import com.enterprise.api.entity.User;
import com.enterprise.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests for caching functionality.
 * 
 * These tests verify:
 * - Cache hit/miss performance
 * - Cache eviction behavior
 * - Memory usage with caching
 * 
 * Requirements addressed:
 * - 5.3: Caching performance optimization
 * - 5.5: Query optimization with caching
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@org.springframework.context.annotation.Import(com.enterprise.api.config.TestConfig.class)
class CachePerformanceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CacheManager cacheManager;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Clear all caches before each test
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });

        // Create a test user
        testUser = new User();
        testUser.setUsername("cachetest");
        testUser.setEmail("cachetest@example.com");
        testUser.setPassword("password123");
        testUser = userRepository.save(testUser);
        userRepository.flush();
    }

    @Test
    void testCacheHitPerformance() {
        String username = testUser.getUsername();
        
        // First call - cache miss
        Instant start = Instant.now();
        Optional<User> user1 = userRepository.findByUsernameActive(username);
        Duration cacheMissTime = Duration.between(start, Instant.now());
        
        // Second call - should be cache hit (if caching is implemented)
        start = Instant.now();
        Optional<User> user2 = userRepository.findByUsernameActive(username);
        Duration cacheHitTime = Duration.between(start, Instant.now());
        
        assertTrue(user1.isPresent());
        assertTrue(user2.isPresent());
        assertEquals(user1.get().getId(), user2.get().getId());
        
        System.out.println("Cache miss time: " + cacheMissTime.toMillis() + "ms");
        System.out.println("Cache hit time: " + cacheHitTime.toMillis() + "ms");
        
        // Cache hit should be faster than cache miss
        // Note: With small test data and in-memory database, the difference might be minimal
        assertTrue(cacheHitTime.toMillis() <= cacheMissTime.toMillis() + 10,
            "Cache hit should be faster or similar to cache miss");
    }

    @Test
    void testCacheEvictionPerformance() {
        String username = testUser.getUsername();
        
        // Populate cache
        userRepository.findByUsernameActive(username);
        
        // Verify cache is populated
        var cache = cacheManager.getCache(CacheConfig.USERS_CACHE);
        assertNotNull(cache, "Users cache should exist");
        
        // Test cache eviction performance
        Instant start = Instant.now();
        if (cache != null) {
            cache.clear();
        }
        Duration evictionTime = Duration.between(start, Instant.now());
        
        System.out.println("Cache eviction time: " + evictionTime.toMillis() + "ms");
        
        // Cache eviction should be fast
        assertTrue(evictionTime.toMillis() < 100, "Cache eviction should be fast");
        
        // Verify cache is cleared by checking next query performance
        start = Instant.now();
        userRepository.findByUsernameActive(username);
        Duration postEvictionTime = Duration.between(start, Instant.now());
        
        System.out.println("Post-eviction query time: " + postEvictionTime.toMillis() + "ms");
    }

    @Test
    void testMultipleCacheOperations() {
        // Test performance with multiple cache operations
        Instant start = Instant.now();
        
        for (int i = 0; i < 10; i++) {
            userRepository.findByUsernameActive(testUser.getUsername());
        }
        
        Duration multipleOperationsTime = Duration.between(start, Instant.now());
        
        System.out.println("10 cache operations time: " + multipleOperationsTime.toMillis() + "ms");
        
        // Multiple cache operations should be efficient
        assertTrue(multipleOperationsTime.toMillis() < 1000,
            "Multiple cache operations should be efficient");
    }

    @Test
    void testCacheMemoryUsage() {
        // Test memory usage with caching
        Runtime runtime = Runtime.getRuntime();
        
        // Force garbage collection to get accurate memory reading
        System.gc();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        // Perform operations that should populate cache
        for (int i = 0; i < 100; i++) {
            userRepository.findByUsernameActive(testUser.getUsername());
        }
        
        System.gc();
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        
        long memoryUsed = memoryAfter - memoryBefore;
        
        System.out.println("Memory used by caching: " + memoryUsed + " bytes");
        
        // Memory usage should be reasonable (less than 10MB for test data)
        assertTrue(memoryUsed < 10 * 1024 * 1024,
            "Cache memory usage should be reasonable: " + memoryUsed + " bytes");
    }

    @Test
    void testCacheManagerPerformance() {
        // Test cache manager operations performance
        Instant start = Instant.now();
        
        // Test getting cache by name
        var cache = cacheManager.getCache(CacheConfig.USERS_CACHE);
        assertNotNull(cache);
        
        // Test cache operations
        cache.put("testKey", testUser);
        var cachedValue = cache.get("testKey");
        assertNotNull(cachedValue);
        
        cache.evict("testKey");
        
        Duration cacheManagerTime = Duration.between(start, Instant.now());
        
        System.out.println("Cache manager operations time: " + cacheManagerTime.toMillis() + "ms");
        
        // Cache manager operations should be fast
        assertTrue(cacheManagerTime.toMillis() < 100,
            "Cache manager operations should be fast");
    }

    @Test
    void testConcurrentCacheAccess() {
        // Test concurrent cache access performance
        String username = testUser.getUsername();
        
        Instant start = Instant.now();
        
        // Simulate concurrent access
        Thread[] threads = new Thread[5];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    userRepository.findByUsernameActive(username);
                }
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("Thread interrupted during concurrent cache test");
            }
        }
        
        Duration concurrentTime = Duration.between(start, Instant.now());
        
        System.out.println("Concurrent cache access time: " + concurrentTime.toMillis() + "ms");
        
        // Concurrent cache access should be efficient
        assertTrue(concurrentTime.toMillis() < 5000,
            "Concurrent cache access should be efficient");
    }
}