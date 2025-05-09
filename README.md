# Flow DSL

A lightweight, expressive Flow DSL for Java that simplifies business logic implementation with a fluent API. Built with Java 21 features, this DSL provides a clean and efficient way to compose business operations with built-in error handling, retries, timeouts, and more.

## Features

- 🔄 Fluent API for flow definition
- 🌳 Conditional logic and branching
- ⚡ Parallel execution support
- 🔁 Retry and circuit breaker integration
- ⏰ Timeout handling
- 🔍 Context propagation and metadata
- ❌ Error handling and compensation actions
- 📡 Event emission and listeners
- 🧩 Flow composition and reusability
- 🔄 Observable and reactive support
- 📊 Metrics and telemetry integration
- 🔍 Conditional mapping and filtering
- 🐛 Flow debugging and visualization
- 📦 Step result handling and aggregation
- 🚨 Async error aggregation and completion handlers

## Requirements

- Java 21 or higher
- Maven 3.6 or higher

## Installation

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.arash.ariani</groupId>
    <artifactId>flow-dsl</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## Usage Examples

### Basic Flow

```java
String result = Flow.of(() -> "Hello")
    .map(str -> str + " World")
    .execute();
```

### Error Handling

```java
Flow.of(() -> fetchOrder(orderId))
    .onError(e -> log.error("Flow error", e))
    .execute();
```

### Retry with Backoff

```java
Flow.of(() -> callExternalService())
    .withRetry(3)
    .withBackoff(Duration.ofSeconds(1))
    .execute();
```

### Conditional Branching

```java
Flow.of(() -> fetchUser(userId))
    .thenIf(user -> user.isActive(), 
            user -> activateAccount(user))
    .otherwise(user -> deactivateAccount(user))
    .execute();
```

### Timeout Handling

```java
Flow.of(() -> fetchData())
    .withTimeout(Duration.ofSeconds(5))
    .execute();
```

### Async Execution

```java
CompletableFuture<String> future = Flow.of(() -> "Async")
    .map(str -> str + " Test")
    .executeAsync();
```

### Complex Flow Example

```java
record Order(int id, double amount) {}
record ProcessedOrder(int id, double amount, String status) {}

ProcessedOrder result = Flow.of(() -> new Order(1, 100.0))
    .filter(order -> order.amount() > 0)
    .map(order -> new ProcessedOrder(order.id(), order.amount(), "PROCESSED"))
    .thenIf(
        order -> order.amount() > 50,
        order -> new ProcessedOrder(order.id(), order.amount() * 0.9, "DISCOUNTED")
    )
    .withRetry(2)
    .withTimeout(Duration.ofSeconds(1))
    .execute();
```

## Key Benefits

1. **Readability**: The fluent API makes business logic easy to read and understand.
2. **Maintainability**: Separation of concerns and modular design simplifies maintenance.
3. **Reliability**: Built-in error handling, retries, and timeouts improve reliability.
4. **Flexibility**: Easy to extend and customize for specific business needs.
5. **Performance**: Efficient execution with virtual threads support.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details. 