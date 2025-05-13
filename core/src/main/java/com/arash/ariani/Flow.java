package com.arash.ariani;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Core interface for the Flow DSL that provides a fluent API for defining business flows.
 *
 * @param <T> The type of data flowing through the pipeline
 */
public interface Flow<T> {
    /**
     * Creates a new Flow from a supplier
     *
     * @param supplier The supplier that initiates the flow
     * @param <T>     The type of data
     * @return A new Flow instance
     */
    static <T> Flow<T> of(Supplier<T> supplier) {
        return FlowImpl.of(supplier);
    }

    /**
     * Creates a new Flow from a value
     *
     * @param value The initial value
     * @param <T>   The type of data
     * @return A new Flow instance
     */
    static <T> Flow<T> just(T value) {
        return of(() -> value);
    }

    /**
     * Gets the context of the currently executing flow.
     * This is useful within flow operations to access context data and metadata.
     *
     * @return The current flow context
     * @throws IllegalStateException if called outside of a flow execution
     */
    static FlowContext currentContext() {
        return FlowImpl.getCurrentContext();
    }

    /**
     * Creates a new Flow from a supplier that may throw a checked exception
     *
     * @param supplier The supplier that may throw a checked exception
     * @param <T>     The type of data
     * @param <E>     The type of the checked exception
     * @return A new Flow instance
     */
    static <T, E extends Exception> Flow<T> ofChecked(CheckedSupplier<T, E> supplier) {
        return of(() -> {
            try {
                return supplier.get();
            } catch (Exception e) {
                throw new FlowExecutionException("Checked exception in supplier", e);
            }
        });
    }

    /**
     * Transforms the data using a function that may throw a checked exception
     *
     * @param mapper The transformation function that may throw a checked exception
     * @param <R>    The type of the transformed data
     * @param <E>    The type of the checked exception
     * @return A new Flow with the transformed type
     */
    default <R, E extends Exception> Flow<R> mapChecked(CheckedFunction<? super T, ? extends R, E> mapper) {
        return map(value -> {
            try {
                return mapper.apply(value);
            } catch (Exception e) {
                throw new FlowExecutionException("Checked exception in mapper", e);
            }
        });
    }

    /**
     * Transforms the data using a function that returns a Flow and may throw a checked exception
     *
     * @param mapper The transformation function that may throw a checked exception
     * @param <R>    The type of the transformed data
     * @param <E>    The type of the checked exception
     * @return A new Flow with the transformed type
     */
    default <R, E extends Exception> Flow<R> flatMapChecked(CheckedFunction<? super T, ? extends Flow<R>, E> mapper) {
        return flatMap(value -> {
            try {
                return mapper.apply(value);
            } catch (Exception e) {
                throw new FlowExecutionException("Checked exception in flatMap", e);
            }
        });
    }

    /**
     * Functional interface for operations that may throw checked exceptions
     *
     * @param <T> The type of the result
     * @param <E> The type of the checked exception
     */
    @FunctionalInterface
    interface CheckedSupplier<T, E extends Exception> {
        T get() throws E;

        /**
         * Wraps a checked supplier into a regular supplier
         *
         * @param supplier The checked supplier to wrap
         * @param <T>     The type of the result
         * @param <E>     The type of the checked exception
         * @return A regular supplier that wraps the checked exception
         */
        static <T, E extends Exception> Supplier<T> wrap(CheckedSupplier<T, E> supplier) {
            return () -> {
                try {
                    return supplier.get();
                } catch (Exception e) {
                    throw new FlowExecutionException("Checked exception in supplier", e);
                }
            };
        }
    }

    /**
     * Functional interface for transformations that may throw checked exceptions
     *
     * @param <T> The input type
     * @param <R> The result type
     * @param <E> The type of the checked exception
     */
    @FunctionalInterface
    interface CheckedFunction<T, R, E extends Exception> {
        R apply(T t) throws E;

        /**
         * Wraps a checked function into a regular function
         *
         * @param function The checked function to wrap
         * @param <T>     The input type
         * @param <R>     The result type
         * @param <E>     The type of the checked exception
         * @return A regular function that wraps the checked exception
         */
        static <T, R, E extends Exception> Function<T, R> wrap(CheckedFunction<T, R, E> function) {
            return t -> {
                try {
                    return function.apply(t);
                } catch (Exception e) {
                    throw new FlowExecutionException("Checked exception in function", e);
                }
            };
        }
    }

    /**
     * Transforms the data in the flow using the provided mapper
     *
     * @param mapper The transformation function
     * @param <R>    The type of the transformed data
     * @return A new Flow with the transformed type
     */
    <R> Flow<R> map(Function<? super T, ? extends R> mapper);

    /**
     * Transforms the data in the flow using a mapper that returns a new Flow
     *
     * @param mapper The transformation function that returns a new Flow
     * @param <R>    The type of the transformed data
     * @return A new Flow with the transformed type
     */
    <R> Flow<R> flatMap(Function<? super T, ? extends Flow<R>> mapper);

    /**
     * Filters the data in the flow using the provided predicate
     *
     * @param predicate The filter condition
     * @return A new Flow with the same type
     */
    Flow<T> filter(Predicate<? super T> predicate);

