package se.erland.pwamodeller.publishing.policy;

import java.util.regex.Pattern;

/**
 * Strict datasetId validation for filesystem path safety and stable URLs.
 *
 * Default rule: lowercase letters, digits, dash, underscore.
 * Must start with [a-z0-9], length 2..64 (tune as needed).
 */
public final class DatasetIdPolicy {
    private static final Pattern PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-_]{1,63}$");

    private DatasetIdPolicy() {}

    public static boolean isValid(String datasetId) {
        return datasetId != null && PATTERN.matcher(datasetId).matches();
    }

    public static String requireValid(String datasetId) {
        if (!isValid(datasetId)) {
            throw new IllegalArgumentException("Invalid datasetId: '" + datasetId + "'. Expected pattern: " + PATTERN);
        }
        return datasetId;
    }
}
