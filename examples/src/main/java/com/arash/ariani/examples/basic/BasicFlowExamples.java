package com.arash.ariani.examples.basic;

import com.arash.ariani.Flow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Basic examples demonstrating core Flow DSL functionality.
 */
public class BasicFlowExamples {
    private static final Logger log = LoggerFactory.getLogger(BasicFlowExamples.class);

    /**
     * Simple transformation example
     */
    public static void simpleTransformation() {
        String result = Flow.of(() -> "Hello")
            .map(str -> str + " ")
            .map(str -> str + "World")
            .map(String::toUpperCase)
            .execute();

        log.info("Simple transformation result: {}", result);
    }

    /**
     * Filtering example
     */
    public static void filteringExample() {
        Flow.of(() -> 42)
            .filter(n -> n > 0)
            .filter(n -> n % 2 == 0)
            .map(n -> "Number " + n + " is positive and even")
            .onComplete(result -> log.info(result))
            .execute();
    }

    /**
     * Conditional flow example
     */
    public static void conditionalFlow() {
        Flow.of(() -> 15)
            .thenIf(
                n -> n > 10,
                n -> "Number " + n + " is greater than 10"
            )
            .otherwise(n -> "Number " + n + " is less than or equal to 10")
            .onComplete(result -> log.info(result))
            .execute();
    }

    /**
     * Flow composition example
     */
    public static void flowComposition() {
        Flow<String> flow1 = Flow.of(() -> "Hello");
        Flow<String> flow2 = Flow.of(() -> "World");

        String result = flow1.flatMap(hello ->
            flow2.map(world -> hello + " " + world)
        ).execute();

        log.info("Composed flow result: {}", result);
    }

    /**
     * Context sharing example
     */
    public static void contextSharing() {
        Flow.of(() -> "Initial data")
            .withContextData("key", "shared value")
            .flatMap(data -> Flow.of(() -> {
                String contextValue = Flow.just(data)
                    .getContext()
                    .get("key", String.class)
                    .orElse("default");
                return data + " with " + contextValue;
            }))
            .onComplete(result -> log.info(result))
            .execute();
    }

    /**
     * List processing example
     */
    public static void listProcessing() {
        List<Integer> numbers = List.of(1, 2, 3, 4, 5);

        Flow.of(() -> numbers)
            .map(nums -> nums.stream()
                .map(n -> n * 2)
                .toList())
            .map(nums -> nums.stream()
                .map(String::valueOf)
                .toList())
            .onComplete(result -> log.info("Processed numbers: {}", result))
            .execute();
    }

    public static void main(String[] args) {
        log.info("Running basic Flow DSL examples...");
        
        simpleTransformation();
        filteringExample();
        conditionalFlow();
        flowComposition();
        contextSharing();
        listProcessing();
        
        log.info("Basic examples completed.");
    }
} 