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
} 