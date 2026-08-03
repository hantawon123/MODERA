#!/usr/bin/env bash
set -euo pipefail

levels=(${LEVELS:-125 130 150 175 200})
result_dir="${RESULT_DIR:-/home/ubuntu/k6/results/kakao-user-capacity}"
script_dir="${SCRIPT_DIR:-/home/ubuntu/k6}"
base_url="${BASE_URL:-http://modera-api:8080}"
network="${NETWORK:-infra_default}"
user_offset="${USER_OFFSET:-1000000}"
token_prefix="${KAKAO_TOKEN_PREFIX:-modera-perf-kakao}"
calls_per_session="${CALLS_PER_SESSION:-100}"
session_seconds="${SESSION_SECONDS:-120}"
startup_spread_seconds="${STARTUP_SPREAD_SECONDS:-10}"
liveness_duration="${LIVENESS_DURATION:-2m20s}"
max_duration="${MAX_DURATION:-3m}"
bootstrap="${BOOTSTRAP_USERS:-true}"
bootstrap_vus="${BOOTSTRAP_VUS:-20}"
cooldown_seconds="${COOLDOWN_SECONDS:-30}"

mkdir -p "$result_dir"
summary="$result_dir/capacity.tsv"
printf 'vus\tplanned_app_requests\tapp_requests\tapp_errors\tauth_errors\tcompleted_flows\tfailed_flows\tapp_p95_ms\tauth_p95_ms\trps\tk6_exit\n' > "$summary"

max_users=0
for level in "${levels[@]}"; do
  if (( level > max_users )); then max_users=$level; fi
done

if [[ "$bootstrap" == "true" ]]; then
  echo "[bootstrap] preparing ${max_users} distinct Kakao users from offset ${user_offset}"
  docker run --rm --user 0:0 --network "$network" \
    -v "$script_dir:/scripts:ro" -v "$result_dir:/results" \
    grafana/k6 run --quiet \
    -e BASE_URL="$base_url" -e USER_COUNT="$max_users" -e USER_OFFSET="$user_offset" \
    -e BOOTSTRAP_VUS="$bootstrap_vus" -e KAKAO_TOKEN_PREFIX="$token_prefix" \
    --summary-export="/results/bootstrap.json" /scripts/kakao-user-bootstrap.js \
    > "$result_dir/bootstrap.log" 2>&1
  echo "[bootstrap] complete"
fi

for vus in "${levels[@]}"; do
  echo "[progressive] ${vus} distinct Kakao users x ${calls_per_session} app calls over ${session_seconds} seconds"
  set +e
  docker run --rm --user 0:0 --network "$network" \
    -v "$script_dir:/scripts:ro" -v "$result_dir:/results" \
    grafana/k6 run --quiet \
    -e BASE_URL="$base_url" -e VUS="$vus" -e USER_OFFSET="$user_offset" \
    -e KAKAO_TOKEN_PREFIX="$token_prefix" -e CALLS_PER_SESSION="$calls_per_session" \
    -e SESSION_SECONDS="$session_seconds" -e STARTUP_SPREAD_SECONDS="$startup_spread_seconds" \
    -e LIVENESS_DURATION="$liveness_duration" -e MAX_DURATION="$max_duration" \
    --summary-export="/results/vus-${vus}.json" /scripts/kakao-user-capacity.js \
    > "$result_dir/vus-${vus}.log" 2>&1
  exit_code=$?
  set -e

  json="$result_dir/vus-${vus}.json"
  planned_calls=$((vus * calls_per_session))
  app_errors=$(jq -r '.metrics["http_req_failed{kind:app}"].passes // 0' "$json")
  app_successes=$(jq -r '.metrics["http_req_failed{kind:app}"].fails // 0' "$json")
  app_requests=$((app_errors + app_successes))
  auth_errors=$(jq -r '.metrics["http_req_failed{kind:kakao_auth}"].passes // 0' "$json")
  completed_flows=$(jq -r '.metrics.kakao_flow_executions.count // 0' "$json")
  failed_flows=$(jq -r '.metrics.kakao_flow_success.fails // 0' "$json")
  app_p95=$(jq -r '.metrics["http_req_duration{kind:app}"]["p(95)"] // "na"' "$json")
  auth_p95=$(jq -r '.metrics["http_req_duration{kind:kakao_auth}"]["p(95)"] // "na"' "$json")
  rps=$(jq -r '.metrics.http_reqs.rate // "na"' "$json")
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
    "$vus" "$planned_calls" "$app_requests" "$app_errors" "$auth_errors" \
    "$completed_flows" "$failed_flows" "$app_p95" "$auth_p95" "$rps" "$exit_code" \
    | tee -a "$summary"

  if (( app_errors > 0 || auth_errors > 0 || failed_flows > 0 || completed_flows < vus )); then
    echo "[progressive] first functional failure at ${vus} users"
    break
  fi

  if (( cooldown_seconds > 0 )); then
    echo "[progressive] cooldown ${cooldown_seconds}s"
    sleep "$cooldown_seconds"
  fi
done

echo "[progressive] finished"
