package com.arash.ariani.examples.async;

import com.arash.ariani.Flow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Examples demonstrating asynchronous and parallel execution patterns in Flow DSL.
 */
public class AsyncParallelExamples {
    private static final Logger log = LoggerFactory.getLogger(AsyncParallelExamples.class);

    /**
     * Basic async execution example
     */
    public static void basicAsyncExecution() {
        CompletableFuture<String> future = Flow.of(() -> {
            Thread.sleep(1000);
            return "Async result";
        })
        .executeAsync();

        // Do other work while waiting
        log.info("Doing other work while waiting for async result...");

        String result = future.join();
        log.info("Async result: {}", result);
    }

    /**
     * Parallel execution example
     */
    public static void parallelExecution() {
        List<String> results = Flow.parallel(
            () -> {
                Thread.sleep(1000);
                return "Task 1";
            },
            () -> {
                Thread.sleep(1000);
                return "Task 2";
            },
            () -> {
                Thread.sleep(1000);
                return "Task 3";
            }
        )
        .withParallelism(3)
        .execute();

        log.info("Parallel results: {}", results);
    }

    /**
     * Parallel map example
     */
    public static void parallelMap() {
        List<Integer> numbers = List.of(1, 2, 3, 4, 5);

        List<String> results = Flow.of(() -> numbers)
            .parallelMap(n -> {
                Thread.sleep(500);
                return "Processed " + n;
            })
            .execute();

        log.info("Parallel map results: {}", results);
    }

    /**
     * Parallel flatMap example
     */
    public static void parallelFlatMap() {
        List<Integer> numbers = List.of(1, 2, 3);

        List<String> results = Flow.just(numbers)
            .parallelFlatMap(n -> Flow.of(() -> {
                Thread.sleep(500);
                return "Number: " + n;
            }))
            .execute();

        log.info("Parallel flatMap results: {}", results);
    }

    /**
     * Async composition example
     */
    public static void asyncComposition() {
        CompletableFuture<Integer> future = Flow.of(() -> 1)
            .map(n -> n + 1)
            .executeAsync()
            .thenCompose(n -> 
                Flow.of(() -> n * 2)
                    .executeAsync()
            );

        log.info("Async composition result: {}", future.join());
    }

    /**
     * Complex parallel workflow example
     */
    public static void complexParallelWorkflow() {
        List<String> items = List.of("Item1", "Item2", "Item3");

        Flow.of(() -> items)
            // First parallel processing stage
            .parallelMap(item -> {
                Thread.sleep(500);
                return "Processed " + item;
            })
            // Second parallel processing stage
            .parallelFlatMap(processed -> Flow.of(() -> {
                Thread.sleep(300);
                return "Validated " + processed;
            }))
            // Final aggregation
            .map(results -> results.stream()
                .collect(Collectors.joining(", ")))
            .withTimeout(Duration.ofSeconds(5))
            .onComplete(result -> log.info("Complex workflow result: {}", result))
            .execute();
    }

    public static void main(String[] args) throws InterruptedException {
        log.info("Running async and parallel examples...");

        log.info("\nBasic Async Execution:");
        basicAsyncExecution();

        log.info("\nParallel Execution:");
        parallelExecution();

        log.info("\nParallel Map:");
        parallelMap();

        log.info("\nParallel FlatMap:");
        parallelFlatMap();

        log.info("\nAsync Composition:");
        asyncComposition();

        log.info("\nComplex Parallel Workflow:");
        complexParallelWorkflow();

        log.info("Async and parallel examples completed.");
    }
} 