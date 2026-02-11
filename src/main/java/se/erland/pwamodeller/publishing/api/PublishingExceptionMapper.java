package se.erland.pwamodeller.publishing.api;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.container.ContainerRequestContext;

import java.util.logging.Logger;

@Provider
public class PublishingExceptionMapper implements ExceptionMapper<PublishingException> {

    private static final Logger LOG = Logger.getLogger(PublishingExceptionMapper.class.getName());

    @Context
    ContainerRequestContext requestContext;

    @Override
    public Response toResponse(PublishingException exception) {
        String requestId = requestId();
        String path = requestPath();
        LOG.warning(() -> "requestId=" + requestId + " status=" + exception.getStatus() + " path=" + path + " msg=" + exception.getMessage());

        ProblemDetails pd = ProblemDetails.of(
                "Publishing error",
                exception.getStatus(),
                exception.getMessage(),
                path,
                requestId
        );

        return Response.status(exception.getStatus())
                .type("application/problem+json")
                .entity(pd)
                .build();
    }

    private String requestId() {
        if (requestContext == null) return null;
        Object rid = requestContext.getProperty(RequestIdFilter.CTX_KEY);
        return rid == null ? null : rid.toString();
    }

    private String requestPath() {
        if (requestContext == null) return null;
        return requestContext.getUriInfo().getRequestUri().toString();
    }
}
