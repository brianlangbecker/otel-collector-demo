package com.example.statsd.controller;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api")
public class DemoController {

    private static final Logger logger = LoggerFactory.getLogger(DemoController.class);

    private final MeterRegistry meterRegistry;
    private final Counter requestCounter;
    private final Counter errorCounter;
    private final Timer responseTimer;
    private final AtomicInteger activeUsers;
    private final Random random = new Random();

    public DemoController(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Create custom metrics with tags
        this.requestCounter = Counter.builder("api.requests")
                .description("Total API requests")
                .tag("service", "demo-api")
                .register(meterRegistry);

        this.errorCounter = Counter.builder("api.errors")
                .description("Total API errors")
                .tag("service", "demo-api")
                .register(meterRegistry);

        this.responseTimer = Timer.builder("api.response.time")
                .description("API response time")
                .tag("service", "demo-api")
                .register(meterRegistry);

        this.activeUsers = meterRegistry.gauge("active.users", new AtomicInteger(0));
    }

    @GetMapping("/hello")
    public String hello() {
        logger.info("Received hello request");
        requestCounter.increment();

        return responseTimer.record(() -> {
            simulateWork();
            logger.info("Successfully processed hello request");
            return "Hello from StatsD Demo!";
        });
    }

    @PostMapping("/order")
    public OrderResponse createOrder(@RequestBody OrderRequest request) {
        logger.info("Processing order request for product: {}, amount: {}", request.getProduct(), request.getAmount());
        requestCounter.increment();

        return responseTimer.record(() -> {
            simulateWork();

            // Simulate random success/failure
            boolean success = random.nextDouble() > 0.1; // 90% success rate

            if (!success) {
                errorCounter.increment();
                logger.error("Order processing failed for product: {}", request.getProduct());
                throw new RuntimeException("Order processing failed");
            }

            // Track order-specific metrics
            Counter.builder("orders.created")
                    .tag("product", request.getProduct())
                    .tag("status", "success")
                    .register(meterRegistry)
                    .increment();

            meterRegistry.counter("order.value", "currency", "USD")
                    .increment(request.getAmount());

            String orderId = "ORD-" + System.currentTimeMillis();
            logger.info("Successfully created order: {} for product: {}", orderId, request.getProduct());
            return new OrderResponse(orderId, "success");
        });
    }

    @GetMapping("/metrics/simulate")
    public String simulateMetrics() {
        logger.info("Starting metrics simulation");
        // Simulate various metric types
        requestCounter.increment();

        // Simulate active users changing
        int users = random.nextInt(100) + 1;
        activeUsers.set(users);

        // Simulate response times
        Timer.builder("request.duration")
                .tag("endpoint", "/simulate")
                .tag("method", "GET")
                .register(meterRegistry)
                .record(Duration.ofMillis(random.nextInt(500) + 50));

        // Simulate cache hits/misses
        String cacheResult = random.nextBoolean() ? "hit" : "miss";
        Counter.builder("cache.access")
                .tag("result", cacheResult)
                .register(meterRegistry)
                .increment();

        logger.info("Metrics simulation complete: {} active users, cache {}", users, cacheResult);
        return String.format("Simulated metrics: %d active users, cache %s", users, cacheResult);
    }

    @GetMapping("/health")
    public HealthResponse health() {
        logger.debug("Health check requested");
        return new HealthResponse("UP", System.currentTimeMillis());
    }

    private void simulateWork() {
        try {
            Thread.sleep(random.nextInt(100) + 50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Request/Response classes
    public static class OrderRequest {
        private String product;
        private double amount;

        public String getProduct() { return product; }
        public void setProduct(String product) { this.product = product; }
        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }
    }

    public static class OrderResponse {
        private String orderId;
        private String status;

        public OrderResponse(String orderId, String status) {
            this.orderId = orderId;
            this.status = status;
        }

        public String getOrderId() { return orderId; }
        public String getStatus() { return status; }
    }

    public static class HealthResponse {
        private String status;
        private long timestamp;

        public HealthResponse(String status, long timestamp) {
            this.status = status;
            this.timestamp = timestamp;
        }

        public String getStatus() { return status; }
        public long getTimestamp() { return timestamp; }
    }
}
