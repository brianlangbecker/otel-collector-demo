package com.example.statsd.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.statsd.StatsdConfig;
import io.micrometer.statsd.StatsdFlavor;
import io.micrometer.statsd.StatsdMeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * Dual StatsD Configuration
 * Sends metrics to BOTH Datadog Agent and OTel Collector simultaneously
 * Configured via environment variables - NO CODE CHANGES needed in application
 * code!
 */
@Configuration
public class DualStatsDConfig {

    // Read directly from environment variables (set in docker-compose.yaml)
    @Value("${STATSD_HOST_DATADOG:datadog-agent}")
    private String datadogHost;

    @Value("${STATSD_PORT_DATADOG:8125}")
    private int datadogPort;

    @Value("${STATSD_HOST_OTEL:otel-collector}")
    private String otelHost;

    @Value("${STATSD_PORT_OTEL:8125}")
    private int otelPort;

    /**
     * StatsD Registry - sends to Datadog Agent
     */
    @Bean(name = "datadogStatsDRegistry")
    public MeterRegistry datadogStatsDRegistry() {
        StatsdConfig datadogConfig = new StatsdConfig() {
            @Override
            public String get(String key) {
                return null;
            }

            @Override
            public String host() {
                return datadogHost;
            }

            @Override
            public int port() {
                return datadogPort;
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

        return new StatsdMeterRegistry(datadogConfig, io.micrometer.core.instrument.Clock.SYSTEM);
    }

    /**
     * StatsD Registry - sends to OTel Collector
     */
    @Bean(name = "otelStatsDRegistry")
    public MeterRegistry otelStatsDRegistry() {
        StatsdConfig otelConfig = new StatsdConfig() {
            @Override
            public String get(String key) {
                return null;
            }

            @Override
            public String host() {
                return otelHost;
            }

            @Override
            public int port() {
                return otelPort;
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

        return new StatsdMeterRegistry(otelConfig, io.micrometer.core.instrument.Clock.SYSTEM);
    }

    /**
     * Composite Registry - combines both registries
     * All metrics automatically sent to BOTH destinations
     */
    @Bean
    @Primary
    public MeterRegistry compositeMeterRegistry(MeterRegistry datadogStatsDRegistry, MeterRegistry otelStatsDRegistry) {
        CompositeMeterRegistry composite = new CompositeMeterRegistry();
        composite.add(datadogStatsDRegistry);
        composite.add(otelStatsDRegistry);
        return composite;
    }
}
