package com.arash.ariani.examples.error;

import com.arash.ariani.Flow;
import com.arash.ariani.FlowTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Examples demonstrating error handling and resilience patterns in Flow DSL.
 */
public class ErrorHandlingExamples {
    private static final Logger log = LoggerFactory.getLogger(ErrorHandlingExamples.class);

    /**
     * Basic error handling example
     */
    public static void basicErrorHandling() {
        Flow.of(() -> {
            throw new RuntimeException("Simulated error");
        })
        .onError(error -> log.error("Caught error: {}", error.getMessage()))
        .onComplete(result -> log.info("Flow completed with: {}", result))
        .execute();
    }

    /**
     * Retry pattern example
     */
    public static void retryPattern() {
        AtomicInteger attempts = new AtomicInteger(0);

        Flow.of(() -> {
            if (attempts.incrementAndGet() <= 2) {
                throw new RuntimeException("Attempt " + attempts.get() + " failed");
            }
            return "Success on attempt " + attempts.get();
        })
        .withRetry(3)
        .withBackoff(Duration.ofMillis(100))
        .onError(error -> log.error("Error occurred: {}", error.getMessage()))
        .onComplete(result -> log.info(result))
        .execute();
    }

    /**
     * Timeout handling example
     */
    public static void timeoutHandling() {
        try {
            Flow.of(() -> {
                Thread.sleep(2000);
                return "Delayed result";
            })
            .withTimeout(Duration.ofSeconds(1))
            .execute();
        } catch (FlowTimeoutException e) {
            log.error("Flow timed out: {}", e.getMessage());
        }
    }

    /**
     * Compensation actions example
     */
    public static void compensationActions() {
        Flow.of(() -> {
            log.info("Performing main action");
            throw new RuntimeException("Main action failed");
        })
        .withCompensation(result -> log.info("Compensating for failure"))
        .onError(error -> log.error("Error occurred: {}", error.getMessage()))
        .execute();
    }

    /**
     * Circuit breaker pattern example
     */
    public static void circuitBreakerPattern() {
        Flow<String> flow = Flow.of(() -> {
            throw new RuntimeException("Service failure");
        });

        // Execute multiple times to demonstrate circuit breaker
        for (int i = 0; i < 6; i++) {
            try {
                flow.execute();
            } catch (Exception e) {
                log.error("Execution {} failed: {}", i + 1, e.getMessage());
            }
        }
    }

    /**
     * Combined resilience patterns example
     */
    public static void combinedResilience() {
        AtomicInteger attempts = new AtomicInteger(0);

        Flow.of(() -> {
            if (attempts.incrementAndGet() <= 2) {
                throw new RuntimeException("Temporary failure");
            }
            return "Success";
        })
        .withRetry(3)
        .withBackoff(Duration.ofMillis(100))
        .withTimeout(Duration.ofSeconds(1))
        .withCompensation(result -> log.info("Compensating for attempt {}", attempts.get()))
        .onError(e -> log.error("Error in attempt {}: {}", attempts.get(), e.getMessage()))
        .onComplete(result -> log.info("Final result: {}", result))
        .execute();
    }

    public static void main(String[] args) {
        log.info("Running error handling examples...");

        log.info("\nBasic Error Handling:");
        basicErrorHandling();

        log.info("\nRetry Pattern:");
        retryPattern();

        log.info("\nTimeout Handling:");
        timeoutHandling();

        log.info("\nCompensation Actions:");
        compensationActions();

        log.info("\nCircuit Breaker Pattern:");
        circuitBreakerPattern();

        log.info("\nCombined Resilience:");
        combinedResilience();

        log.info("Error handling examples completed.");
    }
} 