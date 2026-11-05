# Enterprise Workflow: Pre-Deployment Load & Stress Testing Pipeline

## 1. Overview
Validates system throughput, API latency under concurrency, and database connection pool behavior prior to major feature launches.

## 2. Participating Subagents
- **`load_tester`**: Authors k6 load test scripts and configures virtual user ramp-up profiles.
- **`database_architect`**: Monitors query performance, lock contention, and connection pool saturation.
- **`sre_specialist`**: Observes APM spans, error rates, and CPU/memory exhaustion.

## 3. Step-by-Step Execution Sequence

### Phase 1: Benchmark Definition
1. Define target service endpoints and access scenarios in `load_test.js`.
2. Configure SLA thresholds:
   - `http_req_duration`: p95 < 400ms, p99 < 800ms.
   - `http_req_failed`: error rate < 1.0%.

### Phase 2: Smoke & Ramp-Up Testing
```bash
# Smoke test (5 VUs, 30 seconds)
k6 run --vus 5 --duration 30s load_test.js

# Stress test (Ramp to 200 VUs over 5 minutes)
k6 run --out json=k6-summary.json load_test.js
```

### Phase 3: Telemetry & Bottleneck Analysis
1. Inspect database active connections (`SELECT count(*) FROM pg_stat_activity`).
2. Verify Redis cache hit ratio (> 85%).
3. Inspect slow query logs for queries taking > 100ms under load.

### Phase 4: Release Certification
Certify that the candidate build meets or exceeds production throughput and SLA requirements.
