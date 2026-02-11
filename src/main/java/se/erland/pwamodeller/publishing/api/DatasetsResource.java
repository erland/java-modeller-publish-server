package se.erland.pwamodeller.publishing.api;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.ws.rs.GET;
import jakarta.inject.Inject;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import se.erland.pwamodeller.publishing.config.PublishConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@jakarta.ws.rs.Path("/datasets")
public class DatasetsResource {

    @Inject
    PublishConfig cfg;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> listDatasets() {
        PublishConfig effectiveCfg = (cfg != null) ? cfg : PublishConfig.loadFromEnvOrSystem();
        Path datasetsRoot = effectiveCfg.getDataRoot().toAbsolutePath().normalize().resolve("datasets");

        List<DatasetInfo> out = new ArrayList<>();

        if (!Files.isDirectory(datasetsRoot)) {
            return Map.of("datasets", out);
        }

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(datasetsRoot)) {
            for (Path datasetDir : ds) {
                if (!Files.isDirectory(datasetDir)) continue;

                DatasetInfo info = readDatasetInfo(datasetDir);
                if (info != null) out.add(info);
            }
        } catch (IOException e) {
            throw new PublishingException(500, "Failed to list datasets", e);
        }

        // Sort: most recently updated first (fallback datasetId)
        out.sort(Comparator
                .comparing((DatasetInfo d) -> d.updatedAt == null ? "" : d.updatedAt)
                .reversed()
                .thenComparing(d -> d.datasetId));

        return Map.of("datasets", out);
    }

    private static DatasetInfo readDatasetInfo(Path datasetDir) {
        DatasetInfo info = new DatasetInfo();
        info.datasetId = datasetDir.getFileName().toString();

        // dataset.json (title/updatedAt)
        Path datasetJson = datasetDir.resolve("dataset.json");
        if (Files.isRegularFile(datasetJson)) {
            try (var reader = Files.newBufferedReader(datasetJson, StandardCharsets.UTF_8);
                 JsonReader jr = Json.createReader(reader)) {
                JsonObject obj = jr.readObject();
                info.datasetId = obj.getString("datasetId", info.datasetId);
                info.title = obj.getString("title", info.datasetId);
                info.updatedAt = obj.getString("updatedAt", null);
            } catch (Exception ignored) {
                info.title = info.datasetId;
            }
        } else {
            info.title = info.datasetId;
        }

        // latest.json (latest bundle id)
        Path latestJson = datasetDir.resolve("latest.json");
        if (Files.isRegularFile(latestJson)) {
            try (var reader = Files.newBufferedReader(latestJson, StandardCharsets.UTF_8);
                 JsonReader jr = Json.createReader(reader)) {
                JsonObject obj = jr.readObject();
                info.latestBundleId = obj.getString("bundleId", null);
                // if updatedAt missing, use publishedAt
                if (info.updatedAt == null) {
                    info.updatedAt = obj.getString("publishedAt", null);
                }
            } catch (Exception ignored) {}
        }

        // If still no updatedAt, fall back to filesystem mtime
        if (info.updatedAt == null) {
            try {
                info.updatedAt = Files.getLastModifiedTime(datasetDir).toInstant().toString();
            } catch (Exception ignored) {}
        }

        return info;
    }
}