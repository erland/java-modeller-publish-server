package se.erland.pwamodeller.publishing.it;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.QuarkusTestResource;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@QuarkusTestResource(PublishTestResource.class)
public class ProblemJsonIT {

    @Test
    void publishMissingBundleZip_returnsProblemJson() {
        given()
                .multiPart("title", "x")
                .when().post("/datasets/ds/publish")
                .then()
                .statusCode(422)
                .contentType(startsWith("application/problem+json"))
                .body("title", equalTo("Publishing error"))
                .body("status", equalTo(422))
                .body("detail", containsString("bundleZip"))
                .body("timestamp", notNullValue());
    }
}
