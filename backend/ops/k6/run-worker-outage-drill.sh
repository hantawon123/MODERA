#!/usr/bin/env bash
# E2: worker 장애 격리 실험 — 유사 이미지 tx 분리(A)의 증명.
#
# v2 용량 시나리오(similar 포함)를 돌리는 중간에 worker 컨테이너를 pause했다가
# 되살린다. stop이 아니라 pause인 이유: stop은 connection refused로 즉시 실패해
# 커넥션 점유가 짧지만, pause는 TCP가 매달려 읽기 타임아웃(3초)을 꽉 채우는
# 최악 경로를 재현하기 때문이다.
#
# 판정: pause 창(manifest의 worker_paused~worker_resumed) 동안
#   - 개선 전(tx 안 HTTP): similar가 커넥션을 3초씩 점유 → Hikari pending↑,
#     커넥션 획득 실패 발생, 무관 API(kind:app)까지 오류·지연 전파
#   - 개선 후(tx 밖 HTTP): similar만 3초 후 빈 목록(200 degrade), kind:app의
#     오류 0·p95 유지, Hikari pending 0
set -euo pipefail

result_dir="${RESULT_DIR:-/home/ubuntu/k6/results/outage-drill-$(date +%Y%m%d-%H%M%S)}"
script_dir="${SCRIPT_DIR:-/home/ubuntu/k6}"
network="${NETWORK:-infra_default}"
base_url="${BASE_URL:-http://modera-api:8080}"
vus="${VUS:-200}"
user_offset="${USER_OFFSET:-3000000}"
worker_container="${WORKER_CONTAINER:-modera-worker}"
pause_after="${PAUSE_AFTER:-60}"
pause_seconds="${PAUSE_SECONDS:-60}"
session_seconds="${SESSION_SECONDS:-180}"
calls_per_session="${CALLS_PER_SESSION:-150}"

mkdir -p "$result_dir"
manifest="$result_dir/manifest.tsv"
stamp() { printf '%s\t%s\t%s\n' "$1" "$(date +%s)" "$(date -Iseconds)" | tee -a "$manifest"; }
printf 'phase\tepoch\tiso\n' > "$manifest"

stamp k6_start
docker run --rm --user 0:0 --network "$network" \
  -v "$script_dir:/scripts:ro" -v "$result_dir:/results" \
  grafana/k6 run --quiet \
  -e BASE_URL="$base_url" -e VUS="$vus" -e USER_OFFSET="$user_offset" \
  -e SESSION_SECONDS="$session_seconds" -e CALLS_PER_SESSION="$calls_per_session" \
  -e LIVENESS_DURATION="$((session_seconds + 20))s" -e MAX_DURATION="$((session_seconds + 60))s" \
  --summary-export=/results/outage.json /scripts/kakao-user-capacity.js \
  > "$result_dir/outage.log" 2>&1 &
k6_pid=$!

sleep "$pause_after"
docker pause "$worker_container"
stamp worker_paused
sleep "$pause_seconds"
docker unpause "$worker_container"
stamp worker_resumed

set +e
wait "$k6_pid"
k6_exit=$?
set -e
stamp k6_done
echo "k6_exit=$k6_exit" | tee -a "$manifest"
echo "[outage-drill] 완료. 결과: $result_dir (판정은 manifest 구간으로 Prometheus 조회)"
