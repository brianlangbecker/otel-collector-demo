# Appendix H: DogStatsD Support - Java Implementation

Send DogStatsD metrics from Java applications to Honeycomb using OpenTelemetry Collector.

---

## Solution 1: Direct to OpenTelemetry Collector

**Architecture:** `Application → OTel Collector (8125) → Honeycomb`

### OTel Collector Config

```yaml
receivers:
  statsd:
    endpoint: '0.0.0.0:8125'
    aggregation_interval: 60s
    enable_metric_type: true
    timer_histogram_mapping:
      - statsd_type: 'histogram'
      - statsd_type: 'timing'
      - statsd_type: 'distribution'

exporters:
  otlp/metrics:
    endpoint: 'api.honeycomb.io:443'
    headers:
      x-honeycomb-team: '${HONEYCOMB_API_KEY}'
      x-honeycomb-dataset: 'demo-metrics'

service:
  pipelines:
    metrics:
      receivers: [statsd]
      processors: [memory_limiter, batch]
      exporters: [otlp/metrics]
```

### Java Config (`StatsDConfig.java`)

```java
@Configuration
public class StatsDConfig {
    @Value("${STATSD_HOST:otel-collector}")
    private String statsdHost;

    @Value("${STATSD_PORT:8125}")
    private int statsdPort;

    @Bean
    @Primary
    public MeterRegistry meterRegistry() {
        StatsdConfig config = new StatsdConfig() {
            @Override public String get(String key) { return null; }
            @Override public String host() { return statsdHost; }
            @Override public int port() { return statsdPort; }
            @Override public StatsdFlavor flavor() { return StatsdFlavor.DATADOG; }
            @Override public Duration step() { return Duration.ofSeconds(10); }
        };
        return new StatsdMeterRegistry(config, Clock.SYSTEM);
    }
}
```

**Environment Variables:**

```yaml
environment:
  - STATSD_HOST=otel-collector
  - STATSD_PORT=8125
```

---

## Solution 2: Dual-Destination (Datadog + Honeycomb)

**Architecture:** `Application → Datadog Agent (8125) + OTel Collector (8125)`

### Java Config (`DualStatsDConfig.java`)

```java
@Configuration
public class DualStatsDConfig {
    @Value("${STATSD_HOST_DATADOG:datadog-agent}")
    private String datadogHost;
    @Value("${STATSD_PORT_DATADOG:8125}")
    private int datadogPort;
    @Value("${STATSD_HOST_OTEL:otel-collector}")
    private String otelHost;
    @Value("${STATSD_PORT_OTEL:8125}")
    private int otelPort;

    @Bean(name = "datadogStatsDRegistry")
    public MeterRegistry datadogStatsDRegistry() {
        StatsdConfig config = new StatsdConfig() {
            @Override public String get(String key) { return null; }
            @Override public String host() { return datadogHost; }
            @Override public int port() { return datadogPort; }
            @Override public StatsdFlavor flavor() { return StatsdFlavor.DATADOG; }
            @Override public Duration step() { return Duration.ofSeconds(10); }
        };
        return new StatsdMeterRegistry(config, Clock.SYSTEM);
    }

    @Bean(name = "otelStatsDRegistry")
    public MeterRegistry otelStatsDRegistry() {
        StatsdConfig config = new StatsdConfig() {
            @Override public String get(String key) { return null; }
            @Override public String host() { return otelHost; }
            @Override public int port() { return otelPort; }
            @Override public StatsdFlavor flavor() { return StatsdFlavor.DATADOG; }
            @Override public Duration step() { return Duration.ofSeconds(10); }
        };
        return new StatsdMeterRegistry(config, Clock.SYSTEM);
    }

    @Bean
    @Primary
    public MeterRegistry compositeMeterRegistry(
            MeterRegistry datadogStatsDRegistry,
            MeterRegistry otelStatsDRegistry) {
        CompositeMeterRegistry composite = new CompositeMeterRegistry();
        composite.add(datadogStatsDRegistry);
        composite.add(otelStatsDRegistry);
        return composite;
    }
}
```

**Environment Variables:**

```yaml
environment:
  - STATSD_HOST_DATADOG=datadog-agent
  - STATSD_PORT_DATADOG=8125
  - STATSD_HOST_OTEL=otel-collector
  - STATSD_PORT_OTEL=8125
```

---

## Usage

Application code requires **no changes** - inject `MeterRegistry` as usual:

```java
@Service
public class DataGeneratorService {
    private final MeterRegistry meterRegistry;

    public void generateMetrics() {
        Counter.builder("api.requests")
            .tag("service", "demo-api")
            .register(meterRegistry)
            .increment();
    }
}
```

---

## Verification

1. Generate metrics: `curl http://localhost:8081/api/hello`
2. Wait 60-90 seconds (aggregation interval)
3. Check Honeycomb: `WHERE environment = "solution1-otel-only" OR environment = "solution2-dual"`

See `VERIFY-HONEYCOMB.md` for detailed steps.

---

## Supported Metric Types

All DogStatsD types supported: Counter (`c`), Gauge (`g`), Histogram (`h`), Timer (`ms`), Set (`s`), Distribution (`d`).

Tag format: `metric:value|type|#tag1:value1,tag2:value2`
