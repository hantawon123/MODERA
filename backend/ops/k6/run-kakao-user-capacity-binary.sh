#!/usr/bin/env bash
set -euo pipefail

result_dir="${RESULT_DIR:-/home/ubuntu/k6/results/kakao-user-capacity-binary}"
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
bootstrap_vus="${BOOTSTRAP_VUS:-40}"
cooldown_seconds="${COOLDOWN_SECONDS:-30}"

# KNOWN_PASS는 동일 조건에서 이미 성공한 사용자 수다. INITIAL_UPPER부터
# 실패 상한을 찾고, 성공 하한과 실패 상한 사이를 RESOLUTION 단위로 이분 탐색한다.
known_pass="${KNOWN_PASS:-0}"
initial_upper="${INITIAL_UPPER:-300}"
max_users="${MAX_USERS:-4000}"
resolution="${RESOLUTION:-25}"

if (( known_pass < 0 || initial_upper <= known_pass || max_users < initial_upper || resolution < 1 )); then
  echo "invalid binary-search bounds" >&2
  exit 2
fi

mkdir -p "$result_dir"
summary="$result_dir/capacity.tsv"
printf 'vus\tplanned_app_requests\tapp_requests\tapp_errors\tauth_errors\tcompleted_flows\tfailed_flows\tapp_p95_ms\tauth_p95_ms\trps\tk6_exit\tverdict\n' > "$summary"

echo "[bootstrap] preparing ${max_users} users from offset ${user_offset}"
docker run --rm --user 0:0 --network "$network" \
  -v "$script_dir:/scripts:ro" -v "$result_dir:/results" \
  grafana/k6 run --quiet \
  -e BASE_URL="$base_url" -e USER_COUNT="$max_users" -e USER_OFFSET="$user_offset" \
  -e BOOTSTRAP_VUS="$bootstrap_vus" -e KAKAO_TOKEN_PREFIX="$token_prefix" \
  --summary-export="/results/bootstrap.json" /scripts/kakao-user-bootstrap.js \
  > "$result_dir/bootstrap.log" 2>&1
echo "[bootstrap] complete"

run_level() {
  local vus="$1"
  local json="$result_dir/vus-${vus}.json"
  local log="$result_dir/vus-${vus}.log"
  local exit_code app_errors app_successes app_requests auth_errors
  local completed_flows failed_flows app_p95 auth_p95 rps verdict

  echo "[probe] ${vus} users x ${calls_per_session} calls over ${session_seconds}s"
  set +e
  docker run --rm --user 0:0 --network "$network" \
    -v "$script_dir:/scripts:ro" -v "$result_dir:/results" \
    grafana/k6 run --quiet \
    -e BASE_URL="$base_url" -e VUS="$vus" -e USER_OFFSET="$user_offset" \
    -e KAKAO_TOKEN_PREFIX="$token_prefix" -e CALLS_PER_SESSION="$calls_per_session" \
    -e SESSION_SECONDS="$session_seconds" -e STARTUP_SPREAD_SECONDS="$startup_spread_seconds" \
    -e LIVENESS_DURATION="$liveness_duration" -e MAX_DURATION="$max_duration" \
    --summary-export="/results/vus-${vus}.json" /scripts/kakao-user-capacity.js \
    > "$log" 2>&1
  exit_code=$?
  set -e

  if [[ ! -s "$json" ]]; then
    printf '%s\t%s\t0\t0\t0\t0\t0\tna\tna\tna\t%s\tFAIL_NO_SUMMARY\n' \
      "$vus" "$((vus * calls_per_session))" "$exit_code" | tee -a "$summary"
    return 1
  fi

  app_errors=$(jq -r '.metrics["http_req_failed{kind:app}"].passes // 0' "$json")
  app_successes=$(jq -r '.metrics["http_req_failed{kind:app}"].fails // 0' "$json")
  app_requests=$((app_errors + app_successes))
  auth_errors=$(jq -r '.metrics["http_req_failed{kind:kakao_auth}"].passes // 0' "$json")
  completed_flows=$(jq -r '.metrics.kakao_flow_executions.count // 0' "$json")
  failed_flows=$(jq -r '.metrics.kakao_flow_success.fails // 0' "$json")
  app_p95=$(jq -r '.metrics["http_req_duration{kind:app}"]["p(95)"] // "na"' "$json")
  auth_p95=$(jq -r '.metrics["http_req_duration{kind:kakao_auth}"]["p(95)"] // "na"' "$json")
  rps=$(jq -r '.metrics.http_reqs.rate // "na"' "$json")

  verdict=PASS
  if (( app_errors > 0 || auth_errors > 0 || failed_flows > 0 || completed_flows < vus )); then
    verdict=FAIL
  fi
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
    "$vus" "$((vus * calls_per_session))" "$app_requests" "$app_errors" "$auth_errors" \
    "$completed_flows" "$failed_flows" "$app_p95" "$auth_p95" "$rps" "$exit_code" "$verdict" \
    | tee -a "$summary"

  if (( cooldown_seconds > 0 )); then sleep "$cooldown_seconds"; fi
  [[ "$verdict" == PASS ]]
}

low="$known_pass"
high=0
candidate="$initial_upper"

# 성공 지점만 계속 나오면 후보를 두 배로 올려 최초 실패 상한을 찾는다.
while (( candidate <= max_users )); do
  if run_level "$candidate"; then
    low="$candidate"
    if (( candidate == max_users )); then break; fi
    candidate=$((candidate * 2))
    if (( candidate > max_users )); then candidate="$max_users"; fi
  else
    high="$candidate"
    break
  fi
done

if (( high == 0 )); then
  printf 'status\tLOWER_BOUND_ONLY\nmax_pass\t%s\nfirst_fail\tNA\nresolution\t%s\n' \
    "$low" "$resolution" > "$result_dir/boundary.tsv"
  echo "[result] no failure through ${low} users"
  exit 0
fi

# 경계의 변동성을 고려해 기본 25명 단위까지 좁힌다.
while (( high - low > resolution )); do
  mid=$((((low + high) / 2 / resolution) * resolution))
  if (( mid <= low )); then mid=$((low + resolution)); fi
  if run_level "$mid"; then low="$mid"; else high="$mid"; fi
done

printf 'status\tBOUNDED\nmax_pass\t%s\nfirst_fail\t%s\nresolution\t%s\n' \
  "$low" "$high" "$resolution" > "$result_dir/boundary.tsv"
echo "[result] max pass ${low}; first fail ${high}; resolution ${resolution}"
