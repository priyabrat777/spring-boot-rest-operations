package com.enterprise.api.performance;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Utility class for performance benchmarking.
 * 
 * This utility provides:
 * - Method execution timing
 * - Statistical analysis of performance metrics
 * - Memory usage monitoring
 * - Benchmark result reporting
 * 
 * Requirements addressed:
 * - 5.3: Performance optimization measurement
 * - 5.5: Query optimization benchmarking
 */
public final class BenchmarkUtil {

    private BenchmarkUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Benchmark result containing performance metrics.
     */
    public static class BenchmarkResult {
        private final String operationName;
        private final List<Long> executionTimes;
        private final long totalTime;
        private final long averageTime;
        private final long minTime;
        private final long maxTime;
        private final long memoryUsed;

        public BenchmarkResult(String operationName, List<Long> executionTimes, long memoryUsed) {
            this.operationName = operationName;
            this.executionTimes = new ArrayList<>(executionTimes);
            this.totalTime = executionTimes.stream().mapToLong(Long::longValue).sum();
            this.averageTime = executionTimes.isEmpty() ? 0 : totalTime / executionTimes.size();
            this.minTime = executionTimes.stream().mapToLong(Long::longValue).min().orElse(0);
            this.maxTime = executionTimes.stream().mapToLong(Long::longValue).max().orElse(0);
            this.memoryUsed = memoryUsed;
        }

        public String getOperationName() { return operationName; }
        public List<Long> getExecutionTimes() { return new ArrayList<>(executionTimes); }
        public long getTotalTime() { return totalTime; }
        public long getAverageTime() { return averageTime; }
        public long getMinTime() { return minTime; }
        public long getMaxTime() { return maxTime; }
        public long getMemoryUsed() { return memoryUsed; }

        @Override
        public String toString() {
            return String.format(
                "Benchmark[%s]: avg=%dms, min=%dms, max=%dms, total=%dms, runs=%d, memory=%d bytes",
                operationName, averageTime, minTime, maxTime, totalTime, executionTimes.size(), memoryUsed
            );
        }
    }

    /**
     * Benchmark a single operation execution.
     * 
     * @param operationName name of the operation being benchmarked
     * @param operation the operation to benchmark
     * @param <T> return type of the operation
     * @return benchmark result
     */
    public static <T> BenchmarkResult benchmarkSingle(String operationName, Supplier<T> operation) {
        return benchmark(operationName, operation, 1);
    }

    /**
     * Benchmark an operation multiple times.
     * 
     * @param operationName name of the operation being benchmarked
     * @param operation the operation to benchmark
     * @param iterations number of times to run the operation
     * @param <T> return type of the operation
     * @return benchmark result
     */
    public static <T> BenchmarkResult benchmark(String operationName, Supplier<T> operation, int iterations) {
        List<Long> executionTimes = new ArrayList<>();
        
        // Warm up JVM
        for (int i = 0; i < Math.min(iterations / 10, 10); i++) {
            operation.get();
        }
        
        // Force garbage collection before measurement
        System.gc();
        long memoryBefore = getUsedMemory();
        
        // Perform actual benchmarking
        for (int i = 0; i < iterations; i++) {
            Instant start = Instant.now();
            operation.get();
            Duration duration = Duration.between(start, Instant.now());
            executionTimes.add(duration.toMillis());
        }
        
        System.gc();
        long memoryAfter = getUsedMemory();
        long memoryUsed = memoryAfter - memoryBefore;
        
        return new BenchmarkResult(operationName, executionTimes, memoryUsed);
    }

    /**
     * Benchmark an operation with warmup and multiple iterations.
     * 
     * @param operationName name of the operation being benchmarked
     * @param operation the operation to benchmark
     * @param warmupIterations number of warmup iterations
     * @param benchmarkIterations number of benchmark iterations
     * @param <T> return type of the operation
     * @return benchmark result
     */
    public static <T> BenchmarkResult benchmarkWithWarmup(String operationName, Supplier<T> operation, 
                                                          int warmupIterations, int benchmarkIterations) {
        // Warmup phase
        for (int i = 0; i < warmupIterations; i++) {
            operation.get();
        }
        
        // Actual benchmark
        return benchmark(operationName, operation, benchmarkIterations);
    }

