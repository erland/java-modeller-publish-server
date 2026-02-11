package se.erland.pwamodeller.publishing.it;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.QuarkusTestResource;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@QuarkusTestResource(PublishTestResource.class)
public class PublishResourceIT {

    @Test
    void publishMultipart_happyPath() {
        byte[] zip = TestZips.minimalValidBundle("bundle-001");

        given()
                .multiPart("bundleZip", "bundle.zip", zip, "application/zip")
                .multiPart("title", "My dataset")
                .when().post("/datasets/test-ds/publish")
                .then()
                .statusCode(201)
                .body("datasetId", equalTo("test-ds"))
                .body("bundleId", equalTo("bundle-001"))
                .body("publishedAt", notNullValue())
                .body("urls.latest", notNullValue())
                .body("urls.manifest", notNullValue());

        // After publishing, the dataset should show up in the list endpoint.
        given()
                .when().get("/datasets")
                .then()
                .statusCode(200)
                .body("datasets.size()", is(1))
                .body("datasets[0].datasetId", equalTo("test-ds"))
                .body("datasets[0].latestBundleId", equalTo("bundle-001"));
    }
}
