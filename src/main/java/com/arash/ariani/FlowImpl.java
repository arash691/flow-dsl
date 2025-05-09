package com.arash.ariani;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Implementation of the Flow interface that provides the core functionality
 * for the Flow DSL.
 *
 * @param <T> The type of data flowing through the pipeline
 */
public class FlowImpl<T> implements Flow<T> {
    private static final Logger log = LoggerFactory.getLogger(FlowImpl.class);
    private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private static final FlowEventEmitter eventEmitter = new DefaultFlowEventEmitter();

    private final Supplier<T> supplier;
    private final FlowContext context;
    private final FlowCompensation<T> compensation;
    private final FlowMetrics metrics;
    private final FlowCircuitBreaker circuitBreaker;
    private Consumer<Throwable> errorHandler = e -> log.error("Flow error", e);
    private Consumer<? super T> successHandler = r -> log.debug("Flow completed with result: {}", r);
    private Duration timeout;
    private int maxRetries;
    private Duration backoffDuration;
    private boolean debugEnabled;
    private int maxParallelThreads = Runtime.getRuntime().availableProcessors();
    private boolean thenIfExecuted = false;

    private Object lastResult;
    private Object predicate;
    private FlowImpl<?> parentFlow;
    private Class<?> resultType;

    private FlowImpl(Supplier<T> supplier) {
        this.supplier = supplier;
        this.context = new FlowContext();
        this.compensation = new FlowCompensation<>();
        this.metrics = new FlowMetrics(UUID.randomUUID().toString());
        this.circuitBreaker = new FlowCircuitBreaker(5, Duration.ofSeconds(60));
    }

    private FlowImpl(Supplier<T> supplier, FlowImpl<?> parent) {
        this(supplier);
        this.parentFlow = parent;
    }

    public static <T> Flow<T> of(Supplier<T> supplier) {
        return new FlowImpl<>(supplier);
    }

