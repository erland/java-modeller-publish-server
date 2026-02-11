package se.erland.pwamodeller.publishing.api;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.container.ContainerRequestContext;

import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GenericExceptionMapper.class.getName());

    @Context
    ContainerRequestContext requestContext;

    @Override
    public Response toResponse(Throwable exception) {
        String requestId = requestId();
        String path = requestPath();
        LOG.log(Level.SEVERE, "Unhandled error requestId=" + requestId + " path=" + path, exception);

        ProblemDetails pd = ProblemDetails.of(
                "Internal Server Error",
                500,
                "Unexpected server error",
                path,
                requestId
        );
        return Response.status(500)
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
