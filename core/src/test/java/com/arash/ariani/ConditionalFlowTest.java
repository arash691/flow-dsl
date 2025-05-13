package com.arash.ariani;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConditionalFlowTest extends BaseFlowTest {

    @Test
    void testThenIfWithTrueCondition() {
        Integer result = Flow.of(() -> 42)
                .<Integer>thenIf(
                        n -> n > 40,
                        n -> n * 2
                )
                .map(n -> n != null ? n : 42)
                .execute();

        assertEquals(84, result);
    }

    @Test
    void testThenIfWithFalseCondition() {
        Integer result = Flow.of(() -> 42)
                .<Integer>thenIf(
                        n -> n < 40,
                        n -> n * 2
                )
                .map(n -> n != null ? n : 42)
                .execute();

        assertEquals(42, result);
    }

    @Test
    void testThenIfWithOtherwiseWhenTrue() {
        String result = Flow.of(() -> 100)
                .<String>thenIf(
                        n -> n > 50,
                        n -> "High"
                )
                .map(s -> s != null ? s : "Low")
                .execute();

        assertEquals("High", result);
    }

    @Test
    void testThenIfWithOtherwiseWhenFalse() {
        String result = Flow.of(() -> 30)
                .<String>thenIf(
                        n -> n > 50,
                        n -> "High"
                )
                .map(s -> s != null ? s : "Low")
                .execute();

        assertEquals("Low", result);
    }

    @Test
    void testChainedConditionals() {
        String result = Flow.of(() -> 75)
                .<String>map(n -> {
                    if (n > 90) return "A+";
                    if (n > 80) return "A";
                    if (n > 70) return "B";
                    if (n > 60) return "C";
                    return "D";
                })
                .execute();

        assertEquals("B", result);
    }

    @Test
    void testOtherwiseWithoutThenIf() {
        assertThrows(IllegalStateException.class, () ->
                Flow.of(() -> 42)
                        .otherwise(n -> n * 2)
                        .execute()
        );
    }

    @Test
    void testConditionalWithTransformation() {
        record User(String name, int age) {
        }

        User result = Flow.of(() -> new User("John", 25))
                .<User>thenIf(
                        user -> user.age() >= 18,
                        user -> new User(user.name() + " (Adult)", user.age())
                )
                .map(user -> user != null ? user : new User("John (Minor)", 25))
                .execute();

        assertEquals("John (Adult)", result.name());
        assertEquals(25, result.age());
    }

    @Test
    void testNestedConditionals() {
        Integer result = Flow.of(() -> 42)
                .<Integer>thenIf(
                        n -> n > 40,
                        n -> Flow.of(() -> n)
                                .<Integer>thenIf(
                                        m -> m % 2 == 0,
                                        m -> m + 10
                                )
                                .map(m -> m != null ? m : n - 10)
                                .execute()
                )
                .map(n -> n != null ? n : 42)
                .execute();

        assertEquals(52, result);
    }

    @Test
    void testTypeTransformingConditionals() {
        Status result = Flow.of(() -> 200)
                .<Status>map(code -> {
                    if (code >= 200 && code < 300) {
                        return new Status(code, "Success");
                    }
                    return new Status(code, "Error");
                })
                .execute();

        assertEquals(200, result.code());
        assertEquals("Success", result.message());
    }

    record Status(int code, String message) {
    }
} 