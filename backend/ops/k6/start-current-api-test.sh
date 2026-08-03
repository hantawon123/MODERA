#!/usr/bin/env bash
set -euo pipefail

current_container="${CURRENT_CONTAINER:-modera-api}"
test_container="${TEST_CONTAINER:-modera-api-current-test}"
current_image="${CURRENT_IMAGE:-modera-api:135}"
network="${NETWORK:-infra_default}"
firebase_source="${FIREBASE_SOURCE:-/opt/modera/secrets/firebase-service-account.json}"
kakao_mock_base_url="${KAKAO_MOCK_BASE_URL:-http://modera-mock-kakao:8080}"

if [[ "$(docker inspect -f '{{.State.Running}}' "$current_container")" != "true" ]]; then
  echo "current API container is not running; refusing to switch" >&2
  exit 1
fi
if docker container inspect "$test_container" >/dev/null 2>&1; then
  echo "temporary current-version API container already exists; refusing to overwrite it" >&2
  exit 1
fi

env_file="$(mktemp)"
cleanup() { rm -f "$env_file"; }
trap cleanup EXIT
docker inspect -f '{{range .Config.Env}}{{println .}}{{end}}' "$current_container" > "$env_file"

docker stop "$current_container" >/dev/null
if ! docker run -d \
  --name "$test_container" \
  --network "$network" \
  --network-alias "$current_container" \
  --env-file "$env_file" \
  -e "OAUTH_KAKAO_API_BASE_URL=$kakao_mock_base_url" \
  -e "KAKAO_ALLOWED_APP_IDS=1525155" \
  --mount "type=bind,src=$firebase_source,dst=/run/secrets/firebase-service-account.json,readonly" \
  "$current_image" >/dev/null; then
  docker start "$current_container" >/dev/null
  echo "failed to start current-version test API; original API was restored" >&2
  exit 1
fi

echo "current-version test API started as $test_container from $current_image"
