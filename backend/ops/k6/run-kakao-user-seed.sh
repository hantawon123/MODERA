#!/usr/bin/env bash
# 성능 테스트 사용자 데이터 시딩 러너 (서버 실행용).
#
# 전제:
#   - mock Kakao가 떠 있고 대상 API가 mock Kakao 설정으로 기동돼 있다
#     (start-current-api-test.sh 또는 start-previous-api.sh)
#   - worker가 fastapi 클라이언트 + 확장 mock-ai(ops/mock-ai/server.py 최신)로
#     전환돼 있다 (docker-compose.worker-override.yml / deploy-remote.sh 절차)
#
# 산출:
#   $RESULT_DIR/manifest.tsv  단계별 타임스탬프(Prometheus 구간 조회용)
#   $RESULT_DIR/seed.json     k6 요약
#   $RESULT_DIR/seed.log      k6 전체 로그
#   $RESULT_DIR/verify.log    시딩 검증 결과
set -euo pipefail

result_dir="${RESULT_DIR:-/home/ubuntu/k6/results/seed-$(date +%Y%m%d-%H%M%S)}"
script_dir="${SCRIPT_DIR:-/home/ubuntu/k6}"
network="${NETWORK:-infra_default}"
base_url="${BASE_URL:-http://modera-api:8080}"
users="${USERS:-200}"
user_offset="${USER_OFFSET:-3000000}"
seed_vus="${SEED_VUS:-10}"
seed_rate="${SEED_RATE:-25}"
# Redis 컨테이너 이름은 환경마다 다르다(로컬 modera-redis, 배포 infra-redis-1).
# 지정이 없으면 실행 중인 컨테이너에서 찾는다 — 이름을 잘못 넣으면 검증 단계가
# 통째로 죽는다(실제로 첫 실행에서 그렇게 됐다).
redis_container="${REDIS_CONTAINER:-$(docker ps --format '{{.Names}}' \
  | grep -i redis | grep -vi exporter | head -1)}"

mkdir -p "$result_dir"
manifest="$result_dir/manifest.tsv"
stamp() { printf '%s\t%s\t%s\n' "$1" "$(date +%s)" "$(date -Iseconds)" | tee -a "$manifest"; }

printf 'phase\tepoch\tiso\n' > "$manifest"
stamp seed_start

# DOCS_PER_USER 기본 0: 운영 api의 ai.base-url이 실제 AI를 보고 있으면 문서 생성이
# 실 LLM 토큰을 쓴다. mock으로 전환된 테스트 컨테이너에서만 1 이상으로 올릴 것.
docker run --rm --user 0:0 --network "$network" \
  -v "$script_dir:/scripts:ro" -v "$result_dir:/results" \
  grafana/k6 run --quiet \
  -e BASE_URL="$base_url" -e USERS="$users" -e USER_OFFSET="$user_offset" \
  -e SEED_VUS="$seed_vus" -e SEED_RATE="$seed_rate" \
  -e DOCS_PER_USER="${DOCS_PER_USER:-0}" \
  -e IMAGES_LIGHT="${IMAGES_LIGHT:-50}" -e IMAGES_MEDIUM="${IMAGES_MEDIUM:-200}" \
  -e IMAGES_HEAVY="${IMAGES_HEAVY:-500}" \
  --summary-export=/results/seed.json /scripts/kakao-user-seed.js \
  > "$result_dir/seed.log" 2>&1 || echo "[seed] k6 exit=$? (seed.log 확인)"

stamp seed_k6_done

# 검증은 정보 제공용이다. 개별 확인이 실패해도(컨테이너 이름 불일치 등) 시딩 자체를
# 실패로 만들지 않도록 set -e의 영향을 끊는다.
set +e
{
  echo "== 스트림 소화 검증 (lag/pending이 0으로 수렴해야 함) =="
  echo "(redis container: ${redis_container:-미검출})"
  for i in 1 2 3; do
    docker exec "$redis_container" redis-cli XINFO GROUPS image-analysis | paste - - | tr '\n' ' '
    echo
    sleep 10
  done

  echo "== 사용자 표본 검증 (라이트/미들/헤비 각 1명) =="
  for idx in 1 6 9; do
    n=$((user_offset + idx))
    token=$(docker run --rm --network "$network" curlimages/curl -s \
      -X POST "$base_url/api/v1/auth/kakao/login" \
      -H 'Content-Type: application/json' \
      -d "{\"kakaoAccessToken\":\"modera-perf-kakao-$n\",\"deviceId\":\"k6-kakao-device-$n\"}" \
      | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
    echo "-- user $n --"
    docker run --rm --network "$network" curlimages/curl -s \
      "$base_url/api/v1/images?page=0&size=1" -H "Authorization: Bearer $token" \
      | sed -n 's/.*"totalElements":\([0-9]*\).*/이미지 totalElements=\1/p' || true
    docker run --rm --network "$network" curlimages/curl -s \
      "$base_url/api/v1/categories" -H "Authorization: Bearer $token" \
      | grep -o '"categoryName":"[^"]*"' | sort | uniq -c || true
    docker run --rm --network "$network" curlimages/curl -s \
      "$base_url/api/v1/schedules?page=0&size=1" -H "Authorization: Bearer $token" \
      | sed -n 's/.*"totalElements":\([0-9]*\).*/일정 totalElements=\1/p' || true
  done
} | tee "$result_dir/verify.log"
set -e

stamp seed_verified
echo "[seed] 완료. 결과: $result_dir"
