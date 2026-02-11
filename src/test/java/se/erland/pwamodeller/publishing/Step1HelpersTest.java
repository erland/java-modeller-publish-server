package se.erland.pwamodeller.publishing;

import org.junit.jupiter.api.Test;
import se.erland.pwamodeller.publishing.policy.DatasetIdPolicy;
import se.erland.pwamodeller.publishing.fs.FileOps;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class Step1HelpersTest {

    @Test
    void datasetIdValidation_acceptsExpected() {
        assertTrue(DatasetIdPolicy.isValid("tullverket-business"));
        assertTrue(DatasetIdPolicy.isValid("samordningsforum_demo"));
        assertTrue(DatasetIdPolicy.isValid("a1"));
        assertTrue(DatasetIdPolicy.isValid("a-1_b"));
    }

    @Test
    void datasetIdValidation_rejectsBad() {
        assertFalse(DatasetIdPolicy.isValid("A-uppercase"));
        assertFalse(DatasetIdPolicy.isValid(" space "));
        assertFalse(DatasetIdPolicy.isValid("../evil"));
        assertFalse(DatasetIdPolicy.isValid("x"));
        assertFalse(DatasetIdPolicy.isValid(""));
        assertFalse(DatasetIdPolicy.isValid(null));
    }

    @Test
    void safeResolve_preventsTraversal() {
        Path root = Path.of("/var/www/ea-portal-data");
        Path ok = FileOps.safeResolveUnderRoot(root, "datasets/tullverket-business/latest.json");
        assertTrue(ok.toString().contains("datasets"));

        assertThrows(IllegalArgumentException.class, () -> FileOps.safeResolveUnderRoot(root, "../etc/passwd"));
        assertThrows(IllegalArgumentException.class, () -> FileOps.safeResolveUnderRoot(root, "datasets/../../etc/passwd"));
    }
}
