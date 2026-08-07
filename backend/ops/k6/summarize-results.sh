#!/usr/bin/env bash
set -euo pipefail

printf 'file\tp95_ms\tfail_rate\tactual_rps\tdropped\thealth_fail_rate\thealth_check_rate\n'
for file in "$@"; do
  jq -r --arg file "$(basename "$file")" '[
    $file,
    (.metrics["http_req_duration{kind:endpoint}"]["p(95)"] // "na"),
    (.metrics["http_req_failed{kind:endpoint}"].value // 0),
    (.metrics.http_reqs.rate // "na"),
    (.metrics.dropped_iterations.count // 0),
    (.metrics["http_req_failed{kind:health}"].value // "na"),
    (.metrics["checks{kind:health}"].value // "na")
  ] | @tsv' "$file"
done
