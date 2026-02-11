package se.erland.pwamodeller.publishing.it;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.QuarkusTestResource;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@QuarkusTestResource(PublishTestResource.class)
public class HealthResourceIT {

    @Test
    void health_ok() {
        given()
                .when().get("/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("ok"))
                .body("time", notNullValue());
    }
}
