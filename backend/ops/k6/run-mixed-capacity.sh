#!/usr/bin/env bash
set -euo pipefail

levels=(${LEVELS:-40 35 30 25 20})
result_dir="${RESULT_DIR:-/home/ubuntu/k6/results/mixed-5m-200}"
script_dir="${SCRIPT_DIR:-/home/ubuntu/k6}"
base_url="${BASE_URL:-http://modera-api-old:8080}"
mkdir -p "$result_dir"
summary="$result_dir/capacity.tsv"
printf 'vus\texit\tsteps_min\tflow_success\tbusiness_p95\tlogin_p95\tfail_rate\thttp_reqs\trps\n' > "$summary"

for vus in "${levels[@]}"; do
  echo "[mixed-5m] testing ${vus} concurrent users, 200 calls each over 300 seconds"
  set +e
  docker run --rm --user 0:0 --network infra_default \
    -v "$script_dir:/scripts:ro" -v "$result_dir:/results" \
    grafana/k6 run --quiet \
    -e BASE_URL="$base_url" -e VUS="$vus" -e CALLS_PER_SESSION=200 \
    -e SESSION_SECONDS=300 -e STARTUP_SPREAD_SECONDS=10 -e THINK_TIME=0 \
    -e LIVENESS_DURATION=5m20s -e MAX_DURATION=7m \
    --summary-export="/results/vus-${vus}.json" /scripts/mixed-user-scenarios.js \
    > "$result_dir/vus-${vus}.log" 2>&1
  exit_code=$?
  set -e
  json="$result_dir/vus-${vus}.json"
  steps_min=$(jq -r '.metrics.mixed_session_steps.min // "na"' "$json")
  flow_success=$(jq -r '.metrics.mixed_flow_success.value // "na"' "$json")
  business_p95=$(jq -r '.metrics["http_req_duration{kind:business}"]["p(95)"] // "na"' "$json")
  login_p95=$(jq -r '.metrics["http_req_duration{step:session_login}"]["p(95)"] // "na"' "$json")
  fail_rate=$(jq -r '.metrics["http_req_failed{kind:business}"].value // "na"' "$json")
  http_reqs=$(jq -r '.metrics.http_reqs.count // "na"' "$json")
  rps=$(jq -r '.metrics.http_reqs.rate // "na"' "$json")
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
    "$vus" "$exit_code" "$steps_min" "$flow_success" "$business_p95" \
    "$login_p95" "$fail_rate" "$http_reqs" "$rps" | tee -a "$summary"
  if (( exit_code == 0 )); then
    echo "[mixed-5m] first stable level: ${vus} users"
    break
  fi
done

echo "[mixed-5m] finished"
