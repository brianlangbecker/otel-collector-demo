package com.example.statsd.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.statsd.StatsdConfig;
import io.micrometer.statsd.StatsdFlavor;
import io.micrometer.statsd.StatsdMeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * StatsD Configuration - Solution 1: Direct to OpenTelemetry Collector
 * Sends metrics ONLY to OTel Collector (no Datadog Agent)
 * Configured via environment variables - NO CODE CHANGES needed in application code!
 */
@Configuration
public class StatsDConfig {

    // Read directly from environment variables (set in docker-compose.yaml)
    @Value("${STATSD_HOST:otel-collector}")
    private String statsdHost;

    @Value("${STATSD_PORT:8125}")
    private int statsdPort;

    /**
     * StatsD Registry - sends to OpenTelemetry Collector only
     */
    @Bean
    @Primary
    public MeterRegistry meterRegistry() {
        StatsdConfig config = new StatsdConfig() {
            @Override
            public String get(String key) {
                return null;
            }

            @Override
            public String host() {
                return statsdHost;
            }

            @Override
            public int port() {
                return statsdPort;
            }

            @Override
            public StatsdFlavor flavor() {
                return StatsdFlavor.DATADOG;
            }

            @Override
            public Duration step() {
                return Duration.ofSeconds(10);
            }
        };

        return new StatsdMeterRegistry(config, io.micrometer.core.instrument.Clock.SYSTEM);
    }
}

