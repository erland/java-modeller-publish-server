package se.erland.pwamodeller.publishing.api;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import static se.erland.pwamodeller.publishing.api.ApiErrors.*;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable ex) {
        if (ex instanceof PayloadTooLargeException) {
            return build(413, "Payload Too Large", ex.getMessage());
        }
        if (ex instanceof ConflictException) {
            return build(409, "Conflict", ex.getMessage());
        }
        if (ex instanceof ValidationException || ex instanceof IllegalArgumentException) {
            return build(422, "Unprocessable Entity", ex.getMessage());
        }
        // default
        return build(500, "Internal Server Error", ex.getMessage() == null ? "Unexpected error" : ex.getMessage());
    }

    private static Response build(int status, String title, String detail) {
        Problem p = Problem.of(title, status, detail);
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(p)
                .build();
    }
}
