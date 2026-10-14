package com.mlisows.testgen.app.job;

import com.mlisows.testgen.usecase.generation.GeneratedTestFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GeneratedTestsArchiveTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void shouldPreservePackagesAndUtf8Content() throws Exception {
        String content = "package sample; // Zażółć gęślą jaźń\nclass ExampleTest {}\n";
        List<GeneratedTestFile> tests = List.of(
                new GeneratedTestFile(Path.of("sample/ExampleTest.java"), content),
                new GeneratedTestFile(Path.of("sample/nested/OtherTest.java"), "class OtherTest {}")
        );

        byte[] archive = new GeneratedTestsArchive().create(tests);
        Path archivePath = temporaryDirectory.resolve("tests.zip");
        Files.write(archivePath, archive);

        try (ZipFile zip = new ZipFile(archivePath.toFile(), StandardCharsets.UTF_8)) {
            assertEquals(2, zip.size());
            assertNotNull(zip.getEntry("sample/nested/OtherTest.java"));
            try (var input = zip.getInputStream(zip.getEntry("sample/ExampleTest.java"))) {
                assertEquals(content, new String(input.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
    }

    @Test
    void shouldCreateValidEmptyArchive() throws Exception {
        byte[] archive = new GeneratedTestsArchive().create(List.of());
        Path archivePath = temporaryDirectory.resolve("empty.zip");
        Files.write(archivePath, archive);

        try (ZipFile zip = new ZipFile(archivePath.toFile())) {
            assertEquals(0, zip.size());
        }
    }
}
