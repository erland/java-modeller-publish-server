# java-modeller-publish-server

Publishing server for EA Modeller PWA bundles.

## Build

```bash
mvn -q test package
```

## Run with containers (recommended)

See `docs/container-run.md`.

```bash
export PUBLISH_HOST_ROOT="$PWD/runtime"
mkdir -p "$PUBLISH_HOST_ROOT"/{data,staging,archive}
docker compose up --build
```

- API: `http://localhost:8080/api/health`
- Static data: `http://localhost:8080/portal-data/`


### nginx CORS

When using the nginx sidecar, CORS for `/api/*` is controlled by `CORS_ORIGIN_REGEX` on the `portal-nginx` service (see `nginx/default.conf.template`).

## Configuration

Environment variables or system properties:

- `PUBLISH_DATA_ROOT` (required)
- `PUBLISH_STAGING_ROOT` (required)
- `PUBLISH_MAX_ZIP_BYTES` (optional, default `104857600`)
- `PUBLISH_MAX_JSON_BYTES` (optional, default `52428800`)
- `PUBLISH_BASE_URL` (optional)
- `PUBLISH_ARCHIVE_ROOT` (optional) store uploaded ZIPs as `<datasetId>/<bundleId>.zip`

## API

- `POST /api/datasets/{datasetId}/publish` (multipart field `bundleZip`, optional field `title`) validates + publishes ZIP and updates dataset `latest.json`
- `GET /api/datasets` lists available datasets

Errors are returned as `application/problem+json`. Responses include `X-Request-Id`.

## CI and releases (GitHub Actions)

This repo includes two workflows under `.github/workflows/`:

- `ci.yml` runs on pushes to `main` and pull requests and executes `mvn test package`.
- `release-image.yml` runs when you push a tag matching `v*` and builds + pushes a container image to GHCR.

### Release tagging

```bash
git tag v0.2.0
git push origin v0.2.0
```

Images will be published as:

- `ghcr.io/<owner>/<repo>:v0.2.0`
- `ghcr.io/<owner>/<repo>:latest`
