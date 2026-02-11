# Run locally (Quarkus)

## Prereqs

- Java 17+
- Maven 3.9+

## Required environment

```bash
export PUBLISH_DATA_ROOT="$PWD/runtime/data"
export PUBLISH_STAGING_ROOT="$PWD/runtime/staging"
# optional:
# export PUBLISH_ARCHIVE_ROOT="$PWD/runtime/archive"
# export PUBLISH_BASE_URL="http://localhost:8080"

mkdir -p "$PUBLISH_DATA_ROOT" "$PUBLISH_STAGING_ROOT" "${PUBLISH_ARCHIVE_ROOT:-}"
```

## Run

```bash
mvn quarkus:dev
```

Test:

- `http://localhost:8080/api/health`

## Tests

Unit tests + Quarkus integration tests:

```bash
mvn test
```

The Quarkus integration tests boot the application with isolated temp directories
via `PublishTestResource` and exercise:

- `GET /api/health`
- `GET /api/datasets`
- `POST /api/datasets/{datasetId}/publish` (multipart)
- error responses as `application/problem+json`
