# Flow DSL

A fluent Java DSL for building robust, resilient, and parallel business workflows.

## Features

### Core Features

- Fluent API for building complex workflows
- Type-safe operations with generic support
- Comprehensive error handling
- Event emission and metrics collection
- Context propagation across operations
- Thread-safe state management

### Parallel & Async Operations

- Efficient parallel execution using virtual threads
- Controlled parallelism with configurable thread pools
- Parallel map operations for list processing
- Async execution with CompletableFuture support
- Thread-safe context propagation in parallel operations
- Automatic resource cleanup and thread management

### Resilience Patterns

- Circuit breaker pattern
- Retry mechanism with backoff support
- Timeout handling
- Fallback operations
- Compensation actions
- Graceful degradation
- Error handling and compensation
- Event emission and monitoring
- Flow composition
- Metrics collection
- Debugging support

### Exception Handling

- Functional interfaces for checked exceptions
- Automatic exception conversion utilities
- Consistent error propagation
- Comprehensive error context
- Parallel execution error handling

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
// Simple sequential flow
String result = Flow.of(() -> "input")
    .map(String::toUpperCase)
    .execute();

// Parallel processing with context
List<String> items = List.of("item1", "item2", "item3");
List<String> results = Flow.just(items)
    .parallelMap(Flow.CheckedFunction.wrap((String item) -> {
        // Access shared context safely in parallel operations
        Integer multiplier = Flow.currentContext()
            .get("multiplier", Integer.class)
            .orElse(1);
        return item.toUpperCase() + "-" + multiplier;
    }))
    .withParallelism(3)
    .withTimeout(Duration.ofSeconds(1))
    .withContextData("multiplier", 42)
    .execute();

// Controlled parallel execution
List<Integer> numbers = IntStream.range(0, 100).boxed().toList();
List<Integer> doubled = Flow.parallel(5, numbers.stream()
    .map(n -> (Supplier<Integer>) () -> n * 2)
    .toArray(Supplier[]::new))
    .execute();

// Async execution with error handling
CompletableFuture<String> future = Flow.ofChecked(() -> {
        Thread.sleep(1000);
        return "async result";
    })
    .withTimeout(Duration.ofSeconds(2))
    .withRetry(3)
    .withFallback(() -> "fallback")
    .executeAsync();
```

## Advanced Features

### Context Propagation

The Flow DSL provides thread-safe context propagation across all operations, including parallel executions:

```java
Flow.of(() -> "input")
    .withContextData("key", "value")
    .parallelMap(item -> {
        // Access context safely in parallel threads
        String contextValue = Flow.currentContext()
            .get("key", String.class)
            .orElseThrow();
        return item + contextValue;
    })
    .execute();
```

### Parallel Execution Control

Fine-grained control over parallel execution:

```java
// Set maximum parallelism
Flow.parallel(maxThreads, suppliers)
    .withTimeout(Duration.ofSeconds(1))
    .execute();

// Parallel map with controlled concurrency
Flow.just(items)
    .parallelMap(mapper)
    .withParallelism(5)
    .execute();
```

### Error Handling in Parallel Operations

Comprehensive error handling for parallel executions:

```java
Flow.parallel(suppliers)
    .withRetry(3)
    .withBackoff(Duration.ofMillis(100))
    .withFallback(() -> fallbackValue)
    .withCompensation(result -> cleanup(result))
    .execute();
```

## Examples

The `examples` module contains comprehensive examples demonstrating:

- Basic Flow DSL usage
- Parallel and async operations
- Error handling and resilience patterns
- Business use cases
- Complex workflows
- Context propagation patterns
- Thread management strategies

## Documentation

Detailed documentation is available in the JavaDoc. Key topics include:

- Building workflows with the Flow DSL
- Parallel execution patterns and best practices
- Context propagation in multi-threaded scenarios
- Error handling strategies
- Resilience patterns
- Performance optimization
- Thread management and resource cleanup

## Requirements

- Java 21 or higher (for virtual threads support)
- SLF4J for logging
- JUnit 5 for testing

## License

This project is licensed under the MIT License - see the LICENSE file for details. 
