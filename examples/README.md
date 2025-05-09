# Flow DSL Examples

This module contains practical examples demonstrating how to use the Flow DSL in various scenarios. The examples are organized into different categories to help you understand and learn the various features and patterns available in the Flow DSL.

## Categories

### 1. Basic Examples (`BasicFlowExamples.java`)
- Simple transformations
- Filtering
- Conditional flows
- Flow composition
- Context sharing
- List processing

### 2. Error Handling Examples (`ErrorHandlingExamples.java`)
- Basic error handling
- Retry patterns
- Timeout handling
- Compensation actions
- Circuit breaker pattern
- Combined resilience patterns

### 3. Async and Parallel Examples (`AsyncParallelExamples.java`)
- Basic async execution
- Parallel execution
- Parallel map operations
- Parallel flatMap operations
- Async composition
- Complex parallel workflows

### 4. Business Process Example (`OrderProcessingExample.java`)
A comprehensive example demonstrating a real-world order processing workflow that includes:
- Order validation
- Payment processing
- Inventory checking
- Shipping label generation
- Parallel order processing
- Error handling and compensation
- Event monitoring

## Running the Examples

Each example class has a `main` method that you can run directly. Here's how to run them:

### Using Maven
```bash
# Run all examples
mvn exec:java -Dexec.mainClass="com.arash.ariani.examples.basic.BasicFlowExamples"
mvn exec:java -Dexec.mainClass="com.arash.ariani.examples.error.ErrorHandlingExamples"
mvn exec:java -Dexec.mainClass="com.arash.ariani.examples.async.AsyncParallelExamples"
mvn exec:java -Dexec.mainClass="com.arash.ariani.examples.business.OrderProcessingExample"
```

### Using Your IDE
Simply run the `main` method in any of the example classes:
- `BasicFlowExamples`
- `ErrorHandlingExamples`
- `AsyncParallelExamples`
- `OrderProcessingExample`

## Key Concepts Demonstrated

1. **Flow Creation and Composition**
   - Creating flows using `Flow.of()` and `Flow.just()`
   - Composing flows using `flatMap`
   - Conditional branching with `thenIf` and `otherwise`

2. **Error Handling and Resilience**
   - Error handlers with `onError`
   - Retry mechanisms with `withRetry` and `withBackoff`
   - Circuit breaker pattern
   - Compensation actions with `withCompensation`
   - Timeout handling with `withTimeout`

3. **Asynchronous and Parallel Processing**
   - Async execution with `executeAsync`
   - Parallel execution with `Flow.parallel`
   - Parallel operations with `parallelMap` and `parallelFlatMap`
   - Controlling parallelism with `withParallelism`

4. **Context and Event Handling**
   - Sharing context between flows
   - Event monitoring with `onEvent`
   - Success handling with `onComplete`

5. **Business Process Patterns**
   - Workflow composition
   - Transaction management
   - Compensation handling
   - Parallel processing
   - Error recovery

## Best Practices Demonstrated

1. **Error Handling**
   - Always handle errors appropriately using `onError`
   - Implement compensation actions for rollback scenarios
   - Use timeouts for external service calls

2. **Performance**
   - Use parallel processing for independent operations
   - Control parallelism levels based on resource availability
   - Implement proper timeout handling

3. **Monitoring**
   - Add event handlers for monitoring flow execution
   - Include proper logging
   - Track metrics where appropriate

4. **Code Organization**
   - Break down complex flows into smaller, reusable components
   - Use proper naming conventions
   - Include comprehensive documentation

## Contributing

Feel free to contribute additional examples by submitting pull requests. When adding new examples:
1. Create a new package for your example category if needed
2. Include comprehensive documentation
3. Add unit tests
4. Update this README with your new example

## License

This project is licensed under the MIT License - see the LICENSE file for details. 