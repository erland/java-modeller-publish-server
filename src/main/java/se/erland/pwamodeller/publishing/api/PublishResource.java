package se.erland.pwamodeller.publishing.api;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import se.erland.pwamodeller.publishing.config.PublishConfig;
import se.erland.pwamodeller.publishing.service.PublishResult;
import se.erland.pwamodeller.publishing.service.PublisherService;
import se.erland.pwamodeller.publishing.service.ValidatedBundle;
import se.erland.pwamodeller.publishing.service.ZipValidator;

import java.io.InputStream;
import java.util.List;
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

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response publishZip(@PathParam("datasetId") String datasetId, MultipartFormDataInput input) {
        Map<String, List<InputPart>> parts = input.getFormDataMap();
        List<InputPart> zipParts = parts.get("bundleZip");

        if (zipParts == null || zipParts.isEmpty()) {
            throw PublishingException.validation("Missing multipart field 'bundleZip'");
        }
        if (zipParts.size() > 1) {
            throw PublishingException.validation("Multiple 'bundleZip' parts provided; expected 1");
        }

        Optional<String> title = readOptionalText(parts, "title");

        try {
            InputStream is = zipParts.get(0).getBody(InputStream.class, null);

            PublishConfig cfg = PublishConfig.loadFromEnvOrSystem();
            ZipValidator validator = new ZipValidator(cfg);
            ValidatedBundle vb = validator.validateToStaging(datasetId, is);

            PublisherService publisher = new PublisherService(cfg);
            PublishResult result = publisher.publish(datasetId, vb, title);

            return Response.status(201).entity(Map.of(
                    "datasetId", result.datasetId(),
                    "bundleId", result.bundleId(),
                    "publishedAt", result.publishedAt(),
                    "urls", Map.of(
                            "latest", result.latestUrl().map(Object::toString).orElse(null),
                            "manifest", result.manifestUrl().map(Object::toString).orElse(null)
                    )
            )).build();

        } catch (PublishingException pe) {
            throw pe;
        } catch (Exception e) {
            throw new PublishingException(500, "Failed to read multipart upload", e);
        }
    }

    private static Optional<String> readOptionalText(Map<String, List<InputPart>> parts, String field) {
        try {
            List<InputPart> ps = parts.get(field);
            if (ps == null || ps.isEmpty()) return Optional.empty();
            String v = ps.get(0).getBodyAsString();
            if (v == null) return Optional.empty();
            String t = v.trim();
            return t.isEmpty() ? Optional.empty() : Optional.of(t);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
