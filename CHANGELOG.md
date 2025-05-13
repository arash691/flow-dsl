# Changelog

## [1.1.0] - 2024-03-XX

### Added
- Enhanced parallel execution support:
  - Improved type safety in parallel operations
  - Added `parallelMap` for efficient list processing
  - Better handling of checked exceptions in parallel operations
  - Support for controlled parallelism with `withParallelism(n)`
  - Thread-safe context propagation in parallel operations
  - Automatic resource cleanup in parallel executions
  - New `parallel(maxThreads, suppliers)` method for direct control
  - Virtual thread support for optimal performance

### Changed
- Improved exception handling:
  - Added `CheckedSupplier` and `CheckedFunction` interfaces for better exception handling
  - New `wrap()` utility methods for converting checked to unchecked operations
  - Consistent error propagation in parallel operations
  - Enhanced context propagation in error scenarios
  - Better error reporting in parallel executions

### Fixed
- Thread safety improvements in parallel operations
- Fixed type inference issues in parallel map operations
- Corrected context handling in parallel executions
- Proper cleanup of thread pools in parallel operations
- Fixed context propagation in virtual threads
- Improved error handling in parallel timeouts
- Better resource management in parallel operations

### Examples
- Reorganized example modules for better clarity:
  - Basic Flow DSL features
  - Resilience patterns (retry, circuit breaker, timeout)
  - Async and parallel operations
  - Business use cases
- Added comprehensive examples demonstrating:
  - Parallel execution of multiple operations
  - Parallel map with controlled concurrency
  - Async execution with non-blocking operations
  - Complex workflows combining parallel and async operations
  - Error handling and resilience patterns
  - Context propagation in parallel operations
  - Thread management and cleanup patterns

### Technical Improvements
- Added ThreadLocal-based context management
- Improved thread pool management in parallel operations
- Better timeout handling in parallel executions
- Enhanced type inference in generic operations
- Optimized virtual thread usage
- Improved resource cleanup mechanisms
- Enhanced parallel execution monitoring

### Documentation
- Added detailed documentation for parallel operations
- Improved JavaDoc for all major interfaces
- Added usage examples for common patterns
- Better error handling documentation
- New sections on:
  - Context propagation in parallel operations
  - Thread management best practices
  - Performance optimization guidelines
  - Resource cleanup strategies

## [1.0.0-alpha] - 2024-03-XX

### Added
- Enhanced parallel execution support:
  - Improved type safety in parallel operations
  - Added `parallelMap` for efficient list processing
  - Better handling of checked exceptions in parallel operations
  - Support for controlled parallelism with `withParallelism(n)`
  - Thread-safe context propagation in parallel operations
  - Automatic resource cleanup in parallel executions
  - New `parallel(maxThreads, suppliers)` method for direct control
  - Virtual thread support for optimal performance

### Changed
- Improved exception handling:
  - Added `CheckedSupplier` and `CheckedFunction` interfaces for better exception handling
  - New `wrap()` utility methods for converting checked to unchecked operations
  - Consistent error propagation in parallel operations
  - Enhanced context propagation in error scenarios
  - Better error reporting in parallel executions

### Fixed
- Thread safety improvements in parallel operations
- Fixed type inference issues in parallel map operations
- Corrected context handling in parallel executions
- Proper cleanup of thread pools in parallel operations
- Fixed context propagation in virtual threads
- Improved error handling in parallel timeouts
- Better resource management in parallel operations

### Examples
- Reorganized example modules for better clarity:
  - Basic Flow DSL features
  - Resilience patterns (retry, circuit breaker, timeout)
  - Async and parallel operations
  - Business use cases
- Added comprehensive examples demonstrating:
  - Parallel execution of multiple operations
  - Parallel map with controlled concurrency
  - Async execution with non-blocking operations
  - Complex workflows combining parallel and async operations
  - Error handling and resilience patterns
  - Context propagation in parallel operations
  - Thread management and cleanup patterns

### Technical Improvements
- Added ThreadLocal-based context management
- Improved thread pool management in parallel operations
- Better timeout handling in parallel executions
- Enhanced type inference in generic operations
- Optimized virtual thread usage
- Improved resource cleanup mechanisms
- Enhanced parallel execution monitoring

### Documentation
- Added detailed documentation for parallel operations
- Improved JavaDoc for all major interfaces
- Added usage examples for common patterns
- Better error handling documentation
- New sections on:
  - Context propagation in parallel operations
  - Thread management best practices
  - Performance optimization guidelines
  - Resource cleanup strategies

## [0.1.0] - Initial Release

- Core Flow DSL implementation
- Basic operations (map, flatMap, filter)
- Error handling
- Async execution support
- Circuit breaker implementation
- Retry mechanism
- Timeout support
- Context propagation
- Event emission system
- Metrics collection 