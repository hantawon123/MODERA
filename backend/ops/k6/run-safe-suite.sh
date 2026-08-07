#!/usr/bin/env bash
set -euo pipefail

result_dir="${RESULT_DIR:-/home/ubuntu/k6/results}"
mkdir -p "$result_dir"
: > "$result_dir/suite.log"

targets=(
  '/api/v1/user'
  '/api/v1/categories'
  '/api/v1/images?page=0&size=20'
  '/api/v1/schedules?page=0&size=20'
  '/api/v1/documents?page=0&size=20'
)

for target in "${targets[@]}"; do
  echo "START:$target" >> "$result_dir/suite.log"
  MODE=load TARGET="$target" MIN_RATE="${MIN_RATE:-100}" MAX_RATE="${MAX_RATE:-5000}" \
    RESOLUTION="${RESOLUTION:-250}" DURATION="${DURATION:-15s}" RESULT_DIR="$result_dir" \
    /home/ubuntu/k6/find-max-remote.sh >> "$result_dir/suite.log" 2>&1
  echo "DONE:$target" >> "$result_dir/suite.log"
done

echo 'SUITE_COMPLETE' >> "$result_dir/suite.log"
