# nginx layout (portal + API)

`portal-nginx` is an optional sidecar that:

- Serves the published files volume read-only under `/portal-data/`
- Proxies `/api/*` to the Quarkus publishing API

## Paths

- `/portal-data/...` -> files from the host data volume
  - Example: `/portal-data/datasets/<datasetId>/latest.json`
- `/api/...` -> proxied to `publish-api` (Quarkus)

Config file: `nginx/default.conf`.

## Why this setup

- Keeps Quarkus focused on the publish API and atomic filesystem operations
- Lets nginx handle static caching/headers and future TLS termination
