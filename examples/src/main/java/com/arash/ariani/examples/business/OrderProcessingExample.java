package com.arash.ariani.examples.business;

import com.arash.ariani.Flow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Example demonstrating a practical order processing workflow using Flow DSL.
 */
public class OrderProcessingExample {
    private static final Logger log = LoggerFactory.getLogger(OrderProcessingExample.class);

    // Domain models
    record Order(String orderId, String customerId, List<OrderItem> items, BigDecimal total) {}
    record OrderItem(String productId, int quantity, BigDecimal price) {}
    record PaymentResult(String transactionId, boolean success, String message) {}
    record InventoryResult(String productId, boolean available, int remainingStock) {}
    record ShippingLabel(String trackingNumber, String address) {}
    record ProcessedOrder(Order order, PaymentResult payment, List<InventoryResult> inventory, ShippingLabel shipping) {}

    // Service interfaces (simulated)
    static class OrderService {
        public static Order validateOrder(Order order) {
            log.info("Validating order {}", order.orderId);
            if (order.items.isEmpty()) {
                throw new IllegalArgumentException("Order must have at least one item");
            }
            return order;
        }
    }

    static class PaymentService {
        public static PaymentResult processPayment(Order order) {
            log.info("Processing payment for order {}", order.orderId);
            return new PaymentResult(
                UUID.randomUUID().toString(),
                true,
                "Payment successful"
            );
        }

        public static void rollbackPayment(PaymentResult payment) {
            log.info("Rolling back payment {}", payment.transactionId);
        }
    }

    static class InventoryService {
        public static InventoryResult checkInventory(OrderItem item) {
            log.info("Checking inventory for product {}", item.productId);
            return new InventoryResult(
                item.productId,
                true,
                100 - item.quantity
            );
        }

        public static void rollbackInventory(InventoryResult result) {
            log.info("Rolling back inventory for product {}", result.productId);
        }
    }

    static class ShippingService {
        public static ShippingLabel generateShippingLabel(Order order) {
            log.info("Generating shipping label for order {}", order.orderId);
            return new ShippingLabel(
                "TRACK-" + UUID.randomUUID().toString().substring(0, 8),
                "123 Customer Street"
            );
        }
    }

    /**
     * Process a single order through the entire workflow
     */
    public static ProcessedOrder processOrder(Order order) {
        return Flow.of(() -> order)
            // Validate order
            .map(OrderService::validateOrder)
            
            // Process payment with retry and timeout
            .flatMap(validOrder -> Flow.of(() -> PaymentService.processPayment(validOrder))
                .withRetry(3)
                .withBackoff(Duration.ofMillis(100))
                .withTimeout(Duration.ofSeconds(5))
                .withCompensation(PaymentService::rollbackPayment))
            
            // Check inventory in parallel for all items
            .flatMap(payment -> Flow.of(() -> order.items)
                .parallelMap(item -> InventoryService.checkInventory(item))
                .withParallelism(order.items.size())
                .withCompensation(results -> results.forEach(InventoryService::rollbackInventory)))
            
            // Generate shipping label
            .flatMap(inventoryResults -> Flow.of(() -> ShippingService.generateShippingLabel(order))
                .map(shippingLabel -> new ProcessedOrder(order, null, inventoryResults, shippingLabel)))
            
            // Add event handling and logging
            .onEvent(event -> log.debug("Order processing event: {}", event))
            .onError(error -> log.error("Order processing error: {}", error.getMessage()))
            .onComplete(result -> log.info("Order {} processed successfully", order.orderId))
            .execute();
    }

    /**
     * Process multiple orders in parallel
     */
    public static List<ProcessedOrder> processOrders(List<Order> orders) {
        return Flow.of(() -> orders)
            .parallelMap(OrderProcessingExample::processOrder)
            .withParallelism(10)
            .withTimeout(Duration.ofMinutes(5))
            .execute();
    }

    public static void main(String[] args) {
        // Create a sample order
        Order order = new Order(
            UUID.randomUUID().toString(),
            "CUST-001",
            List.of(
                new OrderItem("PROD-1", 2, new BigDecimal("29.99")),
                new OrderItem("PROD-2", 1, new BigDecimal("49.99"))
            ),
            new BigDecimal("109.97")
        );

        log.info("Processing single order...");
        ProcessedOrder result = processOrder(order);
        log.info("Order processed with tracking number: {}", result.shipping.trackingNumber);

        // Process multiple orders in parallel
        List<Order> orders = List.of(
            order,
            new Order(
                UUID.randomUUID().toString(),
                "CUST-002",
                List.of(new OrderItem("PROD-3", 1, new BigDecimal("19.99"))),
                new BigDecimal("19.99")
            )
        );

        log.info("\nProcessing multiple orders in parallel...");
        List<ProcessedOrder> results = processOrders(orders);
        log.info("Processed {} orders", results.size());
    }
} 