    /**
     * Executes an action when an error occurs
     *
     * @param errorHandler The error handling function
     * @return The same Flow instance
     */
    Flow<T> onError(Consumer<Throwable> errorHandler);

    /**
     * Executes an action when the flow completes successfully
     *
     * @param successHandler The success handling function
     * @return The same Flow instance
     */
    Flow<T> onComplete(Consumer<? super T> successHandler);

    /**
     * Sets a timeout for the flow execution
     *
     * @param duration The timeout duration
     * @return The same Flow instance
     */
    Flow<T> withTimeout(Duration duration);

    /**
     * Adds retry behavior to the flow
     *
     * @param maxAttempts The maximum number of retry attempts
     * @return The same Flow instance
     */
    Flow<T> withRetry(int maxAttempts);

    /**
     * Adds backoff strategy to retries
     *
     * @param duration The initial backoff duration
     * @return The same Flow instance
     */
    Flow<T> withBackoff(Duration duration);

    /**
     * Executes the flow and returns the result
     *
     * @return The result of the flow execution
     */
    T execute();

    /**
     * Executes the flow asynchronously
     *
     * @return A CompletableFuture containing the result
     */
    CompletableFuture<T> executeAsync();

    /**
     * Enables debug mode for the flow
     *
     * @param enabled Whether debug mode should be enabled
     * @return The same Flow instance
     */
    Flow<T> debugMode(boolean enabled);

    /**
     * Executes a conditional branch based on a predicate
     *
     * @param predicate The condition to evaluate
     * @param action    The action to execute if the condition is true
     * @param <R>      The type of the transformed data
     * @return A new Flow instance
     */
    <R> Flow<R> thenIf(Predicate<? super T> predicate, Function<? super T, ? extends R> action);

    /**
     * Executes an alternative branch when the previous condition was false
     *
     * @param action The action to execute
     * @param <R>    The type of the transformed data
     * @return A new Flow instance
     */
    <R> Flow<R> otherwise(Function<? super T, ? extends R> action);

    /**
     * Creates a parallel flow from multiple suppliers with controlled parallelism
     *
     * @param maxParallelThreads The maximum number of parallel threads to use
     * @param suppliers The suppliers to execute in parallel
     * @param <T>      The type of data
     * @return A new Flow instance with List<T> result
     */
    @SafeVarargs
    static <T> Flow<List<T>> parallel(int maxParallelThreads, Supplier<T>... suppliers) {
        return FlowImpl.parallel(maxParallelThreads, suppliers);
    }

    /**
     * Creates a parallel flow from multiple suppliers
     *
     * @param suppliers The suppliers to execute in parallel
     * @param <T>      The type of data
     * @return A new Flow instance with List<T> result
     */
    @SafeVarargs
    static <T> Flow<List<T>> parallel(Supplier<T>... suppliers) {
        return FlowImpl.parallel(suppliers);
    }

    /**
     * Transforms the data in parallel using the provided mapper
     *
     * @param mapper The transformation function
     * @param <R>    The type of the transformed data
     * @return A new Flow with the transformed type
     */
    <R> Flow<R> parallelMap(Function<? super T, ? extends R> mapper);

    /**
     * Transforms the data in parallel using a mapper that returns a new Flow
     *
     * @param mapper The transformation function that returns a new Flow
     * @param <R>    The type of the transformed data
     * @return A new Flow with the transformed type
     */
    <R> Flow<R> parallelFlatMap(Function<? super T, ? extends Flow<R>> mapper);

    /**
     * Sets the maximum number of parallel threads
     *
     * @param maxThreads The maximum number of parallel threads
     * @return The same Flow instance
     */
    Flow<T> withParallelism(int maxThreads);

    /**
     * Adds a compensation action to be executed in case of failure
     *
     * @param action The compensation action to execute
     * @return The same Flow instance
     */
    Flow<T> withCompensation(Consumer<T> action);

    /**
     * Gets the flow context
     *
     * @return The flow context
     */
    FlowContext getContext();

    /**
     * Stores a value in the flow context
     *
     * @param key   The key to store the value under
     * @param value The value to store
     * @return The same Flow instance
     */
    Flow<T> withContextData(String key, Object value);

    /**
     * Stores metadata in the flow context
     *
     * @param key   The metadata key
     * @param value The metadata value
     * @return The same Flow instance
     */
    Flow<T> withContextMetadata(String key, Object value);

    /**
     * Subscribes to flow events
     *
     * @param listener The event listener
     * @return The same Flow instance
     */
    Flow<T> onEvent(Consumer<FlowEvent> listener);

    /**
     * Subscribes to specific flow event types
     *
     * @param listener The event listener
     * @param types   The event types to subscribe to
     * @return The same Flow instance
     */
    Flow<T> onEventTypes(Consumer<FlowEvent> listener, FlowEvent.EventType... types);

    /**
     * Configures the circuit breaker for this flow
     *
     * @param config The circuit breaker configuration
     * @return The same Flow instance
     */
    Flow<T> withCircuitBreaker(CircuitBreakerConfig config);

    /**
     * Sets a fallback value to be returned when the flow fails
     *
     * @param fallback The supplier providing the fallback value
     * @return The same Flow instance
     */
    Flow<T> withFallback(Supplier<? extends T> fallback);
} 