package se.erland.pwamodeller.publishing.service;

import java.nio.file.Path;

/** Result of ZIP validation (staging only). */
public record ValidatedBundle(
        String bundleId,
        Path bundleDir,
        Path manifestPath,
        Path modelPath,
        Path indexesPath
) {}
