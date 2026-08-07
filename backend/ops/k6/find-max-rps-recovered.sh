#!/usr/bin/env bash
set -euo pipefail

test_name="${TEST_NAME:-kakao-login}"
script="${SCRIPT:-kakao-login-rps.js}"
kind="${KIND:-kakao_login}"
target="${TARGET:-/api/v1/categories}"
min_rate="${MIN_RATE:-50}"
max_rate="${MAX_RATE:-1000}"
resolution="${RESOLUTION:-10}"
duration="${DURATION:-60s}"
base_url="${BASE_URL:-http://modera-api:8080}"
result_dir="${RESULT_DIR:-/home/ubuntu/k6/results/rps-recovered}"
host_script_dir="${HOST_SCRIPT_DIR:-/home/ubuntu/k6}"
network="${NETWORK:-infra_default}"
k6_image="${K6_IMAGE:-grafana/k6}"
user_offset="${USER_OFFSET:-10000000}"
user_count="${USER_COUNT:-4000}"
token_prefix="${KAKAO_TOKEN_PREFIX:-modera-perf-kakao}"
pre_allocated_vus="${PRE_ALLOCATED_VUS:-}"
max_vus="${MAX_VUS:-}"
recovery_timeout="${RECOVERY_TIMEOUT_SECONDS:-300}"
recovery_interval="${RECOVERY_INTERVAL_SECONDS:-15}"

if (( min_rate < 1 || max_rate < min_rate || resolution < 1 )); then
  echo "invalid search bounds" >&2
  exit 2
fi

mkdir -p "$result_dir"
summary="$result_dir/capacity.tsv"
printf 'target_rps\texit\tp95_ms\tfail_rate\tchecks_rate\tdropped\tactual_rps\tverdict\n' > "$summary"

prometheus_query() {
  local query="$1"
  curl -fsSG --data-urlencode "query=$query" http://127.0.0.1:9090/api/v1/query \
    | jq -r '.data.result[] | select(.metric.application == "modera-api") | .value[1]' \
    | head -n 1
}

wait_for_pool_recovery() {
  local deadline=$((SECONDS + recovery_timeout))
  local previous_timeouts=""
  local stable_samples=0

  while (( SECONDS < deadline )); do
    local active pending timeouts
    active="$(prometheus_query 'hikaricp_connections_active' || true)"
    pending="$(prometheus_query 'hikaricp_connections_pending' || true)"
    timeouts="$(prometheus_query 'hikaricp_connections_timeout_total' || true)"

    if [[ "$active" == "0" && "$pending" == "0" && -n "$timeouts" ]]; then
      if [[ "$timeouts" == "$previous_timeouts" ]]; then
        stable_samples=$((stable_samples + 1))
      else
        stable_samples=1
      fi
      if (( stable_samples >= 2 )); then
        echo "[recovery] active=0 pending=0 timeouts=${timeouts} stable"
        return 0
      fi
    else
      stable_samples=0
    fi

    previous_timeouts="$timeouts"
    sleep "$recovery_interval"
  done

  echo "Hikari pool did not recover within ${recovery_timeout}s" >&2
  return 1
}

run_rate() {
  local candidate="$1"
  local json="$result_dir/rps-${candidate}.json"
  local log="$result_dir/rps-${candidate}.log"
  local exit_code p95 fail_rate checks_rate dropped actual_rps verdict
  local extra_env=()

  if [[ -n "$pre_allocated_vus" ]]; then
    extra_env+=( -e PRE_ALLOCATED_VUS="$pre_allocated_vus" )
  fi
  if [[ -n "$max_vus" ]]; then
    extra_env+=( -e MAX_VUS="$max_vus" )
  fi

  wait_for_pool_recovery
  echo "[probe][$test_name] ${candidate} RPS for ${duration}"

  set +e
  docker run --rm --user 0:0 --network "$network" \
    -e BASE_URL="$base_url" -e RATE="$candidate" -e DURATION="$duration" \
    -e TARGET="$target" -e USER_OFFSET="$user_offset" -e USER_COUNT="$user_count" \
    -e KAKAO_TOKEN_PREFIX="$token_prefix" \
    "${extra_env[@]}" \
    -v "$host_script_dir:/scripts:ro" -v "$result_dir:/results" \
    "$k6_image" run --quiet --summary-export "/results/rps-${candidate}.json" "/scripts/${script}" \
    > "$log" 2>&1
  exit_code=$?
  set -e

  if [[ ! -s "$json" ]]; then
    printf '%s\t%s\tna\tna\tna\tna\tna\tFAIL_NO_SUMMARY\n' "$candidate" "$exit_code" | tee -a "$summary"
    return 1
  fi

  p95="$(jq -r --arg key "http_req_duration{kind:${kind}}" '.metrics[$key]["p(95)"] // "na"' "$json")"
  fail_rate="$(jq -r --arg key "http_req_failed{kind:${kind}}" \
    '.metrics[$key] | if . == null then "na" elif .value != null then .value else ((.passes // 0) / (((.passes // 0) + (.fails // 0)) | if . == 0 then 1 else . end)) end' "$json")"
  checks_rate="$(jq -r --arg key "checks{kind:${kind}}" \
    '.metrics[$key] | if . == null then "na" elif .value != null then .value else ((.passes // 0) / (((.passes // 0) + (.fails // 0)) | if . == 0 then 1 else . end)) end' "$json")"
  dropped="$(jq -r '.metrics.dropped_iterations.count // 0' "$json")"
  actual_rps="$(jq -r --arg key "http_reqs{kind:${kind}}" '.metrics[$key].rate // .metrics.http_reqs.rate // "na"' "$json")"
  verdict=PASS
  if (( exit_code != 0 )); then verdict=FAIL; fi

  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
    "$candidate" "$exit_code" "$p95" "$fail_rate" "$checks_rate" "$dropped" "$actual_rps" "$verdict" \
    | tee -a "$summary"
  [[ "$verdict" == PASS ]]
}

low="$min_rate"
high="$max_rate"
best=0
first_fail=0

while (( low <= high )); do
  mid=$(( ((low + high) / 2 / resolution) * resolution ))
  if (( mid < low )); then mid="$low"; fi

  if run_rate "$mid"; then
    best="$mid"
    low=$((mid + resolution))
  else
    first_fail="$mid"
    high=$((mid - resolution))
  fi
done

printf 'max_pass\t%s\nfirst_fail\t%s\nresolution\t%s\n' \
  "$best" "$first_fail" "$resolution" > "$result_dir/boundary.tsv"
echo "[result][$test_name] max_pass=${best} first_fail=${first_fail} resolution=${resolution}"
