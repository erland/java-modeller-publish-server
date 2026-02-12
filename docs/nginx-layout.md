# nginx layout (portal + API)

`portal-nginx` is an optional sidecar that:

- Serves the published files volume read-only under `/portal-data/`
- Proxies `/api/*` to the Quarkus publishing API

## Paths

- `/portal-data/...` -> files from the host data volume
  - Example: `/portal-data/datasets/<datasetId>/latest.json`
- `/api/...` -> proxied to `publish-api` (Quarkus)

Config template: `nginx/default.conf.template` (rendered at container start via envsubst).

## Why this setup

- Keeps Quarkus focused on the publish API and atomic filesystem operations
- Lets nginx handle static caching/headers and future TLS termination


## CORS configuration

Allowed CORS origins for `/api/*` are configured via the `CORS_ORIGIN_REGEX` environment variable on the `portal-nginx` service.

Example default:

- `^http://localhost(:5173)?$` (allows `http://localhost` and `http://localhost:5173`)
