#!/usr/bin/env bash
set -euo pipefail
cd /home/ubuntu/app/spring
docker compose -f docker-compose.yml up -d modera-worker
docker rm -f modera-mock-ai >/dev/null 2>&1 || true
