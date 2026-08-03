#!/usr/bin/env bash
set -euo pipefail

levels=(${LEVELS:-80 100 120 150 200 250 300 350 400})
result_dir="${RESULT_DIR:-/home/ubuntu/k6/results/progressive-5m-200}"
script_dir="${SCRIPT_DIR:-/home/ubuntu/k6}"
base_url="${BASE_URL:-http://modera-api-old:8080}"
network="${NETWORK:-infra_default}"
login_id="${LOGIN_ID:-k6tester}"
password="${PASSWORD:-password123}"
email="${EMAIL:-k6tester@example.com}"
mkdir -p "$result_dir"
summary="$result_dir/capacity.tsv"
printf 'vus\tplanned_calls\tbusiness_requests\terror_requests\tcompleted_flows\tfailed_flows\tbusiness_p95_ms\tlogin_p95_ms\trps\tk6_exit\n' > "$summary"

for vus in "${levels[@]}"; do
  echo "[progressive] ${vus} users x 200 calls over 5 minutes"
  set +e
  docker run --rm --user 0:0 --network "$network" \
    -v "$script_dir:/scripts:ro" -v "$result_dir:/results" \
    grafana/k6 run --quiet \
    -e BASE_URL="$base_url" -e VUS="$vus" -e CALLS_PER_SESSION=200 \
    -e LOGIN_ID="$login_id" -e PASSWORD="$password" -e EMAIL="$email" \
    -e SESSION_SECONDS=300 -e STARTUP_SPREAD_SECONDS=10 -e THINK_TIME=0 \
    -e LIVENESS_DURATION=5m20s -e MAX_DURATION=7m \
    --summary-export="/results/vus-${vus}.json" /scripts/mixed-user-scenarios.js \
    > "$result_dir/vus-${vus}.log" 2>&1
  exit_code=$?
  set -e

  json="$result_dir/vus-${vus}.json"
  planned_calls=$((vus * 200))
  error_requests=$(jq -r '.metrics["http_req_failed{kind:business}"].passes // 0' "$json")
  successful_requests=$(jq -r '.metrics["http_req_failed{kind:business}"].fails // 0' "$json")
  business_requests=$((error_requests + successful_requests))
  completed_flows=$(jq -r '.metrics.mixed_flow_executions.count // 0' "$json")
  failed_flows=$(jq -r '.metrics.mixed_flow_success.fails // 0' "$json")
  business_p95=$(jq -r '.metrics["http_req_duration{kind:business}"]["p(95)"] // "na"' "$json")
  login_p95=$(jq -r '.metrics["http_req_duration{step:session_login}"]["p(95)"] // "na"' "$json")
  rps=$(jq -r '.metrics.http_reqs.rate // "na"' "$json")
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
    "$vus" "$planned_calls" "$business_requests" "$error_requests" \
    "$completed_flows" "$failed_flows" "$business_p95" "$login_p95" "$rps" "$exit_code" \
    | tee -a "$summary"

  if (( error_requests > 0 || failed_flows > 0 || completed_flows < vus )); then
    echo "[progressive] first functional failure at ${vus} users"
    break
  fi
done

echo "[progressive] finished"
