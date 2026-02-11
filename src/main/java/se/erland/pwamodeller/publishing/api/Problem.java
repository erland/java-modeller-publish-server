package se.erland.pwamodeller.publishing.api;

import java.util.Map;

/**
 * Minimal RFC7807-like error payload.
 */
public record Problem(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        Map<String, Object> extra
) {
    public static Problem of(String title, int status, String detail) {
        return new Problem("about:blank", title, status, detail, null, null);
    }

    public Problem withInstance(String instance) {
        return new Problem(type, title, status, detail, instance, extra);
    }

    public Problem withExtra(Map<String, Object> extra) {
        return new Problem(type, title, status, detail, instance, extra);
    }
}
