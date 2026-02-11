package se.erland.pwamodeller.publishing.service;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import se.erland.pwamodeller.publishing.api.PublishingException;
import se.erland.pwamodeller.publishing.config.PublishConfig;
import se.erland.pwamodeller.publishing.fs.FileOps;
import se.erland.pwamodeller.publishing.policy.DatasetIdPolicy;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.Optional;

/**
 * Step 3 + Step 5 hardening:
 * - Write bundle to a temp dir, then move to final dir.
 * - Update latest.json last (atomic rename).
 * - Best-effort rollback: if latest write fails, remove new bundle dir.
 * - Optional ZIP archiving.
 */
public class PublisherService {

    private final PublishConfig config;

    public PublisherService(PublishConfig config) {
        this.config = config;
    }

    public PublishResult publish(String datasetId, ValidatedBundle vb, Optional<String> datasetTitleOpt) {
        DatasetIdPolicy.requireValid(datasetId);
        String bundleId = vb.bundleId();
        Instant now = Instant.now();

        Path dataRoot = config.getDataRoot().toAbsolutePath().normalize();
        Path bundlesRoot = dataRoot.resolve("bundles");
        Path datasetsRoot = dataRoot.resolve("datasets");

        Path bundleFinalDir = bundlesRoot.resolve(bundleId);
        Path datasetDir = datasetsRoot.resolve(datasetId);

        Path tempBundleDir = bundlesRoot.resolve(".tmp." + bundleId + "." + now.toEpochMilli());

        boolean bundleCreated = false;

        try {
            FileOps.ensureDir(bundlesRoot);
            FileOps.ensureDir(datasetsRoot);
            FileOps.ensureDir(datasetDir);

            if (Files.exists(bundleFinalDir)) {
                throw PublishingException.conflict("Bundle already exists: " + bundleId);
            }

            // 1) Copy bundle files to temp dir
            FileOps.ensureDir(tempBundleDir);
            Files.copy(vb.manifestPath(), tempBundleDir.resolve("manifest.json"), StandardCopyOption.COPY_ATTRIBUTES);
            Files.copy(vb.modelPath(), tempBundleDir.resolve("model.json"), StandardCopyOption.COPY_ATTRIBUTES);
            Files.copy(vb.indexesPath(), tempBundleDir.resolve("indexes.json"), StandardCopyOption.COPY_ATTRIBUTES);

            // 2) Move temp dir -> final dir (atomic if supported)
            moveDir(tempBundleDir, bundleFinalDir);
            bundleCreated = true;

            // 3) Upsert dataset metadata
            upsertDatasetJson(datasetDir, datasetId, datasetTitleOpt, now);

            // 4) Append release log (best-effort)
            try {
                appendRelease(datasetDir, bundleId, now);
            } catch (Exception ignored) {}

            // 5) latest.json LAST (atomic)
            writeLatest(datasetDir, datasetId, bundleId, now);

            // 6) Optional archive of uploaded ZIP
            archiveZipIfConfigured(datasetId, bundleId, vb);

            // 7) Cleanup staging
            cleanupQuietly(vb.bundleDir().toAbsolutePath().normalize().getParent().getParent()); // staging/<requestId>

            Optional<URI> latestUrl = config.getBaseUrl().map(b -> ensureSlash(b).resolve("datasets/" + datasetId + "/latest.json"));
            Optional<URI> manifestUrl = config.getBaseUrl().map(b -> ensureSlash(b).resolve("bundles/" + bundleId + "/manifest.json"));

            return new PublishResult(datasetId, bundleId, now.toString(), latestUrl, manifestUrl);

        } catch (PublishingException pe) {
            rollbackQuietly(bundleCreated, bundleFinalDir);
            rollbackQuietly(true, tempBundleDir);
            throw pe;
        } catch (Exception e) {
            rollbackQuietly(bundleCreated, bundleFinalDir);
            rollbackQuietly(true, tempBundleDir);
            throw new PublishingException(500, "Failed to publish bundle", e);
        }
    }

