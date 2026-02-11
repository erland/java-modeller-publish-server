package se.erland.pwamodeller.publishing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import se.erland.pwamodeller.publishing.config.PublishConfig;
import se.erland.pwamodeller.publishing.service.PublisherService;
import se.erland.pwamodeller.publishing.service.ValidatedBundle;
import se.erland.pwamodeller.publishing.service.ZipValidator;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class PublishOperationTest {

    @TempDir
    Path tmp;

    @Test
    void publishesBundleAndWritesLatest() throws Exception {
        Path dataRoot = tmp.resolve("data");
        Path stagingRoot = tmp.resolve("staging");

        System.setProperty("PUBLISH_DATA_ROOT", dataRoot.toString());
        System.setProperty("PUBLISH_STAGING_ROOT", stagingRoot.toString());
        System.setProperty("PUBLISH_MAX_ZIP_BYTES", String.valueOf(5_000_000));
        System.setProperty("PUBLISH_MAX_JSON_BYTES", String.valueOf(5_000_000));

        String datasetId = "tullverket-business";
        String bundleId = "2026-02-11T10-00-00Z_abcd123";

        byte[] zip = makeZip(bundleId);

        PublishConfig cfg = PublishConfig.loadFromEnvOrSystem();
        ZipValidator validator = new ZipValidator(cfg);
        ValidatedBundle vb = validator.validateToStaging(datasetId, new ByteArrayInputStream(zip));

        PublisherService publisher = new PublisherService(cfg);
        var res = publisher.publish(datasetId, vb, java.util.Optional.of("Tullverket – Business"));

        assertEquals(bundleId, res.bundleId());

        Path bundleDir = dataRoot.resolve("bundles").resolve(bundleId);
        assertTrue(Files.isRegularFile(bundleDir.resolve("manifest.json")));
        assertTrue(Files.isRegularFile(bundleDir.resolve("model.json")));
        assertTrue(Files.isRegularFile(bundleDir.resolve("indexes.json")));

        Path latest = dataRoot.resolve("datasets").resolve(datasetId).resolve("latest.json");
        assertTrue(Files.isRegularFile(latest));

        String latestText = Files.readString(latest, StandardCharsets.UTF_8);
        JsonObject obj = Json.createReader(new java.io.StringReader(latestText)).readObject();
        assertEquals(datasetId, obj.getString("datasetId"));
        assertEquals(bundleId, obj.getString("bundleId"));
        assertTrue(obj.getString("manifestUrl").contains(bundleId));

        Path datasetJson = dataRoot.resolve("datasets").resolve(datasetId).resolve("dataset.json");
        assertTrue(Files.isRegularFile(datasetJson));

        String datasetText = Files.readString(datasetJson, StandardCharsets.UTF_8);
        JsonObject d = Json.createReader(new java.io.StringReader(datasetText)).readObject();
        assertEquals(datasetId, d.getString("datasetId"));
        assertEquals("Tullverket – Business", d.getString("title"));
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
