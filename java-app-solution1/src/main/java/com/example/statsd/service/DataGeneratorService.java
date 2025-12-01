package com.example.statsd.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DataGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(DataGeneratorService.class);
    private final MeterRegistry meterRegistry;
    private final Random random = new Random();
    private final AtomicInteger activeUsers;

    public DataGeneratorService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.activeUsers = meterRegistry.gauge("active.users", new AtomicInteger(0));
        logger.info("DataGeneratorService initialized - will generate telemetry every 10 seconds");
    }

    @Scheduled(fixedRate = 10000)
    public void generateMetrics() {
        logger.info("Generating periodic metrics and logs");

        // Simulate API requests
        int requestCount = random.nextInt(10) + 1;
        Counter requestCounter = Counter.builder("api.requests")
                .tag("service", "demo-api")
                .tag("endpoint", "/periodic")
                .register(meterRegistry);
        requestCounter.increment(requestCount);

        // Simulate response times
        long responseTime = random.nextInt(300) + 50;
        Timer.builder("api.response.time")
                .tag("service", "demo-api")
                .tag("endpoint", "/periodic")
                .register(meterRegistry)
                .record(Duration.ofMillis(responseTime));

        // Simulate active users
        int users = random.nextInt(100) + 10;
        activeUsers.set(users);

        // Simulate cache operations
        String cacheResult = random.nextBoolean() ? "hit" : "miss";
        Counter.builder("cache.access")
                .tag("result", cacheResult)
                .tag("cache", "redis")
                .register(meterRegistry)
                .increment();

        logger.info("Generated metrics: {} requests, {}ms response time, {} users, cache {}",
                    requestCount, responseTime, users, cacheResult);
    }

    @Scheduled(fixedRate = 15000)
    public void generateOrders() {
        String[] products = {"laptop", "phone", "tablet", "monitor", "keyboard"};
        String product = products[random.nextInt(products.length)];
        double amount = random.nextInt(900) + 100;

        logger.info("Simulating order creation for product: {}, amount: ${}", product, amount);

        // Simulate order creation with 90% success rate
        boolean success = random.nextDouble() > 0.1;

        if (success) {
            Counter.builder("orders.created")
                    .tag("product", product)
                    .tag("status", "success")
                    .register(meterRegistry)
                    .increment();

            meterRegistry.counter("order.value", "currency", "USD")
                    .increment(amount);

            logger.info("Order created successfully for product: {}, amount: ${}", product, amount);
        } else {
            Counter.builder("orders.created")
                    .tag("product", product)
                    .tag("status", "failed")
                    .register(meterRegistry)
                    .increment();

            logger.error("Order creation failed for product: {}", product);
        }
    }

    @Scheduled(fixedRate = 20000)
    public void generateErrors() {
        // Occasionally generate error logs
        if (random.nextDouble() < 0.3) {
            String[] errors = {"Database connection timeout", "External API rate limit exceeded",
                             "Invalid request payload", "Authentication token expired"};
            String error = errors[random.nextInt(errors.length)];

            logger.error("Simulated error occurred: {}", error);

            Counter.builder("api.errors")
                    .tag("service", "demo-api")
                    .tag("type", "simulated")
                    .register(meterRegistry)
                    .increment();
        }
    }
}
