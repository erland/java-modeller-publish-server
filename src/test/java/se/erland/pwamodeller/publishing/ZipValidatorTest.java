package se.erland.pwamodeller.publishing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import se.erland.pwamodeller.publishing.api.PublishingException;
import se.erland.pwamodeller.publishing.config.PublishConfig;
import se.erland.pwamodeller.publishing.service.ValidatedBundle;
import se.erland.pwamodeller.publishing.service.ZipValidator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class ZipValidatorTest {

    @TempDir
    Path tmp;

    @Test
    void validatesHappyPath() {
        Path dataRoot = tmp.resolve("data");
        Path stagingRoot = tmp.resolve("staging");

        System.setProperty("PUBLISH_DATA_ROOT", dataRoot.toString());
        System.setProperty("PUBLISH_STAGING_ROOT", stagingRoot.toString());
        System.setProperty("PUBLISH_MAX_ZIP_BYTES", String.valueOf(5_000_000));
        System.setProperty("PUBLISH_MAX_JSON_BYTES", String.valueOf(5_000_000));

        String bundleId = "bundle-123";
        byte[] zip = makeZip(bundleId);

        ZipValidator v = new ZipValidator(PublishConfig.loadFromEnvOrSystem());
        ValidatedBundle vb = v.validateToStaging("tullverket-business", new ByteArrayInputStream(zip));

        assertEquals(bundleId, vb.bundleId());
        assertTrue(vb.manifestPath().toString().endsWith("manifest.json"));
        assertTrue(vb.modelPath().toString().endsWith("model.json"));
        assertTrue(vb.indexesPath().toString().endsWith("indexes.json"));
    }

    @Test
    void rejectsTraversalEntry() {
        Path dataRoot = tmp.resolve("data");
        Path stagingRoot = tmp.resolve("staging");

        System.setProperty("PUBLISH_DATA_ROOT", dataRoot.toString());
        System.setProperty("PUBLISH_STAGING_ROOT", stagingRoot.toString());
        System.setProperty("PUBLISH_MAX_ZIP_BYTES", String.valueOf(5_000_000));
        System.setProperty("PUBLISH_MAX_JSON_BYTES", String.valueOf(5_000_000));

        byte[] zip = makeZipWithTraversal();
        ZipValidator v = new ZipValidator(PublishConfig.loadFromEnvOrSystem());

        PublishingException ex = assertThrows(PublishingException.class,
                () -> v.validateToStaging("tullverket-business", new ByteArrayInputStream(zip)));
        assertEquals(422, ex.getStatus());
    }

    private static byte[] makeZip(String bundleId) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
                String base = bundleId + "/";
                zos.putNextEntry(new ZipEntry(base));
                zos.closeEntry();
                put(zos, base + "manifest.json", "{\"bundleId\":\"" + bundleId + "\"}");
                put(zos, base + "model.json", "{\"m\":1}");
                put(zos, base + "indexes.json", "{\"i\":2}");
            }
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] makeZipWithTraversal() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
                put(zos, "../evil.txt", "nope");
                put(zos, "x/manifest.json", "{\"bundleId\":\"x\"}");
                put(zos, "x/model.json", "{\"m\":1}");
                put(zos, "x/indexes.json", "{\"i\":2}");
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