    @SafeVarargs
    public static <T> Flow<List<T>> parallel(Supplier<T>... suppliers) {
        return new FlowImpl<>(() -> {
            List<CompletableFuture<T>> futures = new ArrayList<>();
            for (Supplier<T> supplier : suppliers) {
                futures.add(CompletableFuture.supplyAsync(supplier, executor));
            }
            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                return futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
            } catch (Exception e) {
                throw new FlowExecutionException("Parallel execution failed", e);
            }
        });
    }

    @Override
    public <R> Flow<R> map(Function<? super T, ? extends R> mapper) {
        FlowImpl<R> newFlow = new FlowImpl<>(() -> {
            T result = executeWithRetry();
            eventEmitter.emit(new FlowEvent(metrics.getFlowId(), FlowEvent.EventType.STEP_STARTED, "map"));
            try {
                R mappedResult = mapper.apply(result);
                eventEmitter.emit(new FlowEvent(metrics.getFlowId(), FlowEvent.EventType.STEP_COMPLETED, "map"));
                return mappedResult;
            } catch (Exception e) {
                eventEmitter.emit(new FlowEvent(metrics.getFlowId(), FlowEvent.EventType.STEP_ERROR, e));
                throw e;
            }
        });
        newFlow.context.put("parentFlowId", this.metrics.getFlowId());
        return newFlow;
    }

    @Override
    public <R> Flow<R> parallelMap(Function<? super T, ? extends R> mapper) {
        return new FlowImpl<>(() -> {
            T result = executeWithRetry();
            if (result instanceof List<?> list) {
                ExecutorService boundedExecutor = Executors.newFixedThreadPool(
                    Math.min(list.size(), maxParallelThreads),
                    Thread.ofVirtual().factory()
                );
                List<CompletableFuture<R>> futures = new ArrayList<>();
                try {
                    for (Object item : list) {
                        futures.add(CompletableFuture.supplyAsync(
                            () -> mapper.apply((T) item),
                            boundedExecutor));
                    }
                    if (timeout != null) {
                        try {
                            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
                            allFutures.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
                        } catch (TimeoutException e) {
                            futures.forEach(f -> f.cancel(true));
                            throw new FlowTimeoutException("Parallel map timed out after " + timeout.toSeconds() + " seconds", e);
                        } catch (InterruptedException e) {
                            futures.forEach(f -> f.cancel(true));
                            Thread.currentThread().interrupt();
                            throw new FlowExecutionException("Parallel map interrupted", e);
                        } catch (ExecutionException e) {
                            futures.forEach(f -> f.cancel(true));
                            throw new FlowExecutionException("Parallel map failed", e.getCause());
                        }
                    } else {
                        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
                        allFutures.join();
                    }
                    return (R) futures.stream()
                        .map(CompletableFuture::join)
                        .toList();
                } finally {
                    boundedExecutor.shutdown();
                    try {
                        if (!boundedExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                            boundedExecutor.shutdownNow();
                        }
                    } catch (InterruptedException e) {
                        boundedExecutor.shutdownNow();
                        Thread.currentThread().interrupt();
                    }
                }
            }
            return CompletableFuture.supplyAsync(() -> mapper.apply(result), executor).join();
        });
    }

    @Override
    public <R> Flow<R> flatMap(Function<? super T, ? extends Flow<R>> mapper) {
        return new FlowImpl<>(() -> {
            T result = executeWithRetry();
            return mapper.apply(result).execute();
        });
    }

    @Override
    public <R> Flow<R> parallelFlatMap(Function<? super T, ? extends Flow<R>> mapper) {
        return new FlowImpl<>(new Supplier<R>() {
            @Override
            @SuppressWarnings("unchecked")
            public R get() {
                T result = executeWithRetry();
                if (result instanceof List<?> list) {
                    List<CompletableFuture<R>> futures = new ArrayList<>();
                    for (Object item : list) {
                        futures.add(CompletableFuture.supplyAsync(
                            () -> mapper.apply((T) item).execute(),
                            executor));
                    }
                    try {
                        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
                        if (timeout != null) {
                            try {
                                allFutures.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
                            } catch (TimeoutException e) {
                                futures.forEach(f -> f.cancel(true));
                                throw new FlowTimeoutException("Parallel flatMap timed out after " + timeout.toSeconds() + " seconds", e);
                            } catch (InterruptedException e) {
                                futures.forEach(f -> f.cancel(true));
                                Thread.currentThread().interrupt();
                                throw new FlowExecutionException("Parallel flatMap interrupted", e);
                            } catch (ExecutionException e) {
                                futures.forEach(f -> f.cancel(true));
                                throw new FlowExecutionException("Parallel flatMap failed", e.getCause());
                            }
                        } else {
                            allFutures.join();
                        }
                        return (R) futures.stream()
                            .map(CompletableFuture::join)
                            .toList();
                    } catch (Exception e) {
                        if (e instanceof FlowExecutionException) {
                            throw (FlowExecutionException) e;
                        }
                        throw new FlowExecutionException("Parallel flatMap failed", e);
                    }
                }
                return CompletableFuture.supplyAsync(() -> mapper.apply(result).execute(), executor).join();
            }
        });
    }

    @Override
    public Flow<T> filter(Predicate<? super T> predicate) {
        return new FlowImpl<>(() -> {
            T result = executeWithRetry();
            eventEmitter.emit(new FlowEvent(metrics.getFlowId(), FlowEvent.EventType.STEP_STARTED, "filter"));
            try {
                if (predicate.test(result)) {
                    eventEmitter.emit(new FlowEvent(metrics.getFlowId(), FlowEvent.EventType.STEP_COMPLETED, "filter"));
                    return result;
                }
                throw new FlowFilterException("Value filtered out: " + result);
            } catch (Exception e) {
                eventEmitter.emit(new FlowEvent(metrics.getFlowId(), FlowEvent.EventType.STEP_ERROR, e));
                throw e;
            }
        });
    }

    @Override
    public Flow<T> onError(Consumer<Throwable> errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

    @Override
    public Flow<T> onComplete(Consumer<? super T> successHandler) {
        this.successHandler = successHandler;
        return this;
    }

    @Override
    public Flow<T> withTimeout(Duration duration) {
        this.timeout = duration;
        return this;
    }

    @Override
    public Flow<T> withRetry(int maxAttempts) {
        this.maxRetries = maxAttempts;
        return this;
    }

    @Override
    public Flow<T> withBackoff(Duration duration) {
        this.backoffDuration = duration;
        return this;
    }

    @Override
    public Flow<T> withParallelism(int maxThreads) {
        this.maxParallelThreads = maxThreads;
        return this;
    }

    @Override
    public Flow<T> withCompensation(Consumer<T> action) {
        compensation.addCompensation(action);
        return this;
    }

    @Override
    public Flow<T> onEvent(Consumer<FlowEvent> listener) {
        eventEmitter.subscribe(listener);
        return this;
    }

    @Override
    public Flow<T> onEventTypes(Consumer<FlowEvent> listener, FlowEvent.EventType... types) {
        eventEmitter.subscribeToTypes(listener, types);
        return this;
    }

    @Override
    public T execute() {
        Instant start = Instant.now();
        T result = null;
        try {
            eventEmitter.emit(new FlowEvent(metrics.getFlowId(), FlowEvent.EventType.FLOW_STARTED, null));
            
            if (!circuitBreaker.allowExecution()) {
                throw new FlowExecutionException("Circuit breaker is open");
            }

            context.withMetadata("startTime", start)
                   .withMetadata("flowId", metrics.getFlowId());

            result = executeWithRetry();
            successHandler.accept(result);
            circuitBreaker.recordSuccess();
            metrics.recordTiming("execution", Duration.between(start, Instant.now()));

            context.withMetadata("endTime", Instant.now())
                   .withMetadata("success", true);

            eventEmitter.emit(new FlowEvent(metrics.getFlowId(), FlowEvent.EventType.FLOW_COMPLETED, result));
            return result;
        } catch (Exception e) {
            context.withMetadata("error", e)
                   .withMetadata("success", false);
            errorHandler.accept(e);
            circuitBreaker.recordFailure();
            metrics.incrementCounter("errors");
            
            eventEmitter.emit(new FlowEvent(metrics.getFlowId(), FlowEvent.EventType.FLOW_ERROR, e));
            
            // Call compensation with the last known result
            compensation.compensate(result);
            
            if (e instanceof FlowExecutionException flowExecutionException) {
                throw flowExecutionException;
            }
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new FlowExecutionException("Flow execution failed", e);
        }
    }

    @Override
    public CompletableFuture<T> executeAsync() {
        return CompletableFuture.supplyAsync(this::execute, executor)
            .exceptionally(e -> {
                Throwable cause = e.getCause();
                if (cause instanceof FlowTimeoutException) {
                    throw (FlowTimeoutException) cause;
                }
                if (cause instanceof FlowExecutionException) {
                    throw (FlowExecutionException) cause;
                }
                throw new FlowExecutionException("Async flow execution failed", e);
            });
    }

    @Override
    public Flow<T> debugMode(boolean enabled) {
        this.debugEnabled = enabled;
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> Flow<R> thenIf(Predicate<? super T> predicate, Function<? super T, ? extends R> action) {
        this.predicate = predicate;
        this.thenIfExecuted = true;

        return new FlowImpl<R>(() -> {
            T result = executeWithRetry();
            this.lastResult = result;
            this.resultType = result.getClass();
            eventEmitter.emit(new FlowEvent(metrics.getFlowId(), FlowEvent.EventType.STEP_STARTED, "thenIf"));
            try {
                if (predicate.test(result)) {
                    R actionResult = action.apply(result);
                    eventEmitter.emit(new FlowEvent(metrics.getFlowId(), FlowEvent.EventType.STEP_COMPLETED, "thenIf"));
                    return actionResult;
                }
                eventEmitter.emit(new FlowEvent(metrics.getFlowId(), FlowEvent.EventType.STEP_COMPLETED, "thenIf"));
                return null;
            } catch (Exception e) {
                eventEmitter.emit(new FlowEvent(metrics.getFlowId(), FlowEvent.EventType.STEP_ERROR, e));
                throw e;
            }
        }, this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> Flow<R> otherwise(Function<? super T, ? extends R> action) {
        FlowImpl<?> parent = this.parentFlow;
        if (parent == null || !parent.thenIfExecuted || parent.predicate == null) {
            throw new IllegalStateException("otherwise() must be called after thenIf()");
        }

        return new FlowImpl<R>(() -> {
            Object lastResult = parent.lastResult;
            Class<?> expectedType = parent.resultType;

            if (lastResult == null) {
                lastResult = executeWithRetry();
            } else if (!expectedType.isInstance(lastResult)) {
                throw new FlowExecutionException("Type mismatch in flow chain: expected " + expectedType.getSimpleName());
            }

            T result = (T) lastResult;
            Predicate<? super T> predicate = (Predicate<? super T>) parent.predicate;

            eventEmitter.emit(new FlowEvent(metrics.getFlowId(), FlowEvent.EventType.STEP_STARTED, "otherwise"));
            try {
                if (!predicate.test(result)) {
                    R actionResult = action.apply(result);
                    eventEmitter.emit(new FlowEvent(metrics.getFlowId(), FlowEvent.EventType.STEP_COMPLETED, "otherwise"));
                    return actionResult;
                }
                eventEmitter.emit(new FlowEvent(metrics.getFlowId(), FlowEvent.EventType.STEP_COMPLETED, "otherwise"));
                return null;
            } catch (Exception e) {
                eventEmitter.emit(new FlowEvent(metrics.getFlowId(), FlowEvent.EventType.STEP_ERROR, e));
                throw e;
            }
        }, parent);
    }

    private T executeWithRetry() {
        int attempts = 0;
        Exception lastException = null;
        T lastResult = null;

        while (attempts <= maxRetries) {
            try {
                if (debugEnabled) {
                    log.debug("Executing flow attempt {}/{}", attempts + 1, maxRetries + 1);
                }

                if (attempts > 0) {  // Only emit retry event for actual retries
                    eventEmitter.emit(new FlowEvent(metrics.getFlowId(), FlowEvent.EventType.RETRY_ATTEMPT, attempts));
                }

                T result;
                if (timeout != null) {
                    try {
                        result = executeWithTimeout();
                        lastResult = result;
                        return result;
                    } catch (Exception e) {
                        if (e instanceof FlowTimeoutException) {
                            eventEmitter.emit(new FlowEvent(metrics.getFlowId(), FlowEvent.EventType.TIMEOUT_OCCURRED, e));
                            throw e;
                        }
                        throw new FlowExecutionException("Flow execution failed", e);
                    }
                }

                try {
                    result = supplier.get();
                    lastResult = result;
                    return result;
                } catch (FlowFilterException | FlowTimeoutException e) {
                    // Don't retry on these specific exceptions
                    throw e;
                }
            } catch (Exception e) {
                lastException = e;
                attempts++;
                metrics.incrementCounter("retries");

                if (attempts <= maxRetries) {
                    // Call compensation for the failed attempt
                    compensation.compensate(lastResult);
                    
                    if (backoffDuration != null) {
                        try {
                            Thread.sleep(backoffDuration.toMillis() * attempts);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new FlowExecutionException("Flow interrupted during backoff", ie);
                        }
                    }
                } else {
                    if (e instanceof FlowExecutionException) {
                        throw (FlowExecutionException) e;
                    }
                    throw new FlowExecutionException("Flow failed after " + attempts + " attempts", e);
                }
            }
        }

        // This should never be reached due to the else block above
        throw new FlowExecutionException("Flow failed after " + attempts + " attempts", lastException);
    }

    private T executeWithTimeout() {
        CompletableFuture<T> future = CompletableFuture.supplyAsync(supplier, executor);
        try {
            T result = future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (future.isCompletedExceptionally()) {
                throw new FlowExecutionException("Flow execution failed", future.handle((r, e) -> e).join());
            }
            return result;
        } catch (TimeoutException e) {
            future.cancel(true);
            metrics.incrementCounter("timeouts");
            throw new FlowTimeoutException("Flow execution timed out after " + timeout.toSeconds() + " seconds", e);
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new FlowExecutionException("Flow execution interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new FlowExecutionException("Flow execution failed", cause);
        }
    }

    @Override
    public FlowContext getContext() {
        return context;
    }

    @Override
    public Flow<T> withContextData(String key, Object value) {
        context.put(key, value);
        return this;
    }

    @Override
    public Flow<T> withContextMetadata(String key, Object value) {
        context.withMetadata(key, value);
        return this;
    }
} 