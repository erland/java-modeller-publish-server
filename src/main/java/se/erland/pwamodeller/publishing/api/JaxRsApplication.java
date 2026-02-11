package se.erland.pwamodeller.publishing.api;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * JAX-RS activation for the publishing server.
 */
@ApplicationPath("/api")
public class JaxRsApplication extends Application {
}
