package se.erland.pwamodeller.publishing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import se.erland.pwamodeller.publishing.config.PublishConfig;
import se.erland.pwamodeller.publishing.service.PublisherService;
import se.erland.pwamodeller.publishing.service.ValidatedBundle;
import se.erland.pwamodeller.publishing.service.ZipValidator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class PublisherRollbackTest {

    @TempDir
    Path tmp;

    @Test
    void rollsBackBundleIfLatestWriteFails() throws Exception {
        Path dataRoot = tmp.resolve("data");
        Path stagingRoot = tmp.resolve("staging");

        System.setProperty("PUBLISH_DATA_ROOT", dataRoot.toString());
        System.setProperty("PUBLISH_STAGING_ROOT", stagingRoot.toString());
        System.setProperty("PUBLISH_MAX_ZIP_BYTES", String.valueOf(5_000_000));
        System.setProperty("PUBLISH_MAX_JSON_BYTES", String.valueOf(5_000_000));

        String datasetId = "tullverket-business";
        String bundleId = "2026-02-11T10-00-00Z_fail_latest";

        byte[] zip = makeZip(bundleId);

        PublishConfig cfg = PublishConfig.loadFromEnvOrSystem();
        ZipValidator validator = new ZipValidator(cfg);
        ValidatedBundle vb = validator.validateToStaging(datasetId, new ByteArrayInputStream(zip));

        PublisherService publisher = new PublisherService(cfg) {
            @Override
            protected void writeLatest(Path datasetDir, String datasetId, String bundleId, Instant now) throws java.io.IOException {
                throw new java.io.IOException("simulated latest failure");
            }
        };

        assertThrows(RuntimeException.class, () -> publisher.publish(datasetId, vb, Optional.of("Title")));

        // latest.json should not exist
        Path latest = dataRoot.resolve("datasets").resolve(datasetId).resolve("latest.json");
        assertFalse(Files.exists(latest));

        // bundle dir should be rolled back
        Path bundleDir = dataRoot.resolve("bundles").resolve(bundleId);
        assertFalse(Files.exists(bundleDir));
    }

    private static byte[] makeZip(String bundleId) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
                String base = bundleId + "/";
                zos.putNextEntry(new ZipEntry(base));
                zos.closeEntry();

                String manifest = "{\"bundleId\":\"" + bundleId + "\"}";
                put(zos, base + "manifest.json", manifest);
                put(zos, base + "model.json", "{\"m\":1}");
                put(zos, base + "indexes.json", "{\"i\":2}");
            }
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void put(ZipOutputStream zos, String name, String content) throws Exception {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }
}
