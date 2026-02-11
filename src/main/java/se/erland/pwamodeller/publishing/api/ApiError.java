package se.erland.pwamodeller.publishing.api;

import java.time.Instant;

public class ApiError {
    public String type;
    public String title;
    public int status;
    public String detail;
    public String requestId;
    public String timestamp;

    public static ApiError of(String title, int status, String detail, String requestId) {
        ApiError e = new ApiError();
        e.type = "about:blank";
        e.title = title;
        e.status = status;
        e.detail = detail;
        e.requestId = requestId;
        e.timestamp = Instant.now().toString();
        return e;
    }
}
