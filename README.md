# StatsD with DogStatsD Support

OpenTelemetry Collector receiving StatsD metrics, OTLP traces, and logs, exporting to Honeycomb.

## Quick Start

```bash
export HONEYCOMB_API_KEY=your_api_key_here
docker-compose up -d
```

The Java app automatically generates telemetry every 10-20 seconds.

View in Honeycomb: `demo-metrics`, `demo-app`, and `demo-logs` datasets

## What's Included

- OpenTelemetry Collector (StatsD on UDP 8125, OTLP on 4317/4318)
- Java Spring Boot demo app with auto-instrumentation and scheduled telemetry generation

## Test Endpoints

```bash
curl http://localhost:8080/api/hello
curl http://localhost:8080/api/metrics/simulate
curl -X POST http://localhost:8080/api/order -H "Content-Type: application/json" -d '{"product":"laptop","amount":999.99}'
```

## Send StatsD Metrics Manually

```bash
echo "requests:1|c" | nc -u -w0 localhost 8125
echo "requests:1|c|#service:api,env:prod" | nc -u -w0 localhost 8125
```
