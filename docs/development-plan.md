# java-modeller-publish-server — Development plan

## Working rules for LLM-friendly steps
- Each step is implementable in **one prompt**.
- Keep steps minimal but safe (atomic publish, validation).

---

## Step 0 — Repo scaffolding (Maven WAR for JBoss EAP 8)
**Goal:** Create a buildable WAR with a minimal JAX-RS app.

Deliverables:
- `pom.xml` (packaging `war`)
- `JaxRsApplication` enabling JAX-RS under `/api`
- `HealthResource` (`GET /api/health`)
- `README.md` with build/deploy notes
- Smoke: `mvn -q test package` produces `.war`

Acceptance:
- WAR builds successfully.
- `/api/health` returns JSON.

---

## Step 1 — Configuration + filesystem helpers
**Goal:** Centralize config and safe path/file helpers.

Implement:
- `PublishConfig` reading:
  - `PUBLISH_DATA_ROOT`, `PUBLISH_STAGING_ROOT`, size limits, optional `PUBLISH_BASE_URL`
- `DatasetIdPolicy` (validate `[a-z0-9][a-z0-9-_]{1,63}`)
- `FileOps`:
  - `atomicWrite(path, bytes)` (temp + rename)
  - `safeResolveUnderRoot(root, relative)`
  - `ensureDir(path)`

Acceptance:
- Unit tests for datasetId validation and safeResolve.

---

## Step 2 — ZIP upload + validation (staging only)
**Goal:** Accept ZIP upload and validate structure/content without publishing.

Implement:
- `PublishResource`:
  - `POST /api/datasets/{datasetId}/publish` multipart (`bundleZip`)
- `ZipValidator`:
  - unzip into staging
  - protect against traversal entries
  - parse `manifest.json`, extract `bundleId`
  - verify `model.json` + `indexes.json` exist and are valid JSON
  - enforce size limits
  - return `ValidatedBundle` (bundleId + staged paths)

Acceptance:
- Invalid ZIP → `422`
- Too large → `413`
- Existing bundleId → `409`

---

## Step 3 — Publish operation (atomic dataset latest)
**Goal:** Move validated bundle into nginx-served tree and update dataset latest pointer.

Implement:
- `PublisherService.publish(datasetId, validatedBundle)`
  - write bundle files to `DATA_ROOT/bundles/<bundleId>/`
  - upsert `DATA_ROOT/datasets/<datasetId>/dataset.json`
  - append `releases.json` (recommended)
  - write `datasets/<datasetId>/latest.json` last (atomic rename)

Acceptance:
- After success, Portal can load dataset latest and resolve to bundle files.
- Simulated failure before latest update leaves latest unchanged.

---

## Step 4 — Dataset listing endpoint
**Goal:** Provide dropdown data for PWA “publish to server”.

Implement:
- `GET /api/datasets` reading `DATA_ROOT/datasets/*/dataset.json` and `latest.json`.

Acceptance:
- Returns empty list when none exist.
- Robust if metadata missing.

---

## Step 5 — Hardening (minimal, production-friendly)
**Goal:** Improve reliability without expanding scope.

Implement:
- Better error payloads (problem+json style)
- RequestId in logs/responses
- Optional ZIP archive storage (`ARCHIVE_ROOT/<datasetId>/<bundleId>.zip`)
- A few end-to-end tests using temp directories

Acceptance:
- Clear failures, no partial publish.

---

## Optional Step 6 — Access control (later)
- Container-managed security or reverse proxy SSO.
- Dataset-level ACL enforcement.

