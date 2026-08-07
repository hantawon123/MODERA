#!/usr/bin/env bash
set -euo pipefail

cd /home/ubuntu/mock-ai
docker build -t modera-mock-ai:perf .
internal_token="$(docker inspect modera-worker --format '{{range .Config.Env}}{{println .}}{{end}}' | sed -n 's/^INTERNAL_TOKEN=//p')"
docker rm -f modera-mock-ai >/dev/null 2>&1 || true
docker run -d --name modera-mock-ai --network infra_default \
  -e INTERNAL_TOKEN="$internal_token" -e MOCK_AI_DELAY_MS="${MOCK_AI_DELAY_MS:-10}" \
  modera-mock-ai:perf
cd /home/ubuntu/app/spring
docker compose -f docker-compose.yml \
  -f /home/ubuntu/mock-ai/docker-compose.worker-override.yml up -d modera-worker
