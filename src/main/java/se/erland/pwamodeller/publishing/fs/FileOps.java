package se.erland.pwamodeller.publishing.fs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Objects;
import java.util.UUID;

/**
 * Filesystem helper methods used by publishing operations.
 * Focus: safe path handling + atomic writes.
 */
public final class FileOps {

    private FileOps() {}

    /**
     * Resolve a relative path under a root and prevent path traversal.
     */
    public static Path safeResolveUnderRoot(Path root, String relative) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(relative, "relative");

        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path resolved = normalizedRoot.resolve(relative).normalize();

        if (!resolved.startsWith(normalizedRoot)) {
            throw new IllegalArgumentException("Path traversal detected. Root=" + normalizedRoot + ", relative=" + relative);
        }
        return resolved;
    }

    public static void ensureDir(Path dir) throws IOException {
        Objects.requireNonNull(dir, "dir");
        Files.createDirectories(dir);
    }

    /**
     * Atomically write bytes to a target file by writing to a temp file in the same directory and then moving.
     */
    public static void atomicWrite(Path target, byte[] bytes) throws IOException {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(bytes, "bytes");

        Path dir = target.toAbsolutePath().normalize().getParent();
        if (dir == null) throw new IOException("Target has no parent directory: " + target);
        ensureDir(dir);

        String tmpName = "." + target.getFileName() + ".tmp." + UUID.randomUUID();
        Path tmp = dir.resolve(tmpName);

        Files.write(tmp, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            // Fall back to non-atomic replace, but still same-directory move minimizes risk.
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static void atomicWriteUtf8(Path target, String text) throws IOException {
        atomicWrite(target, text.getBytes(StandardCharsets.UTF_8));
    }
}
