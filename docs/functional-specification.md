# pwa-modeller-publish-server — Functional specification

## Purpose

Provide a small **Publishing Server** that receives a **publish bundle ZIP** produced by EA Modeller PWA and publishes it to an intranet-accessible static location for the Portal to load via `latest.json`.

- **Runtime:** JBoss EAP 8
- **Build:** Java + Maven → WAR
- **Repository:** `pwa-modeller-publish-server`
- **Primary goals:** correctness, atomic publishing, auditability, simple operations.
- **Out of scope (initial):** SSO integration details, fine-grained ACL UI, background processing.

## Key concepts

### Dataset
A **dataset** is a stable identifier representing “one model stream” (e.g. `tullverket-business`, `samordningsforum-demo`).

Publishing updates the dataset’s **latest pointer**:
- `/portal-data/datasets/<datasetId>/latest.json`

### Bundle (immutable version)
Each publish produces an immutable **bundle** identified by `bundleId` (already present in `manifest.json` inside the ZIP). Bundles are stored as:
- `/portal-data/bundles/<bundleId>/{manifest.json,model.json,indexes.json}`

### Atomicity rule
Publishing must be **atomic** from the Portal perspective:
- Upload + validation occur in **staging**.
- Move bundle to final location.
- Write/update `latest.json` **last** via atomic rename.

## External interfaces

### Static data root (served by nginx or similar)
The server writes files to a filesystem path that nginx serves:
- `DATA_ROOT=/var/www/ea-portal-data` (example)

Resulting URLs (example):
- `https://intranet/ea/portal-data/datasets/<datasetId>/latest.json`
- `https://intranet/ea/portal-data/bundles/<bundleId>/manifest.json`

### REST API (v1)

All endpoints are under `/api`.

#### 1) List datasets
`GET /api/datasets`

**Response 200**
```json
{
  "datasets": [
    {"datasetId":"tullverket-business","title":"Tullverket – Business","updatedAt":"2026-02-11T12:00:00Z","latestBundleId":"..."},
    {"datasetId":"samordningsforum-demo","title":"Samordningsforum – Demo","updatedAt":"...","latestBundleId":"..."}
  ]
}
```

#### 2) Create or update dataset metadata
`POST /api/datasets`

**Request**
```json
{"datasetId":"tullverket-business","title":"Tullverket – Business"}
```

**Response 201/200**
```json
{"datasetId":"tullverket-business","title":"Tullverket – Business","created":true}
```

#### 3) Publish ZIP to dataset
`POST /api/datasets/{datasetId}/publish`

- Content-Type: `multipart/form-data`
- Part name: `bundleZip`
- Optional form fields: `title`, `channel`, `environment`

**Responses**
- `201 Created` on success:
```json
{
  "datasetId":"tullverket-business",
  "bundleId":"2026-02-11T11-59-01Z_abcd123",
  "publishedAt":"2026-02-11T12:00:00Z",
  "urls":{
    "latest":"https://.../datasets/tullverket-business/latest.json",
    "manifest":"https://.../bundles/2026-.../manifest.json"
  }
}
```
- `409 Conflict` if bundleId already exists
- `413 Payload Too Large` if ZIP exceeds configured limit
- `422 Unprocessable Entity` if ZIP structure/JSON invalid
- `500` if filesystem publish fails (must not partially update latest)

#### 4) Health
`GET /api/health`
- `200 OK` with minimal JSON including build/version and data root writability.

## ZIP contract (input)

Expected ZIP contents:
```text
/<bundleId>/manifest.json
/<bundleId>/model.json
/<bundleId>/indexes.json
```

Validation rules:
- ZIP must contain required files and valid JSON.
- `manifest.json` must contain a non-empty `bundleId`.
- Enforce server-configured size limits (ZIP + JSON).
- Reject path traversal entries (`..`, absolute paths).

## Server storage & file layout

### Filesystem structure

Config values:
- `DATA_ROOT` (nginx-served)
- `STAGING_ROOT` (server-writable temp)
- `ARCHIVE_ROOT` (optional; store uploaded ZIPs)

Layout:
```text
DATA_ROOT/
  datasets/
    <datasetId>/
      latest.json
      releases.json           (optional)
      dataset.json            (server metadata)
  bundles/
    <bundleId>/
      manifest.json
      model.json
      indexes.json

STAGING_ROOT/
  <requestId-or-bundleId>/
    unpack/...
```

### Dataset metadata (server-owned)
`DATA_ROOT/datasets/<datasetId>/dataset.json`:
```json
{"datasetId":"...","title":"...","createdAt":"...","updatedAt":"..."}
```

### Release log (recommended)
`DATA_ROOT/datasets/<datasetId>/releases.json` append-only list of published bundles.

## Security

### v1 (intranet open)
- Static dataset files: open.
- API: can be open inside intranet **or** protected by reverse-proxy rules.
- Always enforce: size limits, safe path handling, datasetId validation.

### Future access control readiness
- Keep all static data under a single prefix (`/portal-data/`).
- Ensure API can return `401/403` cleanly.
- Avoid embedding secrets in published files.

## Operational requirements

### Configuration (env or system properties)
- `PUBLISH_DATA_ROOT` (required)
- `PUBLISH_STAGING_ROOT` (required)
- `PUBLISH_MAX_ZIP_BYTES` (default e.g. 100 MB)
- `PUBLISH_MAX_JSON_BYTES` (default e.g. 50 MB per JSON)
- `PUBLISH_BASE_URL` (optional; for response URLs)

### Logging & audit
Log at INFO: datasetId, bundleId, sizes, outcome/duration. Append to `releases.json` on success.

### Failure behavior
- Validation failure: no changes under `DATA_ROOT`.
- Bundle write failure: do not update `latest.json`.
- Update `latest.json` last via atomic rename.

## Suggested technology choices
- JAX-RS for API
- JSON-B or Jackson for JSON parsing
- `java.util.zip` for ZIP handling
- NIO `Files.move` for atomic publish where supported
- Maven WAR packaging
