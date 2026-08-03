# Modera performance observability

`deploy-performance-dashboard.sh` creates a separate Grafana dashboard with UID
`modera-perf`; it does not overwrite the existing `Modera 백엔드` dashboard.

The dashboard is designed for load-test evidence rather than day-to-day status. Every
panel includes an explicit normal/warning/bottleneck criterion. Judge sustained values
over the load stage, not a single spike.

Required environment variables:

```bash
GRAFANA_URL=https://localhost:8443/grafana \
GRAFANA_USER=admin \
GRAFANA_PASSWORD='...' \
bash deploy-performance-dashboard.sh
```

Current observability gaps that must be disclosed in the final presentation:

- The AI service currently exposes no request or stage-duration samples to Prometheus;
  only scrape health is available.
- cAdvisor is not installed, so CPU and memory cannot yet be attributed to every
  individual Docker container.
- postgres_exporter is not installed, so PostgreSQL CPU, locks, cache hit ratio, and
  slow-query evidence must come from database inspection rather than Grafana.
- End-to-end k6 custom metrics are printed by k6 but are not yet sent to Prometheus.

`docker-compose.observability.yml` adds cAdvisor and postgres_exporter. Deploy it as an
overlay on the infrastructure Compose file and replace the Prometheus configuration
with `prometheus.yml`. This adds container-level CPU/memory/network and PostgreSQL
activity/locks/cache metrics without exposing either exporter to the public network.

The dashboard still distinguishes API, HikariCP, JVM/GC, Redis queue, host resources,
and error logs. This is sufficient to identify the first saturated Spring-side
resource, while the gaps above identify where a conclusion must remain provisional.

After deploying the worker code in this repository, these additional metrics become
available:

- `modera_analysis_pipeline_duration_seconds`: queue-to-persist end-to-end analysis time.
- `modera_analysis_ai_duration_seconds`: AI-dispatch-to-callback time.
- `modera_analysis_results_total{status}`: COMPLETED, EMPTY, FAILED result quality split.
- `modera_analysis_callback_persistence_failures_total`: worker callback persistence errors.
