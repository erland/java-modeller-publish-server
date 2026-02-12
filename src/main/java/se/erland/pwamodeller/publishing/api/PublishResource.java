package se.erland.pwamodeller.publishing.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;

import se.erland.pwamodeller.publishing.config.PublishConfig;
import se.erland.pwamodeller.publishing.service.PublishResult;
import se.erland.pwamodeller.publishing.service.PublisherService;
import se.erland.pwamodeller.publishing.service.ValidatedBundle;
import se.erland.pwamodeller.publishing.service.ZipValidator;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Quarkus / RESTEasy Reactive endpoint:
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
     * Multipart form.
     *
     * We bind the ZIP as an InputStream rather than FileUpload to avoid
     * container temp-file / uploads-directory issues.
     *
     * Field names must match the client keys:
     * - bundleZip (file)
     * - title (text, optional)
     */
    public static class PublishForm {
        @RestForm("bundleZip")
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        public InputStream bundleZip;

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

        try (InputStream is = form.bundleZip) {
            PublishConfig effectiveCfg = (cfg != null) ? cfg : PublishConfig.loadFromEnvOrSystem();
            ZipValidator effectiveValidator = (validator != null) ? validator : new ZipValidator(effectiveCfg);
            PublisherService effectivePublisher = (publisher != null) ? publisher : new PublisherService(effectiveCfg);

            ValidatedBundle vb = effectiveValidator.validateToStaging(datasetId, is);
            PublishResult result = effectivePublisher.publish(datasetId, vb, title);

                        // Build response without Map.of(null) pitfalls (Map.of does not allow null values)
            Map<String, Object> urls = new HashMap<>();
            result.latestUrl().ifPresent(u -> urls.put("latest", u.toString()));
            result.manifestUrl().ifPresent(u -> urls.put("manifest", u.toString()));

            Map<String, Object> body = new HashMap<>();
            body.put("datasetId", result.datasetId());
            body.put("bundleId", result.bundleId());
            body.put("publishedAt", result.publishedAt());
            body.put("urls", urls);

            return Response.status(201).entity(body).build();

        } catch (PublishingException pe) {
            throw pe;
        } catch (Exception e) {
            throw new PublishingException(500, "Failed to process publish request", e);
        }
    }
}
