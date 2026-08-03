#!/usr/bin/env bash
set -euo pipefail

result_dir="${RESULT_DIR:-/home/ubuntu/k6/overload-results}"
mkdir -p "$result_dir"
: > "$result_dir/overload-suite.log"

run_search() {
  local target="$1" min="$2" max="$3" resolution="$4"
  echo "START:$target" >> "$result_dir/overload-suite.log"
  MODE=overload TARGET="$target" MIN_RATE="$min" MAX_RATE="$max" RESOLUTION="$resolution" \
    DURATION=30s RESULT_DIR="$result_dir" /home/ubuntu/k6/find-max-remote.sh \
    >> "$result_dir/overload-suite.log" 2>&1
  echo "DONE:$target" >> "$result_dir/overload-suite.log"
}

run_search '/api/v1/user' 2000 5000 250
run_search '/api/v1/categories' 2000 5000 250
run_search '/api/v1/images?page=0&size=20' 100 1000 100
run_search '/api/v1/schedules?page=0&size=20' 2000 5000 250
run_search '/api/v1/documents?page=0&size=20' 2000 5000 250

echo 'OVERLOAD_SUITE_COMPLETE' >> "$result_dir/overload-suite.log"