    /** Overridable for tests. */
    protected void writeLatest(Path datasetDir, String datasetId, String bundleId, Instant now) throws IOException {
        String manifestUrl = config.getBaseUrl().isPresent()
                ? ensureSlash(config.getBaseUrl().get()).resolve("bundles/" + bundleId + "/manifest.json").toString()
                : "../../bundles/" + bundleId + "/manifest.json";

        JsonObject latest = Json.createObjectBuilder()
                .add("datasetId", datasetId)
                .add("bundleId", bundleId)
                .add("manifestUrl", manifestUrl)
                .add("publishedAt", now.toString())
                .build();

        Path latestPath = datasetDir.resolve("latest.json");
        FileOps.atomicWriteUtf8(latestPath, latest.toString());
    }

    private void archiveZipIfConfigured(String datasetId, String bundleId, ValidatedBundle vb) {
        try {
            Optional<Path> archRoot = config.getArchiveRoot();
            if (archRoot.isEmpty()) return;

            Path archiveRoot = archRoot.get().toAbsolutePath().normalize();
            Path dstDir = archiveRoot.resolve(datasetId);
            FileOps.ensureDir(dstDir);

            // staging/<requestId>/upload.zip
            Path bundleDir = vb.bundleDir().toAbsolutePath().normalize();
            Path stagingDir = bundleDir.getParent().getParent(); // .../<requestId>/unpack/<bundleId> -> .../<requestId>
            Path zipFile = stagingDir.resolve("upload.zip");
            if (!Files.isRegularFile(zipFile)) return;

            Path dst = dstDir.resolve(bundleId + ".zip");
            Files.copy(zipFile, dst, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {}
    }

    private static void upsertDatasetJson(Path datasetDir, String datasetId, Optional<String> titleOpt, Instant now) throws IOException {
        Path datasetJson = datasetDir.resolve("dataset.json");
        boolean exists = Files.isRegularFile(datasetJson);

        String title = titleOpt.orElseGet(() -> {
            if (!exists) return datasetId;
            try (var reader = Files.newBufferedReader(datasetJson, StandardCharsets.UTF_8);
                 JsonReader jr = Json.createReader(reader)) {
                JsonObject obj = jr.readObject();
                String t = obj.getString("title", "").trim();
                return t.isEmpty() ? datasetId : t;
            } catch (Exception e) {
                return datasetId;
            }
        });

        JsonObjectBuilder b = Json.createObjectBuilder()
                .add("datasetId", datasetId)
                .add("title", title)
                .add("updatedAt", now.toString());

        if (exists) {
            try (var reader = Files.newBufferedReader(datasetJson, StandardCharsets.UTF_8);
                 JsonReader jr = Json.createReader(reader)) {
                JsonObject prev = jr.readObject();
                String createdAt = prev.getString("createdAt", "").trim();
                b.add("createdAt", createdAt.isEmpty() ? now.toString() : createdAt);
            } catch (Exception e) {
                b.add("createdAt", now.toString());
            }
        } else {
            b.add("createdAt", now.toString());
        }

        FileOps.atomicWriteUtf8(datasetJson, b.build().toString());
    }

    private static void appendRelease(Path datasetDir, String bundleId, Instant now) throws IOException {
        Path releasesPath = datasetDir.resolve("releases.json");

        JsonObject release = Json.createObjectBuilder()
                .add("bundleId", bundleId)
                .add("publishedAt", now.toString())
                .build();

        JsonArrayBuilder arr = Json.createArrayBuilder();

        if (Files.isRegularFile(releasesPath)) {
            try (var reader = Files.newBufferedReader(releasesPath, StandardCharsets.UTF_8);
                 JsonReader jr = Json.createReader(reader)) {
                var existing = jr.readArray();
                existing.forEach(arr::add);
            } catch (Exception ignored) {}
        }

        arr.add(release);
        FileOps.atomicWriteUtf8(releasesPath, arr.build().toString());
    }

    private static void moveDir(Path src, Path dst) throws IOException {
        try {
            Files.move(src, dst, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(src, dst);
        }
    }

    private static URI ensureSlash(URI base) {
        String s = base.toString();
        if (!s.endsWith("/")) s += "/";
        return URI.create(s);
    }

    private static void rollbackQuietly(boolean should, Path path) {
        if (!should || path == null) return;
        cleanupQuietly(path);
    }

    private static void cleanupQuietly(Path dir) {
        if (dir == null) return;
        try (var walk = Files.walk(dir)) {
            walk.sorted((a,b) -> b.compareTo(a)).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (IOException ignored) {}
            });
        } catch (IOException ignored) {}
    }
}
