#!/usr/bin/env bash
set -euo pipefail

MODE="${MODE:-load}"
TARGET="${TARGET:-/api/v1/categories}"
MIN_RATE="${MIN_RATE:-50}"
MAX_RATE="${MAX_RATE:-3000}"
RESOLUTION="${RESOLUTION:-50}"
DURATION="${DURATION:-60s}"
BASE_URL="${BASE_URL:-http://modera-api:8080}"
RESULT_DIR="${RESULT_DIR:-/home/ubuntu/k6/results}"
CONTAINER_RESULT_DIR="${CONTAINER_RESULT_DIR:-/results}"
SCRIPT_DIR="${SCRIPT_DIR:-/scripts}"
HOST_SCRIPT_DIR="${HOST_SCRIPT_DIR:-/home/ubuntu/k6}"
NETWORK="${NETWORK:-infra_default}"
K6_IMAGE="${K6_IMAGE:-grafana/k6}"

mkdir -p "$RESULT_DIR"
safe_name="$(printf '%s' "$TARGET" | tr '/?&={} ' '_' | tr -cd '[:alnum:]_.-')"
result_file="$RESULT_DIR/${MODE}-${safe_name}.tsv"
: > "$result_file"

low=$MIN_RATE
high=$MAX_RATE
best=0
while (( low <= high )); do
  mid=$(( ((low + high) / 2 / RESOLUTION) * RESOLUTION ))
  (( mid < low )) && mid=$low
  echo "[$MODE][$TARGET] testing ${mid} RPS"

  script="$SCRIPT_DIR/endpoint-load.js"
  if [[ "$MODE" == "overload" ]]; then script="$SCRIPT_DIR/endpoint-overload.js"; fi

  set +e
  docker run --rm --network "$NETWORK" \
    -e BASE_URL="$BASE_URL" -e TARGET="$TARGET" -e RATE="$mid" -e DURATION="$DURATION" \
    -v "$HOST_SCRIPT_DIR:$SCRIPT_DIR:ro" -v "$RESULT_DIR:$CONTAINER_RESULT_DIR" \
    "$K6_IMAGE" run --quiet --summary-export "$CONTAINER_RESULT_DIR/latest.json" "$script" \
    > "$RESULT_DIR/latest.log" 2>&1
  exit_code=$?
  set -e
  candidate_json="$RESULT_DIR/${MODE}-${safe_name}-${mid}.json"
  cp "$RESULT_DIR/latest.json" "$candidate_json"
  p95="$(jq -r '.metrics["http_req_duration{kind:endpoint}"]["p(95)"] // "na"' "$candidate_json")"
  fail_rate="$(jq -r '.metrics["http_req_failed{kind:endpoint}"].rate // "na"' "$candidate_json")"
  actual_rps="$(jq -r '.metrics.http_reqs.rate // "na"' "$candidate_json")"
  printf '%s\t%s\t%s\t%s\t%s\n' "$mid" "$exit_code" "$p95" "$fail_rate" "$actual_rps" >> "$result_file"

  if (( exit_code == 0 )); then
    best=$mid
    low=$((mid + RESOLUTION))
  else
    high=$((mid - RESOLUTION))
  fi
done

echo -e "RESULT\t$MODE\t$TARGET\t$best" | tee -a "$result_file"
