# Run with containers (Quarkus + optional nginx sidecar)

This project can run as:

1) **Quarkus API only** (container)
2) **Quarkus API + nginx** (recommended) where nginx:
   - serves published static files under `/portal-data/`
   - proxies `/api/` to the Quarkus service

## Prereqs

- Docker Compose or Podman Compose
- A host folder to store persistent data, staging and (optional) archive.

Default host root used by `compose.yaml` is `/srv/pwa-publish`.
You can override by setting `PUBLISH_HOST_ROOT`.

## Start

```bash
# Optional: choose a different host root (example)
export PUBLISH_HOST_ROOT="$PWD/runtime"

mkdir -p "${PUBLISH_HOST_ROOT:-/srv/pwa-publish}"/{data,staging,archive}

docker compose up --build
```

Then:

- API: `http://localhost:8080/api/health`
- Static data: `http://localhost:8080/portal-data/`

## Environment variables

These are read by the server (system property wins over env var):

- `PUBLISH_DATA_ROOT` (required)
- `PUBLISH_STAGING_ROOT` (required)
- `PUBLISH_ARCHIVE_ROOT` (optional)
- `PUBLISH_BASE_URL` (optional) e.g. `http://localhost:8080`

In `compose.yaml`, these are set to container paths under `/srv/pwa-publish/...`.

## nginx CORS

When running the nginx sidecar, CORS for `/api/*` is handled by nginx and is configurable via `CORS_ORIGIN_REGEX`.

Default allows:

- `http://localhost`
- `http://localhost:5173`

Override example (allow another machine):

```bash
CORS_ORIGIN_REGEX='^http://(localhost(:5173)?|192\.168\.1\.50(:5173)?)$' docker compose up --build
```
