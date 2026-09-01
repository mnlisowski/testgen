package com.mlisows.testgen.infrastructure.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MavenProjectLayoutDetectorTest {

    @TempDir
    Path tempDir;

    private final MavenProjectLayoutDetector detector = new MavenProjectLayoutDetector();

    @Test
    void shouldDetectMavenProjectLayout() throws Exception {
        Files.writeString(tempDir.resolve("pom.xml"), "<project></project>");
        Files.createDirectories(tempDir.resolve("src/main/java"));
        Files.createDirectories(tempDir.resolve("src/main/resources"));

        MavenProjectLayout layout = detector.detect(tempDir);

        assertEquals(tempDir, layout.getProjectRoot());
        assertEquals(tempDir.resolve("pom.xml"), layout.getPomPath());
        assertEquals(tempDir.resolve("src/main/java"), layout.getMainSourceRoot());
        assertEquals(tempDir.resolve("src/main/resources"), layout.getMainResourceRoot().orElseThrow());
    }

    @Test
    void shouldAllowMavenProjectWithoutMainResources() throws Exception {
        Files.writeString(tempDir.resolve("pom.xml"), "<project></project>");
        Files.createDirectories(tempDir.resolve("src/main/java"));

        MavenProjectLayout layout = detector.detect(tempDir);

        assertTrue(layout.getMainResourceRoot().isEmpty());
    }

    @Test
    void shouldRejectDirectoryWithoutPom() throws Exception {
        Files.createDirectories(tempDir.resolve("src/main/java"));

        assertThrows(IllegalArgumentException.class, () -> detector.detect(tempDir));
    }

    @Test
    void shouldRejectDirectoryWithoutMainSources() throws Exception {
        Files.writeString(tempDir.resolve("pom.xml"), "<project></project>");

        assertThrows(IllegalArgumentException.class, () -> detector.detect(tempDir));
    }
}
