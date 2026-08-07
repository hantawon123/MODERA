#!/usr/bin/env bash
set -euo pipefail

current_container="modera-api"
previous_container="modera-api-old"
previous_image="modera-api:132"
network="infra_default"
firebase_source="/opt/modera/secrets/firebase-service-account.json"
kakao_mock_base_url="${KAKAO_MOCK_BASE_URL:-}"

if [[ "$(docker inspect -f '{{.State.Running}}' "$current_container")" != "true" ]]; then
  echo "current API container is not running; refusing to switch" >&2
  exit 1
fi
if docker container inspect "$previous_container" >/dev/null 2>&1; then
  echo "temporary previous API container already exists; refusing to overwrite it" >&2
  exit 1
fi

env_file="$(mktemp)"
cleanup() { rm -f "$env_file"; }
trap cleanup EXIT
docker inspect -f '{{range .Config.Env}}{{println .}}{{end}}' "$current_container" > "$env_file"

docker stop "$current_container" >/dev/null
extra_env=()
if [[ -n "$kakao_mock_base_url" ]]; then
  extra_env+=(
    -e "OAUTH_KAKAO_API_BASE_URL=$kakao_mock_base_url"
    -e "KAKAO_ALLOWED_APP_IDS=1525155"
  )
fi

if ! docker run -d \
  --name "$previous_container" \
  --network "$network" \
  --network-alias "$current_container" \
  --env-file "$env_file" \
  "${extra_env[@]}" \
  --mount "type=bind,src=$firebase_source,dst=/run/secrets/firebase-service-account.json,readonly" \
  "$previous_image" >/dev/null; then
  docker start "$current_container" >/dev/null
  echo "failed to start previous API; current API was restored" >&2
  exit 1
fi

echo "previous API started as $previous_container from $previous_image"
