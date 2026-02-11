package se.erland.pwamodeller.publishing.api;

public final class ApiErrors {
    private ApiErrors() {}

    public static class ConflictException extends RuntimeException {
        public ConflictException(String message) { super(message); }
    }

    public static class PayloadTooLargeException extends RuntimeException {
        public PayloadTooLargeException(String message) { super(message); }
    }

    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) { super(message); }
        public ValidationException(String message, Throwable cause) { super(message, cause); }
    }
}
