#!/usr/bin/env bash
set -euo pipefail

current_container="modera-api"
previous_container="modera-api-old"

if docker container inspect "$previous_container" >/dev/null 2>&1; then
  docker stop "$previous_container" >/dev/null || true
  docker rm "$previous_container" >/dev/null
fi
docker start "$current_container" >/dev/null
echo "current API container restored"
