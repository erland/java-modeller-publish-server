package se.erland.pwamodeller.publishing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import se.erland.pwamodeller.publishing.api.DatasetInfo;
import se.erland.pwamodeller.publishing.api.DatasetsResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DatasetsResourceTest {

    @TempDir
    Path tmp;

    @SuppressWarnings("unchecked")
    @Test
    void listsDatasetsFromFilesystem() throws Exception {
        Path dataRoot = tmp.resolve("data");
        Path datasetsRoot = dataRoot.resolve("datasets");
        Files.createDirectories(datasetsRoot.resolve("a-dataset"));
        Files.createDirectories(datasetsRoot.resolve("b-dataset"));

        Files.writeString(datasetsRoot.resolve("a-dataset").resolve("dataset.json"),
                "{\"datasetId\":\"a-dataset\",\"title\":\"A\",\"updatedAt\":\"2026-02-11T10:00:00Z\",\"createdAt\":\"2026-02-11T09:00:00Z\"}",
                StandardCharsets.UTF_8);

        Files.writeString(datasetsRoot.resolve("b-dataset").resolve("latest.json"),
                "{\"datasetId\":\"b-dataset\",\"bundleId\":\"bundle-1\",\"publishedAt\":\"2026-02-11T11:00:00Z\",\"manifestUrl\":\"../../bundles/bundle-1/manifest.json\"}",
                StandardCharsets.UTF_8);

        System.setProperty("PUBLISH_DATA_ROOT", dataRoot.toString());
        System.setProperty("PUBLISH_STAGING_ROOT", tmp.resolve("staging").toString());

        DatasetsResource r = new DatasetsResource();
        Map<String, Object> res = r.listDatasets();

        assertTrue(res.containsKey("datasets"));
        List<DatasetInfo> list = (List<DatasetInfo>) res.get("datasets");
        assertEquals(2, list.size());

        boolean hasA = list.stream().anyMatch(d -> "a-dataset".equals(d.datasetId) && "A".equals(d.title));
        boolean hasB = list.stream().anyMatch(d -> "b-dataset".equals(d.datasetId) && "bundle-1".equals(d.latestBundleId));
        assertTrue(hasA);
        assertTrue(hasB);
    }
}
