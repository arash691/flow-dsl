package com.arash.ariani;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class AsyncFlowTest extends BaseFlowTest {

    @Test
    void testParallelExecution() {
        long startTime = System.currentTimeMillis();

        List<String> results = Flow.parallel(
                delayedSupplier(100, "First"),
                delayedSupplier(100, "Second"),
                delayedSupplier(100, "Third")
        ).execute();

        long duration = System.currentTimeMillis() - startTime;
        assertEquals(3, results.size());
        assertTrue(duration < 300); // Should take ~100ms, not 300ms
    }

    @Test
    void testParallelMap() {
        List<Integer> numbers = List.of(1, 2, 3, 4, 5);
        long startTime = System.currentTimeMillis();

        List<Integer> results = Flow.just(numbers)
                .flatMap(nums -> Flow.of(() -> nums.stream()
                        .parallel()
                        .map(n -> {
                            try {
                                Thread.sleep(100);
                                return n * 2;
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new RuntimeException(e);
                            }
                        })
                        .collect(Collectors.toList())))
                .execute();

        long duration = System.currentTimeMillis() - startTime;
        assertEquals(5, results.size());
        assertTrue(duration < 500); // Should take ~100ms, not 500ms
    }

    @Test
    void testParallelFlatMap() {
        List<Integer> numbers = List.of(1, 2, 3);
        long startTime = System.currentTimeMillis();

        List<String> results = Flow.just(numbers)
                .flatMap(nums -> Flow.of(() -> nums.stream()
                        .parallel()
                        .map(n -> {
                            try {
                                Thread.sleep(100);
                                return "Number: " + n;
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new RuntimeException(e);
                            }
                        })
                        .collect(Collectors.toList())))
                .execute();

        long duration = System.currentTimeMillis() - startTime;
        assertEquals(3, results.size());
        assertTrue(duration < 300); // Should take ~100ms, not 300ms
    }

    @Test
    void testAsyncExecution() {
        CompletableFuture<String> future = Flow.of(delayedSupplier(100, "Async"))
                .executeAsync();

        assertFalse(future.isDone()); // Should not be done immediately
        assertEquals("Async", future.join());
    }

    @Test
    void testAsyncComposition() {
        CompletableFuture<Integer> future = Flow.of(() -> 1)
                .map(n -> n + 1)
                .executeAsync()
                .thenCompose(n ->
                        Flow.of(() -> n * 2)
                                .executeAsync()
                );

        assertEquals(4, future.join());
    }

    @Test
    void testParallelWithErrors() {
        assertThrows(FlowExecutionException.class, () ->
                Flow.parallel(
                        () -> "Success",
                        () -> {
                            throw new RuntimeException("Parallel failure");
                        },
                        () -> "Also success"
                ).execute()
        );
    }

    @Test
    void testAsyncWithTimeout() {
        CompletableFuture<String> future = Flow.of(delayedSupplier(500, "Late"))
                .withTimeout(Duration.ofMillis(100))
                .executeAsync();

        ExecutionException exception = assertThrows(ExecutionException.class, () ->
                future.get(1, TimeUnit.SECONDS)
        );
        assertInstanceOf(FlowTimeoutException.class, exception.getCause());
        assertTrue(exception.getCause().getMessage().contains("timed out"));
    }

    @Test
    void testParallelWithCustomThreads() {
        List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6, 7, 8);

        List<Integer> results = Flow.just(numbers)
                .flatMap(nums -> Flow.of(() -> nums.stream()
                        .map(n -> {
                            try {
                                Thread.sleep(100);
                                return n * 2;
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new RuntimeException(e);
                            }
                        })
                        .collect(Collectors.toList())))
                .withParallelism(4)
                .execute();

        assertEquals(8, results.size());
        assertTrue(results.contains(16));
    }

    @Test
    void testAsyncErrorHandling() {
        AtomicInteger errorCount = new AtomicInteger(0);
        CompletableFuture<String> future = Flow.<String>of(() -> {
                    throw new RuntimeException("Async error");
                })
                .onError(e -> errorCount.incrementAndGet())
                .executeAsync();

        assertThrows(RuntimeException.class, future::join);
        assertEquals(1, errorCount.get());
    }
} 