    /**
     * Compare two operations performance.
     * 
     * @param operation1Name name of the first operation
     * @param operation1 the first operation to benchmark
     * @param operation2Name name of the second operation
     * @param operation2 the second operation to benchmark
     * @param iterations number of iterations for each operation
     * @param <T> return type of the operations
     * @return comparison result
     */
    public static <T> String compareOperations(String operation1Name, Supplier<T> operation1,
                                             String operation2Name, Supplier<T> operation2,
                                             int iterations) {
        BenchmarkResult result1 = benchmark(operation1Name, operation1, iterations);
        BenchmarkResult result2 = benchmark(operation2Name, operation2, iterations);
        
        StringBuilder comparison = new StringBuilder();
        comparison.append("Performance Comparison:\n");
        comparison.append(result1.toString()).append("\n");
        comparison.append(result2.toString()).append("\n");
        
        double speedupFactor = (double) result2.getAverageTime() / result1.getAverageTime();
        if (speedupFactor > 1.1) {
            comparison.append(String.format("%s is %.2fx faster than %s\n", 
                operation1Name, speedupFactor, operation2Name));
        } else if (speedupFactor < 0.9) {
            comparison.append(String.format("%s is %.2fx faster than %s\n", 
                operation2Name, 1.0 / speedupFactor, operation1Name));
        } else {
            comparison.append("Both operations have similar performance\n");
        }
        
        return comparison.toString();
    }

    /**
     * Measure memory usage of an operation.
     * 
     * @param operationName name of the operation
     * @param operation the operation to measure
     * @param <T> return type of the operation
     * @return memory usage in bytes
     */
    public static <T> long measureMemoryUsage(String operationName, Supplier<T> operation) {
        System.gc();
        long memoryBefore = getUsedMemory();
        
        operation.get();
        
        System.gc();
        long memoryAfter = getUsedMemory();
        
        long memoryUsed = memoryAfter - memoryBefore;
        System.out.println(operationName + " memory usage: " + formatBytes(memoryUsed));
        
        return memoryUsed;
    }

    /**
     * Get current used memory.
     * 
     * @return used memory in bytes
     */
    private static long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    /**
     * Format bytes into human-readable format.
     * 
     * @param bytes number of bytes
     * @return formatted string
     */
    public static String formatBytes(long bytes) {
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

    /**
     * Create a performance report from multiple benchmark results.
     * 
     * @param results list of benchmark results
     * @return formatted performance report
     */
    public static String createPerformanceReport(List<BenchmarkResult> results) {
        StringBuilder report = new StringBuilder();
        report.append("=== Performance Report ===\n");
        
        for (BenchmarkResult result : results) {
            report.append(result.toString()).append("\n");
        }
        
        // Find fastest and slowest operations
        if (results.size() > 1) {
            BenchmarkResult fastest = results.stream()
                .min((r1, r2) -> Long.compare(r1.getAverageTime(), r2.getAverageTime()))
                .orElse(null);
            
            BenchmarkResult slowest = results.stream()
                .max((r1, r2) -> Long.compare(r1.getAverageTime(), r2.getAverageTime()))
                .orElse(null);
            
            if (fastest != null && slowest != null && !fastest.equals(slowest)) {
                double speedDifference = (double) slowest.getAverageTime() / fastest.getAverageTime();
                report.append(String.format("\nFastest: %s (avg: %dms)\n", 
                    fastest.getOperationName(), fastest.getAverageTime()));
                report.append(String.format("Slowest: %s (avg: %dms)\n", 
                    slowest.getOperationName(), slowest.getAverageTime()));
                report.append(String.format("Speed difference: %.2fx\n", speedDifference));
            }
        }
        
        return report.toString();
    }

    /**
     * Assert that an operation meets performance requirements.
     * 
     * @param result benchmark result
     * @param maxAverageTimeMs maximum allowed average time in milliseconds
     * @param maxMemoryBytes maximum allowed memory usage in bytes
     * @throws AssertionError if performance requirements are not met
     */
    public static void assertPerformance(BenchmarkResult result, long maxAverageTimeMs, long maxMemoryBytes) {
        if (result.getAverageTime() > maxAverageTimeMs) {
            throw new AssertionError(String.format(
                "Performance requirement not met for %s: average time %dms > %dms",
                result.getOperationName(), result.getAverageTime(), maxAverageTimeMs
            ));
        }
        
        if (result.getMemoryUsed() > maxMemoryBytes) {
            throw new AssertionError(String.format(
                "Memory requirement not met for %s: memory used %s > %s",
                result.getOperationName(), formatBytes(result.getMemoryUsed()), formatBytes(maxMemoryBytes)
            ));
        }
    }
}