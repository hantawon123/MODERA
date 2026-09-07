# Public repository configuration

Real deployment endpoints and generated user/token fixtures must not be committed.

* Android: supply `API_BASE_URL` and `MEDIA_BASE_URL` using Gradle `-P` options,
  user-level `~/.gradle/gradle.properties`, or CI environment variables. API_BASE_URL
  is the public API origin (including its port); MEDIA_BASE_URL is the storage origin.
  Public defaults use example.com and do not connect to the former deployment.
* AI: set `GEMINI_BASE_URL` in the untracked deployment `.env`. It is required
  for real AI calls; MOCK_AI uses an inert example endpoint.
* Spring Compose: set `SERVER_URL` to the public API origin.
* Infra Compose: set `PUBLIC_HOST` to the TLS certificate hostname, and
  `GRAFANA_ROOT_URL` to the external Grafana URL. Nginx now renders
  `nginx/templates/default.conf.template`; deploy this template with the updated
  Compose file, not only the previous conf.d file. Nginx request variables remain intact.
* k6: regenerate `backend/ops/k6/generated/` locally. It is ignored because it
  contains user credentials/tokens. Keep synthetic examples outside that directory.

Before deployment, inject the actual private values and validate Compose, Android
builds and login/image URL behavior. Repository edits do not change a running server.
Removing files from branch tips does not erase old commits, tags, PR refs, forks,
CI artifacts or clones. Revoke exposed live tokens and coordinate history cleanup
separately; never publish tokens while checking them.
