#!/usr/bin/env bash
set -euo pipefail

current_container="${CURRENT_CONTAINER:-modera-api}"
test_container="${TEST_CONTAINER:-modera-api-current-test}"

if docker container inspect "$test_container" >/dev/null 2>&1; then
  docker stop "$test_container" >/dev/null || true
  docker rm "$test_container" >/dev/null
fi
docker start "$current_container" >/dev/null
echo "original current API container restored"
