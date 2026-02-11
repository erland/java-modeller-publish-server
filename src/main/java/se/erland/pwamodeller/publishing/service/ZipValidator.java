package se.erland.pwamodeller.publishing.service;

import se.erland.pwamodeller.publishing.api.PublishingException;
import se.erland.pwamodeller.publishing.config.PublishConfig;
import se.erland.pwamodeller.publishing.fs.FileOps;
import se.erland.pwamodeller.publishing.policy.DatasetIdPolicy;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Hardening (Step 5): spool incoming ZIP to a staging file first (size-capped),
 * then unzip with traversal protection and validate required JSON files.
 *
 * Does NOT publish to DATA_ROOT.
 */
public class ZipValidator {

    private final PublishConfig config;

    public ZipValidator(PublishConfig config) {
        this.config = config;
    }

    public ValidatedBundle validateToStaging(String datasetId, InputStream zipStream) {
        DatasetIdPolicy.requireValid(datasetId);

        String requestId = UUID.randomUUID().toString();
        Path stagingDir = config.getStagingRoot().toAbsolutePath().normalize().resolve(requestId);
        Path unpackDir = stagingDir.resolve("unpack");
        Path zipFile = stagingDir.resolve("upload.zip");

        try {
            FileOps.ensureDir(unpackDir);
            FileOps.ensureDir(stagingDir);

            // 1) spool upload.zip with maxZipBytes
            spoolZip(zipStream, zipFile, config.getMaxZipBytes());

            // 2) extract upload.zip to unpackDir with traversal protection
            extractZip(zipFile, unpackDir);

            // 3) find and validate bundle structure
            List<Path> manifests = findFilesNamed(unpackDir, "manifest.json");
            if (manifests.isEmpty()) throw PublishingException.validation("ZIP is missing manifest.json");
            if (manifests.size() > 1) throw PublishingException.validation("ZIP contains multiple manifest.json files; expected exactly one");

            Path manifestPath = manifests.get(0);
            String bundleId = readBundleId(manifestPath);

            // Conflict check (bundle already exists)
            Path bundleFinalDir = config.getDataRoot().toAbsolutePath().normalize().resolve("bundles").resolve(bundleId);
            if (Files.exists(bundleFinalDir)) {
                throw PublishingException.conflict("Bundle already exists: " + bundleId);
            }

            Path bundleDir = manifestPath.getParent();
            if (bundleDir == null) throw PublishingException.validation("Invalid manifest path");

            Path modelPath = bundleDir.resolve("model.json");
            Path indexesPath = bundleDir.resolve("indexes.json");

            if (!Files.isRegularFile(modelPath)) throw PublishingException.validation("ZIP is missing model.json next to manifest.json");
            if (!Files.isRegularFile(indexesPath)) throw PublishingException.validation("ZIP is missing indexes.json next to manifest.json");

            // Size + well-formed JSON checks
            JsonWellFormedValidator.validateJsonFile(manifestPath, config.getMaxJsonBytes());
            JsonWellFormedValidator.validateJsonFile(modelPath, config.getMaxJsonBytes());
            JsonWellFormedValidator.validateJsonFile(indexesPath, config.getMaxJsonBytes());

            return new ValidatedBundle(bundleId, bundleDir, manifestPath, modelPath, indexesPath);

        } catch (PublishingException pe) {
            cleanupQuietly(stagingDir);
            throw pe;
        } catch (IllegalArgumentException iae) {
            cleanupQuietly(stagingDir);
            if (iae.getMessage() != null && iae.getMessage().toLowerCase().contains("too large")) {
                throw PublishingException.tooLarge(iae.getMessage());
            }
            throw PublishingException.validation(iae.getMessage());
        } catch (Exception e) {
            cleanupQuietly(stagingDir);
            throw new PublishingException(500, "Internal error during ZIP validation", e);
        }
    }

    public Path getZipFileFromValidatedBundle(ValidatedBundle vb) {
        // staging/<requestId>/upload.zip
        Path bundleDir = vb.bundleDir().toAbsolutePath().normalize();
        Path stagingDir = bundleDir.getParent().getParent(); // unpack/.. -> staging
        return stagingDir.resolve("upload.zip");
    }

    private static void spoolZip(InputStream in, Path zipFile, long maxZipBytes) throws IOException {
        long total = 0;
        try (OutputStream out = Files.newOutputStream(zipFile, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            byte[] buf = new byte[8192];
            int read;
            while ((read = in.read(buf)) > 0) {
                total += read;
                if (total > maxZipBytes) {
                    throw new IllegalArgumentException("ZIP payload too large; max=" + maxZipBytes);
                }
                out.write(buf, 0, read);
            }
        }
    }

    private static void extractZip(Path zipFile, Path unpackDir) throws IOException {
        try (InputStream fis = Files.newInputStream(zipFile);
             ZipInputStream zis = new ZipInputStream(fis, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name == null || name.isBlank()) continue;

                if (name.startsWith("/") || name.startsWith("\\")) {
                    throw new IllegalArgumentException("ZIP contains absolute path entry: " + name);
                }

                Path out = unpackDir.resolve(name).normalize();
                if (!out.startsWith(unpackDir)) {
                    throw new IllegalArgumentException("ZIP traversal entry rejected: " + name);
                }

                if (entry.isDirectory()) {
                    FileOps.ensureDir(out);
                    continue;
                }

                Path parent = out.getParent();
                if (parent != null) FileOps.ensureDir(parent);

                try (var os = Files.newOutputStream(out, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                    byte[] buf = new byte[8192];
                    int read;
                    while ((read = zis.read(buf)) > 0) {
                        os.write(buf, 0, read);
                    }
                }
            }
        }
    }

    private static List<Path> findFilesNamed(Path root, String fileName) throws IOException {
        List<Path> out = new ArrayList<>();
        try (var stream = Files.walk(root)) {
            stream.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().equals(fileName))
                    .forEach(out::add);
        }
        return out;
    }

    private static String readBundleId(Path manifestPath) throws IOException {
        try (var reader = Files.newBufferedReader(manifestPath, StandardCharsets.UTF_8);
             JsonReader jr = Json.createReader(reader)) {
            JsonObject obj = jr.readObject();
            String bundleId = obj.getString("bundleId", "").trim();
            if (bundleId.isEmpty()) {
                throw new IllegalArgumentException("manifest.json is missing bundleId");
            }
            if (bundleId.contains("..") || bundleId.contains("/") || bundleId.contains("\\")) {
                throw new IllegalArgumentException("Invalid bundleId in manifest.json");
            }
            return bundleId;
        }
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
