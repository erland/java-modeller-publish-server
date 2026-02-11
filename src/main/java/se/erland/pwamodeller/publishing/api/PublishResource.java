package se.erland.pwamodeller.publishing.api;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import jakarta.inject.Inject;

import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import se.erland.pwamodeller.publishing.config.PublishConfig;
import se.erland.pwamodeller.publishing.service.PublishResult;
import se.erland.pwamodeller.publishing.service.PublisherService;
import se.erland.pwamodeller.publishing.service.ValidatedBundle;
import se.erland.pwamodeller.publishing.service.ZipValidator;

import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;

/**
 * Step 2 + Step 3 endpoint:
 * - Upload ZIP (bundleZip)
 * - Validate in staging
 * - Publish under DATA_ROOT and update dataset latest atomically
 */
@Path("/datasets/{datasetId}/publish")
public class PublishResource {

    @Inject
    PublishConfig cfg;

    @Inject
    ZipValidator validator;

    @Inject
    PublisherService publisher;

    /**
     * Quarkus / RESTEasy Reactive multipart form.
     *
     * Field names must match the multipart keys used by the client:
     * - bundleZip (file)
     * - title (text, optional)
     */
    public static class PublishForm {
        @RestForm("bundleZip")
        public FileUpload bundleZip;

        @RestForm("title")
        public String title;
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response publishZip(@PathParam("datasetId") String datasetId, @MultipartForm PublishForm form) {
        if (form == null || form.bundleZip == null) {
            throw PublishingException.validation("Missing multipart field 'bundleZip'");
        }

        Optional<String> title = Optional.ofNullable(form.title)
                .map(String::trim)
                .filter(s -> !s.isEmpty());

        try {
            // RESTEasy Reactive stores uploads on disk; stream from the uploaded file path.
            try (InputStream is = Files.newInputStream(form.bundleZip.uploadedFile())) {

                PublishConfig effectiveCfg = (cfg != null) ? cfg : PublishConfig.loadFromEnvOrSystem();
                ZipValidator effectiveValidator = (validator != null) ? validator : new ZipValidator(effectiveCfg);
                PublisherService effectivePublisher = (publisher != null) ? publisher : new PublisherService(effectiveCfg);

                ValidatedBundle vb = effectiveValidator.validateToStaging(datasetId, is);
                PublishResult result = effectivePublisher.publish(datasetId, vb, title);

                return Response.status(201).entity(Map.of(
                        "datasetId", result.datasetId(),
                        "bundleId", result.bundleId(),
                        "publishedAt", result.publishedAt(),
                        "urls", Map.of(
                                "latest", result.latestUrl().map(Object::toString).orElse(null),
                                "manifest", result.manifestUrl().map(Object::toString).orElse(null)
                        )
                )).build();
            }

        } catch (PublishingException pe) {
            throw pe;
        } catch (Exception e) {
            throw new PublishingException(500, "Failed to read multipart upload", e);
        }
    }
}
