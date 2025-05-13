package com.arash.ariani;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.List;
import java.time.Duration;

class ExceptionHandlingTest extends BaseFlowTest {

    @Test
    @DisplayName("Test CheckedSupplier with successful execution")
    void testCheckedSupplierSuccess() {
        // Given
        Flow.CheckedSupplier<String, Exception> supplier = () -> "success";

        // When
        String result = Flow.ofChecked(supplier)
            .execute();

        // Then
        assertEquals("success", result);
    }

    @Test
    @DisplayName("Test CheckedSupplier with exception")
    void testCheckedSupplierException() {
        // Given
        Flow.CheckedSupplier<String, IOException> supplier = () -> {
            throw new IOException("Simulated IO error");
        };

        // When/Then
        assertThrows(FlowExecutionException.class, () -> {
            Flow.ofChecked(supplier)
                .execute();
        });
    }

    @Test
    @DisplayName("Test CheckedFunction with successful execution")
    void testCheckedFunctionSuccess() {
        // Given
        Flow.CheckedFunction<Integer, String, Exception> function = (num) -> String.valueOf(num * 2);

        // When
        String result = Flow.just(5)
            .mapChecked(function)
            .execute();

        // Then
        assertEquals("10", result);
    }

    @Test
    @DisplayName("Test wrap utility with checked exception")
    void testWrapUtility() {
        // Given
        Flow.CheckedFunction<String, Integer, NumberFormatException> parser = str -> {
            if (str.equals("invalid")) {
                throw new NumberFormatException("Invalid number");
            }
            return Integer.parseInt(str);
        };

        // When/Then
        assertThrows(FlowExecutionException.class, () -> {
            Flow.just("invalid")
                .mapChecked(parser)
                .execute();
        });
    }

    @Test
    @DisplayName("Test exception propagation in nested flows")
    void testExceptionPropagationInNestedFlows() {
        // Given
        Flow<Integer> innerFlow = Flow.of(() -> {
            throw new RuntimeException("Inner flow error");
        });

        // When/Then
        assertThrows(FlowExecutionException.class, () -> {
            Flow.just(42)
                .flatMap(value -> innerFlow)
                .execute();
        });
    }

    @Test
    @DisplayName("Test error recovery with alternative value")
    void testErrorRecoveryWithAlternative() {
        // Given
        Flow.CheckedSupplier<String, IOException> failingSupplier = () -> {
            throw new IOException("Primary source failed");
        };

        // When
        String result = Flow.ofChecked(failingSupplier)
            .withFallback(() -> "fallback value")
            .execute();

        // Then
        assertEquals("fallback value", result);
    }

    @Test
    @DisplayName("Test error recovery with type-specific handling")
    void testErrorRecoveryWithTypeSpecificHandling() {
        // Given
        Flow.CheckedSupplier<String, IOException> failingSupplier = () -> {
            throw new IOException("IO error occurred");
        };

        // When
        String result = Flow.ofChecked(failingSupplier)
            .withFallback(() -> {
                try {
                    throw new IOException("IO error occurred");
                } catch (IOException e) {
                    return "IO error handled";
                } catch (RuntimeException e) {
                    return "Runtime error handled";
                }
            })
            .execute();

        // Then
        assertEquals("IO error handled", result);
    }
} 