package se.erland.pwamodeller.publishing.config;

import jakarta.enterprise.context.ApplicationScoped;

import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Reads publishing server configuration from system properties or environment variables.
 * Precedence: System property wins over env var.
 */
@ApplicationScoped
public class PublishConfig {
    public static final long DEFAULT_MAX_ZIP_BYTES = 100L * 1024L * 1024L;   // 100 MB
    public static final long DEFAULT_MAX_JSON_BYTES = 50L * 1024L * 1024L;   // 50 MB

    private final Path dataRoot;
    private final Path stagingRoot;
    private final Optional<Path> archiveRoot;
    private final long maxZipBytes;
    private final long maxJsonBytes;
    private final Optional<URI> baseUrl;
    /**
     * Quarkus CDI constructor.
     * Keeps the existing env/system-property behavior (Option A in the plan).
     */
    public PublishConfig() {
        PublishConfig cfg = loadFromEnvOrSystem();
        this.dataRoot = cfg.dataRoot;
        this.stagingRoot = cfg.stagingRoot;
        this.archiveRoot = cfg.archiveRoot;
        this.maxZipBytes = cfg.maxZipBytes;
        this.maxJsonBytes = cfg.maxJsonBytes;
        this.baseUrl = cfg.baseUrl;
    }



    // Visible for tests and for future DI/wiring.
    public PublishConfig(
            Path dataRoot,
            Path stagingRoot,
            Optional<Path> archiveRoot,
            long maxZipBytes,
            long maxJsonBytes,
            Optional<URI> baseUrl
    ) {
        this.dataRoot = dataRoot;
        this.stagingRoot = stagingRoot;
        this.archiveRoot = archiveRoot;
        this.maxZipBytes = maxZipBytes;
        this.maxJsonBytes = maxJsonBytes;
        this.baseUrl = baseUrl;
    }

    public Path getDataRoot() { return dataRoot; }
    public Path getStagingRoot() { return stagingRoot; }
    public Optional<Path> getArchiveRoot() { return archiveRoot; }
    public long getMaxZipBytes() { return maxZipBytes; }
    public long getMaxJsonBytes() { return maxJsonBytes; }
    public Optional<URI> getBaseUrl() { return baseUrl; }

    public static PublishConfig loadFromEnvOrSystem() {
        Path dataRoot = Path.of(required("PUBLISH_DATA_ROOT"));
        Path stagingRoot = Path.of(required("PUBLISH_STAGING_ROOT"));

        long maxZip = parseLong("PUBLISH_MAX_ZIP_BYTES", DEFAULT_MAX_ZIP_BYTES);
        long maxJson = parseLong("PUBLISH_MAX_JSON_BYTES", DEFAULT_MAX_JSON_BYTES);

        Optional<URI> base = optional("PUBLISH_BASE_URL").map(URI::create);
        Optional<Path> archive = optional("PUBLISH_ARCHIVE_ROOT").map(Path::of);

        if (maxZip <= 0) throw new IllegalArgumentException("PUBLISH_MAX_ZIP_BYTES must be > 0");
        if (maxJson <= 0) throw new IllegalArgumentException("PUBLISH_MAX_JSON_BYTES must be > 0");

        return new PublishConfig(dataRoot, stagingRoot, archive, maxZip, maxJson, base);
    }

    private static String required(String key) {
        return optional(key).orElseThrow(() -> new IllegalStateException(
                "Missing required configuration: " + key + " (set system property or env var)"
        ));
    }

    private static Optional<String> optional(String key) {
        String sys = System.getProperty(key);
        if (sys != null && !sys.isBlank()) return Optional.of(sys.trim());
        String env = System.getenv(key);
        if (env != null && !env.isBlank()) return Optional.of(env.trim());
        return Optional.empty();
    }

    private static long parseLong(String key, long defaultValue) {
        return optional(key)
                .map(v -> {
                    try {
                        return Long.parseLong(v);
                    } catch (NumberFormatException nfe) {
                        throw new IllegalArgumentException("Invalid long for " + key + ": " + v, nfe);
                    }
                })
                .orElse(defaultValue);
    }
}
