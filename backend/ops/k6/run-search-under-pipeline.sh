#!/usr/bin/env bash
# E3: 업로드 파이프라인 부하 중 시맨틱 검색 지연 실험 — 컨슈머 분리(B)의 증명.
#
# pipeline-load.js(업로드 RATE jobs/s)를 배경으로 깔고, 15초 뒤부터
# semantic-search-load.js(검색 SEARCH_RATE/s)를 겹쳐 돌린다.
#
# 판정: 검색 실행 창(manifest의 search_start~search_done) 동안
#   - 개선 전(단일 컨슈머): 검색이 분석 이벤트 뒤에 줄 서서 p95 수 초,
#     심하면 10초 상한 초과(504 AI_SEARCH_TIMEOUT)
#   - 개선 후(전용 스레드): 업로드 부하와 무관하게 검색 p95 수백 ms, 504 = 0건
set -euo pipefail

result_dir="${RESULT_DIR:-/home/ubuntu/k6/results/search-under-pipeline-$(date +%Y%m%d-%H%M%S)}"
script_dir="${SCRIPT_DIR:-/home/ubuntu/k6}"
network="${NETWORK:-infra_default}"
base_url="${BASE_URL:-http://modera-api:8080}"
pipeline_rate="${PIPELINE_RATE:-25}"
pipeline_duration="${PIPELINE_DURATION:-150s}"
search_rate="${SEARCH_RATE:-5}"
search_duration="${SEARCH_DURATION:-90s}"
search_user="${SEARCH_USER_NUMBER:-3000009}"
warmup_seconds="${WARMUP_SECONDS:-15}"

mkdir -p "$result_dir"
manifest="$result_dir/manifest.tsv"
stamp() { printf '%s\t%s\t%s\n' "$1" "$(date +%s)" "$(date -Iseconds)" | tee -a "$manifest"; }
printf 'phase\tepoch\tiso\n' > "$manifest"

stamp pipeline_start
# pipeline-common.js가 /images/의 실제 파일을 open하므로 이미지 디렉터리를 마운트한다.
docker run --rm --user 0:0 --network "$network" \
  -v "$script_dir:/scripts:ro" -v "$script_dir/images:/images:ro" -v "$result_dir:/results" \
  grafana/k6 run --quiet \
  -e BASE_URL="$base_url" -e RATE="$pipeline_rate" -e DURATION="$pipeline_duration" \
  --summary-export=/results/pipeline.json /scripts/pipeline-load.js \
  > "$result_dir/pipeline.log" 2>&1 &
pipeline_pid=$!

sleep "$warmup_seconds"
stamp search_start
set +e
docker run --rm --user 0:0 --network "$network" \
  -v "$script_dir:/scripts:ro" -v "$result_dir:/results" \
  grafana/k6 run --quiet \
  -e BASE_URL="$base_url" -e RATE="$search_rate" -e DURATION="$search_duration" \
  -e USER_NUMBER="$search_user" \
  --summary-export=/results/search.json /scripts/semantic-search-load.js \
  > "$result_dir/search.log" 2>&1
search_exit=$?
stamp search_done

wait "$pipeline_pid"
pipeline_exit=$?
set -e
stamp pipeline_done
{
  echo "search_exit=$search_exit"
  echo "pipeline_exit=$pipeline_exit"
} | tee -a "$manifest"
echo "[search-under-pipeline] 완료. 결과: $result_dir"
