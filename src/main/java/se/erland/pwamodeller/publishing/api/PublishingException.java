package se.erland.pwamodeller.publishing.api;

public class PublishingException extends RuntimeException {
    private final int status;

    public PublishingException(int status, String message) {
        super(message);
        this.status = status;
    }

    public PublishingException(int status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public int getStatus() { return status; }

    public static PublishingException validation(String message) {
        return new PublishingException(422, message);
    }

    public static PublishingException conflict(String message) {
        return new PublishingException(409, message);
    }

    public static PublishingException tooLarge(String message) {
        return new PublishingException(413, message);
    }
}
