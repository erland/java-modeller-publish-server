package se.erland.pwamodeller.publishing.it;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

final class TestZips {
    private TestZips() {}

    /**
     * Builds the minimal valid publishing bundle ZIP expected by ZipValidator.
     *
     * Required files:
     * - manifest.json (contains bundleId)
     * - model.json
     * - indexes.json
     */
    static byte[] minimalValidBundle(String bundleId) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
                put(zos, "manifest.json", "{\"bundleId\":\"" + escape(bundleId) + "\"}\n");
                put(zos, "model.json", "{\"schemaVersion\":1,\"nodes\":[],\"edges\":[]}\n");
                put(zos, "indexes.json", "{\"byId\":{}}\n");
            }
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void put(ZipOutputStream zos, String name, String content) throws Exception {
        ZipEntry e = new ZipEntry(name);
        zos.putNextEntry(e);
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
