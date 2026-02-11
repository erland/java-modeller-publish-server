package se.erland.pwamodeller.publishing.api;

import java.time.Instant;

/**
 * Minimal RFC7807-like payload (problem+json).
 */
public class ProblemDetails {
    public String type;
    public String title;
    public int status;
    public String detail;
    public String instance;
    public String requestId;
    public String timestamp;

    public static ProblemDetails of(String title, int status, String detail, String instance, String requestId) {
        ProblemDetails p = new ProblemDetails();
        p.type = "about:blank";
        p.title = title;
        p.status = status;
        p.detail = detail;
        p.instance = instance;
        p.requestId = requestId;
        p.timestamp = Instant.now().toString();
        return p;
    }
}
