package com.example.statsd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * StatsD Demo Application - Solution 1
 * Sends metrics directly to OpenTelemetry Collector only (no Datadog Agent)
 */
@SpringBootApplication
@EnableScheduling
public class StatsdDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(StatsdDemoApplication.class, args);
    }
}
