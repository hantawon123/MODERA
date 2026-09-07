# Public repository security checklist

Deployment secrets must be supplied outside Git. In particular:

* Set a fresh shared `INTERNAL_TOKEN` for API, worker and AI. Local/docker Spring
  profiles and `full_stage_test.py` no longer provide a built-in token.
* Set `JWT_SECRET` when starting backend/local-infra Docker Compose; the previous
  built-in signing key has been removed. Do not reuse a previously published key.
* Never commit generated k6 requests, user tokens, private keys, or service-account
  JSON files. Example files must contain placeholders or empty secret values.
* Configure endpoints using PUBLIC_CONFIGURATION.md. No production deployment
  or server-side credentials are changed by this repository cleanup.

Historical branch content has been sanitized. This does NOT revoke leaked
credentials or erase copies in old clones, forks, pull-request refs, caches,
Actions logs, or downloaded artifacts. Revoke/rotate any exposed live credentials
and request GitHub Support removal of sensitive old PR/cached commit references.
Collaborators should clone the cleaned repository again; merging an old branch
can reintroduce the removed history. Preserve uncommitted work separately first.

Pattern scans are a bounded check, not a guarantee that all secrets or security
vulnerabilities have been found. Do not publish real runtime data as test fixtures.
