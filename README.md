# OpenTelemetry Collector DogStatsD Demo

This demo shows two approaches for sending DogStatsD metrics from Java applications to Honeycomb using OpenTelemetry Collector.

---

## 🎯 Solutions

### Solution 1: Direct to OpenTelemetry Collector

**Port:** 8081  
**Architecture:** `Application → OTel Collector → Honeycomb`

### Solution 2: Dual-Destination (Datadog + Honeycomb)

**Port:** 8082  
**Architecture:** `Application → Datadog Agent + OTel Collector → Honeycomb`

---

## 🚀 Quick Start

### 1. Set API Keys

```bash
export HONEYCOMB_API_KEY=your_honeycomb_key_here
export DATADOG_API_KEY=your_datadog_key_here  # Optional, only for Solution 2
```

### 2. Start Everything

```bash
docker-compose up -d
```

### 3. Test the Applications

```bash
# Solution 1 (OTel Collector only)
curl http://localhost:8081/api/hello
curl http://localhost:8081/api/metrics/simulate

# Solution 2 (Dual-destination)
curl http://localhost:8082/api/hello
curl http://localhost:8082/api/metrics/simulate
```

### 4. Verify in Honeycomb

1. Go to https://ui.honeycomb.io
2. Select your team (e.g., "beowulf")

---

## 🏗️ Architecture

```
Solution 1:
java-app-solution1 (8081) → OTel Collector (8125) → Honeycomb

Solution 2:
java-app-solution2 (8082) → Datadog Agent (8125) → Datadog Backend
                        → OTel Collector (8125) → Honeycomb
```

---

## 📝 Configuration

### Solution 1: OTel Collector Only

**Environment Variables:**

```yaml
environment:
  - STATSD_HOST=otel-collector
  - STATSD_PORT=8125
  - ENVIRONMENT=solution1-otel-only
```

**Config File:** `java-app-solution1/src/main/java/.../config/StatsDConfig.java`

### Solution 2: Dual-Destination

**Environment Variables:**

```yaml
environment:
  - STATSD_HOST_DATADOG=datadog-agent
  - STATSD_PORT_DATADOG=8125
  - STATSD_HOST_OTEL=otel-collector
  - STATSD_PORT_OTEL=8125
  - ENVIRONMENT=solution2-dual
```

**Config File:** `java-app/src/main/java/.../config/DualStatsDConfig.java`

---

## ✅ Verification

### Check Applications

```bash
# Health checks
curl http://localhost:8081/api/health  # Solution 1
curl http://localhost:8082/api/health  # Solution 2
```

### Check OTel Collector

```bash
# Health
curl http://localhost:13133

# Logs
docker logs otel-collector | grep "ResourceMetrics"

# Metrics received
docker logs otel-collector 2>&1 | grep -c "receiver.*statsd"
```

### Check Datadog Agent (Solution 2 only)

```bash
docker exec datadog-agent agent status | grep "DogStatsD"
```

---

## 📊 Expected Metrics

Both solutions generate the same metrics:

- `api.requests` - API request counter
- `api.response.time` - Response time histogram
- `api.errors` - Error counter
- `orders.created` - Order creation counter
- `order.value` - Order value counter
- `active.users` - Active users gauge
- `cache.access` - Cache hit/miss counter
- `request.duration` - Request duration timer

**Tags:**

- `environment` = "solution1-otel-only" or "solution2-dual"
- `service` = "demo-api"
- `application` = "demo-app-solution1" or "demo-app"
- `endpoint`, `product`, `status`, etc.

---

## 📁 Key Files

| File                                       | Purpose                              |
| ------------------------------------------ | ------------------------------------ |
| `docker-compose.yaml`                      | Main setup with both solutions       |
| `otel-collector-config.yaml`               | OTel Collector configuration         |
| `java-app-solution1/.../StatsDConfig.java` | Solution 1 config (OTel only)        |
| `java-app/.../DualStatsDConfig.java`       | Solution 2 config (dual-destination) |

---

## 🔧 How It Works

### Solution 1: Single Registry

- Creates one `StatsdMeterRegistry` pointing to OTel Collector
- All metrics sent directly to OTel Collector

### Solution 2: Composite Registry

- Creates two `StatsdMeterRegistry` instances (Datadog + OTel)
- Combines them into `CompositeMeterRegistry`
- All metrics automatically sent to **both** destinations
- **Zero code changes** in application logic

---

## 📚 Documentation

- **`APPENDIX-H-DOGSTATSD.md`** - Detailed Java implementation guide
- See code comments in config files for inline documentation

---

## 🎯 Key Features

✅ **Zero code changes** - Configuration only  
✅ **Environment variables** - Fully configurable  
✅ **Automatic duplication** - Solution 2 uses CompositeMeterRegistry  
✅ **All DogStatsD types** - Counters, gauges, histograms, timers, sets, distributions  
✅ **Full tag support** - All tags preserved in Honeycomb

---

## ⏱️ Timing

- **Aggregation interval:** 60 seconds (configurable in `otel-collector-config.yaml`)
- **Wait time:** 60-90 seconds after generating metrics before checking Honeycomb
- **To see data faster:** Reduce `aggregation_interval` to 10s for testing

---

## 🐛 Troubleshooting

### Metrics not appearing in Honeycomb?

1. **Check API key:**

   ```bash
   echo $HONEYCOMB_API_KEY
   ```

2. **Check collector logs:**

   ```bash
   docker logs otel-collector | grep -i error
   ```

3. **Verify metrics are being received:**

   ```bash
   docker logs otel-collector 2>&1 | grep -c "receiver.*statsd"
   ```

4. **Wait 60-90 seconds** (aggregation interval)

5. **Check dataset name:** `demo-metrics` in your Honeycomb team

### Applications not responding?

```bash
# Check containers
docker ps | grep java-app

# Check logs
docker logs java-app-solution1
docker logs java-app-solution2
```

---

## 📋 Summary

✅ **Solution 1:** Direct to OTel Collector (port 8081)  
✅ **Solution 2:** Dual-destination (port 8082)  
✅ **Zero code changes** - Configuration only  
✅ **Fully working** - Tested and verified
