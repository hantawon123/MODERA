#!/usr/bin/env bash
set -euo pipefail

result_dir="${RESULT_DIR:-/home/ubuntu/k6/final-results}"
mkdir -p "$result_dir"
: > "$result_dir/final-suite.log"

run_search() {
  local mode="$1" target="$2" min="$3" max="$4" resolution="$5" duration="$6"
  echo "START:$mode:$target" >> "$result_dir/final-suite.log"
  MODE="$mode" TARGET="$target" MIN_RATE="$min" MAX_RATE="$max" RESOLUTION="$resolution" \
    DURATION="$duration" RESULT_DIR="$result_dir" /home/ubuntu/k6/find-max-remote.sh \
    >> "$result_dir/final-suite.log" 2>&1
  echo "DONE:$mode:$target" >> "$result_dir/final-suite.log"
}

# 30-second refinement around the coarse load-test boundaries.
run_search load '/api/v1/user' 1700 2300 100 30s
run_search load '/api/v1/categories' 1900 2600 100 30s
run_search load '/api/v1/images?page=0&size=20' 50 1000 50 30s
run_search load '/api/v1/schedules?page=0&size=20' 1900 2600 100 30s
run_search load '/api/v1/documents?page=0&size=20' 1900 2600 100 30s

# 30-second overload, followed by liveness checks built into endpoint-overload.js.
run_search overload '/api/v1/user' 2000 5000 250 30s
run_search overload '/api/v1/categories' 2000 5000 250 30s
run_search overload '/api/v1/images?page=0&size=20' 250 2000 250 30s
run_search overload '/api/v1/schedules?page=0&size=20' 2000 5000 250 30s
run_search overload '/api/v1/documents?page=0&size=20' 2000 5000 250 30s

echo 'FINAL_SUITE_COMPLETE' >> "$result_dir/final-suite.log"
