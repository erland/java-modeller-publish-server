package se.erland.pwamodeller.publishing.service;

import java.net.URI;
import java.util.Optional;

public record PublishResult(
        String datasetId,
        String bundleId,
        String publishedAt,
        Optional<URI> latestUrl,
        Optional<URI> manifestUrl
) {}
