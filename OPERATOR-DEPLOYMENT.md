# OpenTelemetry Operator Deployment

> ⚠️ **EXPERIMENTAL** - This configuration has not been tested with the OpenTelemetry Operator in production environments.

## Overview

This guide explains how to deploy the collector configuration to Kubernetes using the OpenTelemetry Operator.

## Prerequisites

- Kubernetes cluster
- kubectl configured
- OpenTelemetry Operator installed
- Honeycomb API key

## Install OpenTelemetry Operator

```bash
kubectl apply -f https://github.com/open-telemetry/opentelemetry-operator/releases/latest/download/opentelemetry-operator.yaml
```

## Create Honeycomb Secret

```bash
kubectl create secret generic honeycomb-secret \
  --from-literal=api-key=YOUR_HONEYCOMB_API_KEY \
  -n opentelemetry
```

## Deploy OpenTelemetryCollector

```yaml
apiVersion: opentelemetry.io/v1alpha1
kind: OpenTelemetryCollector
metadata:
  name: otel-collector
  namespace: opentelemetry
spec:
  mode: deployment
  config: |
    receivers:
      # StatsD receiver for metrics
      statsd:
        endpoint: "0.0.0.0:8125"
        aggregation_interval: 60s
        enable_metric_type: true
        is_monotonic_counter: false
        timer_histogram_mapping:
          - statsd_type: "histogram"
            observer_type: "histogram"
          - statsd_type: "timing"
            observer_type: "histogram"

      # OTLP receiver for traces and logs
      otlp:
        protocols:
          grpc:
            endpoint: "0.0.0.0:4317"
          http:
            endpoint: "0.0.0.0:4318"

    processors:
      batch:
        timeout: 10s
        send_batch_size: 1024

      memory_limiter:
        check_interval: 1s
        limit_mib: 512

      resource/metrics:
        attributes:
          - key: service.name
            value: k8s-collector
            action: upsert
          - key: deployment.environment
            value: kubernetes
            action: upsert

    exporters:
      otlp/metrics:
        endpoint: "api.honeycomb.io:443"
        headers:
          x-honeycomb-team: "${HONEYCOMB_API_KEY}"
          x-honeycomb-dataset: "demo-metrics"

      otlp/traces:
        endpoint: "api.honeycomb.io:443"
        headers:
          x-honeycomb-team: "${HONEYCOMB_API_KEY}"

      otlp/logs:
        endpoint: "api.honeycomb.io:443"
        headers:
          x-honeycomb-team: "${HONEYCOMB_API_KEY}"
          x-honeycomb-dataset: "demo-logs"

      debug:
        verbosity: detailed
        sampling_initial: 5
        sampling_thereafter: 200

    service:
      pipelines:
        metrics:
          receivers: [statsd, otlp]
          processors: [memory_limiter, batch, resource/metrics]
          exporters: [otlp/metrics, debug]

        traces:
          receivers: [otlp]
          processors: [memory_limiter, batch]
          exporters: [otlp/traces, debug]

        logs:
          receivers: [otlp]
          processors: [memory_limiter, batch]
          exporters: [otlp/logs, debug]

      telemetry:
        logs:
          level: info
  env:
    - name: HONEYCOMB_API_KEY
      valueFrom:
        secretKeyRef:
          name: honeycomb-secret
          key: api-key
  ports:
    - name: statsd-udp
      port: 8125
      protocol: UDP
    - name: otlp-grpc
      port: 4317
      protocol: TCP
    - name: otlp-http
      port: 4318
      protocol: TCP
    - name: metrics
      port: 8888
      protocol: TCP
```

Save this as `otel-collector.yaml` and apply:

```bash
kubectl apply -f otel-collector.yaml
```

## Expose Collector Service

```bash
kubectl expose deployment otel-collector-collector \
  --port=4317 \
  --target-port=4317 \
  --name=otel-collector-grpc \
  -n opentelemetry

kubectl expose deployment otel-collector-collector \
  --port=4318 \
  --target-port=4318 \
  --name=otel-collector-http \
  -n opentelemetry

kubectl expose deployment otel-collector-collector \
  --port=8125 \
  --target-port=8125 \
  --protocol=UDP \
  --name=otel-collector-statsd \
  -n opentelemetry
```

## Configure Applications

Point your applications to the collector service:

**OTLP gRPC:**
```yaml
env:
  - name: OTEL_EXPORTER_OTLP_ENDPOINT
    value: "http://otel-collector-grpc.opentelemetry.svc.cluster.local:4317"
```

**OTLP HTTP:**
```yaml
env:
  - name: OTEL_EXPORTER_OTLP_ENDPOINT
    value: "http://otel-collector-http.opentelemetry.svc.cluster.local:4318"
```

**StatsD metrics:**
```
otel-collector-statsd.opentelemetry.svc.cluster.local:8125
```

## Verify Deployment

```bash
kubectl get pods -n opentelemetry
kubectl logs -n opentelemetry -l app.kubernetes.io/name=otel-collector-collector
```

## Known Issues

- StatsD UDP service may need NodePort or LoadBalancer for external access
- Resource limits may need adjustment based on traffic volume
- Debug exporter verbosity should be reduced in production

## Testing

Deploy a test pod to send telemetry:

```bash
kubectl run test-pod --image=curlimages/curl -it --rm -- sh
```

Inside the pod:
```bash
# Test OTLP endpoint
curl -v http://otel-collector-grpc.opentelemetry.svc.cluster.local:4317

# Send StatsD metric
echo "test.metric:1|c" | nc -u otel-collector-statsd.opentelemetry.svc.cluster.local 8125
```
