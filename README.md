# pwa-modeller-publish-server

Publishing server for EA Modeller PWA bundles.

## Build
```bash
mvn -q test package
```

## Configuration (Step 1)
Environment variables or system properties:

- `PUBLISH_DATA_ROOT` (required)
- `PUBLISH_STAGING_ROOT` (required)
- `PUBLISH_MAX_ZIP_BYTES` (optional, default 104857600)
- `PUBLISH_MAX_JSON_BYTES` (optional, default 52428800)
- `PUBLISH_BASE_URL` (optional)


## API
- `- `POST /api/datasets/{datasetId}/publish` (multipart field `bundleZip`) validates + publishes ZIP and updates dataset latest (Step 3)


- `GET /api/datasets` lists available datasets (Step 4)

- `PUBLISH_ARCHIVE_ROOT` (optional) store uploaded ZIPs as `<datasetId>/<bundleId>.zip`


## Tracing
Responses include `X-Request-Id` and errors are returned as `application/problem+json`.
