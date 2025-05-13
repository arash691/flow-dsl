package com.arash.ariani;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class ParallelFlowTest extends BaseFlowTest {

    @Test
    @DisplayName("Test parallel map with controlled parallelism")
    void testParallelMapWithControlledParallelism() {
        // Given
        List<Integer> numbers = IntStream.range(0, 100).boxed().toList();
        AtomicInteger concurrentExecutions = new AtomicInteger(0);
        AtomicInteger maxConcurrentExecutions = new AtomicInteger(0);
        int maxParallelism = 5;

        // When
        @SuppressWarnings("unchecked")
        Supplier<Integer>[] suppliers = numbers.stream()
                .map(num -> (Supplier<Integer>) () -> {
                    int current = concurrentExecutions.incrementAndGet();
                    maxConcurrentExecutions.set(Math.max(maxConcurrentExecutions.get(), current));
                    try {
                        Thread.sleep(10); // Simulate some work
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new FlowExecutionException("Interrupted during parallel execution", e);
                    }
                    concurrentExecutions.decrementAndGet();
                    return num * 2;
                })
                .toArray(Supplier[]::new);

        List<Integer> result = Flow.parallel(maxParallelism, suppliers)
                .execute();

        // Then
        assertEquals(numbers.size(), result.size());
        assertTrue(maxConcurrentExecutions.get() <= maxParallelism,
                "Max concurrent executions exceeded configured parallelism");
        for (int i = 0; i < numbers.size(); i++) {
            assertEquals(numbers.get(i) * 2, result.get(i));
        }
    }

    @Test
    @DisplayName("Test parallel execution with checked exceptions")
    void testParallelExecutionWithCheckedExceptions() {
        // Given
        List<String> inputs = List.of("valid", "invalid", "valid2");

        // When/Then
        @SuppressWarnings("unchecked")
        Supplier<String>[] suppliers = inputs.stream()
                .map(str -> (Supplier<String>) () -> {
                    try {
                        if (str.equals("invalid")) {
                            throw new Exception("Simulated checked exception");
                        }
                        return str.toUpperCase();
                    } catch (Exception e) {
                        throw new FlowExecutionException("Error in parallel execution", e);
                    }
                })
                .toArray(Supplier[]::new);

        assertThrows(FlowExecutionException.class, () -> {
            Flow.parallel(suppliers).execute();
        });
    }

    @Test
    @DisplayName("Test parallel execution with context propagation")
    void testParallelExecutionWithContext() {
        // Given
        List<Integer> numbers = List.of(1, 2, 3, 4, 5);
        String contextKey = "multiplier";
        Integer multiplier = 10;

        // When
        @SuppressWarnings("unchecked")
        Supplier<Integer>[] suppliers = numbers.stream()
                .map(num -> (Supplier<Integer>) () -> {
                    Integer contextMultiplier = Flow.currentContext().<Integer>get(contextKey, Integer.class)
                            .orElseThrow(() -> new FlowExecutionException("Context value not found"));
                    return num * contextMultiplier;
                })
                .toArray(Supplier[]::new);

        List<Integer> result = Flow.parallel(suppliers)
                .withContextData(contextKey, multiplier)
                .execute();

        // Then
        assertEquals(numbers.size(), result.size());
        for (int i = 0; i < numbers.size(); i++) {
            assertEquals(numbers.get(i) * multiplier, result.get(i));
        }
    }

    @Test
    @DisplayName("Test parallel execution with timeout")
    void testParallelExecutionWithTimeout() {
        // Given
        List<Integer> numbers = IntStream.range(0, 10).boxed().toList();

        // When/Then
        @SuppressWarnings("unchecked")
        Supplier<Integer>[] suppliers = numbers.stream()
                .map(num -> (Supplier<Integer>) () -> {
                    if (num > 5) {
                        try {
                            Thread.sleep(200); // Simulate long operation
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new FlowExecutionException("Interrupted during parallel execution", e);
                        }
                    }
                    return num * 2;
                })
                .toArray(Supplier[]::new);

        assertThrows(FlowTimeoutException.class, () -> {
            Flow.parallel(suppliers)
                    .withTimeout(Duration.ofMillis(100))
                    .execute();
        });
    }

    @Test
    @DisplayName("Test parallel map with cleanup")
    void testParallelMapWithCleanup() {
        // Given
        List<String> resources = List.of("resource1", "resource2", "resource3");
        AtomicInteger cleanupCount = new AtomicInteger(0);

        // When
        @SuppressWarnings("unchecked")
        Supplier<String>[] suppliers = resources.stream()
                .map(resource -> (Supplier<String>) () -> {
                    try {
                        return resource.toUpperCase();
                    } finally {
                        cleanupCount.incrementAndGet();
                    }
                })
                .toArray(Supplier[]::new);

        List<String> result = Flow.parallel(suppliers)
                .execute();

        // Then
        assertEquals(resources.size(), result.size());
        assertEquals(resources.size(), cleanupCount.get(),
                "Cleanup should be called for each resource");
    }
} 