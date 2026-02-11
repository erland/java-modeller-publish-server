package se.erland.pwamodeller.publishing.it;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Boots Quarkus tests with isolated filesystem roots.
 *
 * The application reads configuration from system properties/env vars via PublishConfig.
 * For Quarkus tests we set system properties before the app starts.
 */
public class PublishTestResource implements QuarkusTestResourceLifecycleManager {

    private Path base;

    @Override
    public Map<String, String> start() {
        try {
            base = Files.createTempDirectory("pwa-publish-test-");
            Path data = base.resolve("data");
            Path staging = base.resolve("staging");
            Path archive = base.resolve("archive");

            Files.createDirectories(data);
            Files.createDirectories(staging);
            Files.createDirectories(archive);

            System.setProperty("PUBLISH_DATA_ROOT", data.toString());
            System.setProperty("PUBLISH_STAGING_ROOT", staging.toString());
            System.setProperty("PUBLISH_ARCHIVE_ROOT", archive.toString());
            System.setProperty("PUBLISH_BASE_URL", "http://localhost:8081/portal-data");

            // Keep defaults for size limits unless a test overrides them.

            return Map.of();
        } catch (Exception e) {
            throw new RuntimeException("Failed to prepare temp publish roots", e);
        }
    }

    @Override
    public void stop() {
        // Best-effort cleanup
        if (base == null) return;
        try {
            Files.walk(base)
                    .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                    });
        } catch (Exception ignored) {
        }
    }
}
