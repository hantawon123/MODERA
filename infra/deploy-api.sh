#!/usr/bin/env bash
# deploy-api.sh v2 — modera API 무중단 배포 (bounce blue-green)
# 호스트/Jenkins 양쪽에서 실행 가능: docker.sock + /home/ubuntu/app 접근만 필요
# conf 읽기는 nginx 컨테이너 exec, 쓰기는 일회용 alpine (-v 소스 경로는 데몬이 해석)
set -euo pipefail

TAG="${1:?이미지 태그 필요 (예: BUILD_NUMBER)}"

APP_DIR="/home/ubuntu/app/spring"
GREEN_COMPOSE="docker-compose.green.yml"
GP="modera-green"                       # green compose 프로젝트명 (본체 'spring'과 분리)
GREEN="modera-api-green"
MAIN="modera-api"
NGINX_CTN="infra-nginx-1"
CONF_HOST_DIR="/home/ubuntu/infra/nginx/conf.d"
CONF_IN_NGINX="/etc/nginx/conf.d/default.conf"

HEALTH_TIMEOUT=150
DRAIN=130                               # 문서 생성 동기 90s > graceful 30s 대비
ERR_THRESHOLD=0.05

log(){ echo -e "\n[$(date +%H:%M:%S)] $*"; }

current_upstream(){
  docker exec "$NGINX_CTN" sed -n 's|.*set \$api_upstream http://\([^:]*\):8080;.*|\1|p' "$CONF_IN_NGINX"
}

swap_to(){ # $1=from $2=to — 쓰기만 일회용 컨테이너로
  docker run --rm -v "$CONF_HOST_DIR":/conf alpine:3 \
    sed -i "s|set \$api_upstream http://$1:8080;|set \$api_upstream http://$2:8080;|" /conf/default.conf
  docker exec "$NGINX_CTN" nginx -t
  docker exec "$NGINX_CTN" nginx -s reload
  log "nginx 전환: $1 → $2"
}

wait_health(){ # $1=컨테이너명
  local code=000
  for ((i=0; i<HEALTH_TIMEOUT; i+=3)); do
    code=$(docker exec "$1" curl -s -o /dev/null -w '%{http_code}' \
             http://localhost:9080/actuator/health 2>/dev/null || echo 000)
    [[ "$code" == "200" ]] && { echo "  ✓ $1 health 200 (${i}s)"; return 0; }
    printf '.'; sleep 3
  done
  echo -e "\n  ✗ $1 헬스 실패"; docker logs --tail 40 "$1" || true; return 1
}

error_rate(){ # green을 경유해 prometheus 질의 (같은 infra_default 네트워크)
  docker exec "$GREEN" curl -s http://prometheus:9090/api/v1/query \
    --data-urlencode 'query=sum(rate(http_server_requests_seconds_count{status=~"5..",job="modera-api"}[2m])) / sum(rate(http_server_requests_seconds_count{job="modera-api"}[2m])) or vector(0)' 2>/dev/null \
    | sed -n 's/.*"value":\[[0-9.]*,"\([0-9.eE+-]*\)".*/\1/p'
}

# ── 0. 사전 확인: 안정 상태(modera-api)에서만 시작 ──────────────
CUR=$(current_upstream)
if [[ "$CUR" != "$MAIN" ]]; then
  echo "upstream이 $CUR — 이전 배포가 중간에 멈춘 상태. 수동 확인 후 재시도"; exit 1
fi
cd "$APP_DIR"

# ── 1. green(신버전) 기동 — 트래픽 없음 ─────────────────────────
log "green 기동: modera-api:$TAG"
DEPLOY_TAG="$TAG" docker compose -p "$GP" -f "$GREEN_COMPOSE" up -d --force-recreate
wait_health "$GREEN" || { DEPLOY_TAG="$TAG" docker compose -p "$GP" -f "$GREEN_COMPOSE" down; exit 1; }

# ── 2. 전환 → 드레인 겸 관찰 → 판정 ─────────────────────────────
swap_to "$MAIN" "$GREEN"
log "드레인 겸 관찰 ${DRAIN}s (구버전 잔여요청 소화 + 신버전 5xx 판정)"
sleep "$DRAIN"
RATE=$(error_rate); RATE="${RATE:-0}"
echo "  5xx 비율: $RATE"
if awk "BEGIN{exit !($RATE > $ERR_THRESHOLD)}"; then
  echo "  ✗ 임계 초과 — 구버전으로 원복"
  swap_to "$GREEN" "$MAIN"                      # 구버전 아직 살아있음
  DEPLOY_TAG="$TAG" docker compose -p "$GP" -f "$GREEN_COMPOSE" down
  exit 1
fi
echo "  ✓ 판정 통과"

# ── 3. 본체를 신버전으로 재생성 (트래픽은 green이 처리 중) ───────
log "$MAIN 재생성 (modera-api:latest = 이번 빌드)"
docker compose up -d --force-recreate "$MAIN"
if ! wait_health "$MAIN"; then
  echo "  ✗ 재생성된 $MAIN 헬스 실패 — 서비스는 green(신버전)이 계속 처리 중."
  echo "    conf는 green 유지. 파이프라인만 실패 처리하니 로그 확인 후 수동 마무리할 것."
  exit 1
fi

# ── 4. 복귀 전환 → 드레인 → green 제거 ─────────────────────────
swap_to "$GREEN" "$MAIN"
log "드레인 ${DRAIN}s 후 green 제거"
sleep "$DRAIN"
DEPLOY_TAG="$TAG" docker compose -p "$GP" -f "$GREEN_COMPOSE" down

log "배포 완료: $MAIN → modera-api:$TAG (안정 상태 복귀)"
