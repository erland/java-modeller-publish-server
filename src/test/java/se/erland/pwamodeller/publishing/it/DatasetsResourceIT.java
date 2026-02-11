package se.erland.pwamodeller.publishing.it;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.QuarkusTestResource;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@QuarkusTestResource(PublishTestResource.class)
public class DatasetsResourceIT {

    @Test
    void listDatasets_emptyInitially() {
        given()
                .when().get("/datasets")
                .then()
                .statusCode(200)
                .body("datasets", is(empty()));
    }
}
