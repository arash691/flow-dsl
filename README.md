# Flow DSL

A lightweight, fluent Domain-Specific Language (DSL) for building type-safe, composable business flows in Java.

## Features

- Fluent API for building flows
- Conditional branching with `thenIf` and `otherwise`
- Parallel execution support
- Retry and circuit breaker patterns
- Timeout handling
- Context propagation
- Error handling and compensation
- Event emission and monitoring
- Flow composition
- Reactive support with CompletableFuture
- Metrics collection
- Debugging support

## Getting Started

Add the dependency to your project:

```xml
<dependency>
    <groupId>com.arash.ariani</groupId>
    <artifactId>flow-dsl</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## Examples

### Basic Flow

```java
Flow<String> flow = Flow.of(() -> "Hello")
    .map(str -> str + " World")
    .execute();  // Returns "Hello World"
```

### Error Handling

```java
Flow<String> flow = Flow.of(() -> "data")
    .onError(error -> log.error("Flow failed", error))
    .onComplete(result -> log.info("Flow completed with: {}", result))
    .withRetry(3)
    .withBackoff(Duration.ofSeconds(1))
    .execute();
```

### Conditional Logic

```java
Flow<Integer> flow = Flow.of(() -> 42)
    .thenIf(
        num -> num > 40,
        num -> "Large number: " + num
    )
    .otherwise(
        num -> "Small number: " + num
    )
    .execute();
```

### Parallel Execution

```java
Flow<List<String>> flow = Flow.parallel(
    () -> "Task 1",
    () -> "Task 2",
    () -> "Task 3"
).withParallelism(3)
 .execute();
```

### Context and Metadata

```java
Flow<String> flow = Flow.of(() -> "data")
    .withContextData("key", "value")
    .withContextMetadata("startTime", Instant.now())
    .map(data -> {
        String contextValue = getContext().get("key", String.class)
            .orElse("default");
        return data + contextValue;
    })
    .execute();
```

### Timeout Handling

```java
Flow<String> flow = Flow.of(() -> {
    Thread.sleep(2000);
    return "Delayed result";
})
.withTimeout(Duration.ofSeconds(1))
.onError(e -> {
    if (e instanceof FlowTimeoutException) {
        log.error("Flow timed out");
    }
})
.execute();
```

### Circuit Breaker

```java
Flow<String> flow = Flow.of(() -> callExternalService())
    .withRetry(3)
    .withBackoff(Duration.ofSeconds(1))
    .onError(e -> circuitBreaker.recordFailure())
    .onComplete(r -> circuitBreaker.recordSuccess())
    .execute();
```

### Event Handling

```java
Flow<String> flow = Flow.of(() -> "data")
    .onEvent(event -> {
        switch (event.getType()) {
            case FLOW_STARTED -> log.info("Flow started");
            case FLOW_COMPLETED -> log.info("Flow completed");
            case FLOW_ERROR -> log.error("Flow error: {}", event.getPayload());
        }
    })
    .execute();
```

### Async Execution

```java
CompletableFuture<String> future = Flow.of(() -> "async data")
    .map(data -> processDataAsync(data))
    .executeAsync();

future.thenAccept(result -> log.info("Got result: {}", result));
```

### Flow Composition

```java
Flow<String> flow1 = Flow.of(() -> "Hello");
Flow<String> flow2 = Flow.of(() -> "World");

Flow<String> combined = flow1.flatMap(hello ->
    flow2.map(world -> hello + " " + world)
);

String result = combined.execute(); // "Hello World"
```

### Metrics Collection

```java
Flow<String> flow = Flow.of(() -> "data")
    .map(data -> {
        metrics.recordTiming("processing", Duration.ofMillis(100));
        metrics.incrementCounter("processed");
        return processData(data);
    })
    .execute();
```

## Best Practices

1. Always handle errors appropriately using `onError`
2. Use timeouts for external service calls
3. Implement compensation actions for rollback scenarios
4. Monitor flow execution with events and metrics
5. Use context for passing metadata between flow steps
6. Configure appropriate retry and backoff for resilience
7. Leverage parallel execution for independent operations

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details. 