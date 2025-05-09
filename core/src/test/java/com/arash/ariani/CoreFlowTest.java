package com.arash.ariani;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CoreFlowTest extends BaseFlowTest {

    @Test
    void testFluentApi() {
        String result = Flow.of(() -> "Hello")
            .map(str -> str + " ")
            .map(str -> str + "World")
            .map(String::trim)
            .map(String::toUpperCase)
            .execute();

        assertEquals("HELLO WORLD", result);
    }

    @Test
    void testMapOperations() {
        Integer result = Flow.of(() -> "42")
            .map(Integer::parseInt)
            .map(n -> n * 2)
            .map(n -> n + 8)
            .execute();

        assertEquals(92, result);
    }

    @Test
    void testFlatMapOperations() {
        Integer result = Flow.of(() -> 5)
            .flatMap(n -> Flow.of(() -> n * 2))
            .flatMap(n -> Flow.of(() -> n + 3))
            .execute();

        assertEquals(13, result);
    }

    @Test
    void testFilterSuccess() {
        Integer result = Flow.of(() -> 42)
            .filter(n -> n > 0)
            .filter(n -> n % 2 == 0)
            .execute();

        assertEquals(42, result);
    }

    @Test
    void testFilterFailure() {
        assertThrows(FlowFilterException.class, () ->
            Flow.of(() -> 42)
                .filter(n -> n < 0)
                .execute()
        );
    }

    @Test
    void testConditionalMapping() {
        String result = Flow.of(() -> 15)
            .map(n -> n > 10 ? "High" : "Low")
            .execute();

        assertEquals("High", result);
    }

    @Test
    void testChainedFiltersAndMaps() {
        Integer result = Flow.of(() -> 100)
            .filter(n -> n > 50)
            .map(n -> n / 2)
            .filter(n -> n < 100)
            .map(n -> n * 3)
            .execute();

        assertEquals(150, result);
    }

    @Test
    void testJust() {
        String result = Flow.just("Hello")
            .map(str -> str + " World")
            .execute();

        assertEquals("Hello World", result);
    }

    @Test
    void testNestedFlows() {
        Integer result = Flow.of(() -> 1)
            .flatMap(n -> Flow.of(() -> n + 1)
                .flatMap(m -> Flow.of(() -> m * 2)))
            .execute();

        assertEquals(4, result);
    }

    @Test
    void testTypeConversion() {
        Boolean result = Flow.of(() -> "true")
            .map(Boolean::parseBoolean)
            .filter(b -> b)
            .execute();

        assertTrue(result);
    }
} 