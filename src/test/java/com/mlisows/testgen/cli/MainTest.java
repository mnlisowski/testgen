package com.mlisows.testgen.cli;

import com.mlisows.testgen.domain.ArgumentValueHint;
import com.mlisows.testgen.domain.ObservedProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldPrintGenerationReportForMavenProject() throws Exception {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));

            Main.main(new String[]{
                    "src/test/resources/sample-maven-project",
                    tempDir.resolve("work").toString()
            });
        } finally {
            System.setOut(originalOut);
        }

        String report = output.toString(StandardCharsets.UTF_8);

        assertTrue(report.contains("Test generation report"));
        assertTrue(report.contains("Classes analyzed: 4"));
        assertTrue(report.contains("Supported branch kinds: IF, FOR, WHILE, SWITCH"));
        assertTrue(report.contains("Methods analyzed: 8"));
        assertTrue(report.contains("Detected goals: 15"));
        assertTrue(report.contains("Covered goals: 12"));
        assertTrue(report.contains("Failed to execute: 0"));
        assertTrue(report.contains("Unsupported Java constructs"));
        assertTrue(report.contains("Generation report written to: " + tempDir.resolve("work/report.txt")));
        assertTrue(report.contains("Generated tests written to: " + tempDir.resolve("work/generated-tests")));

        String writtenReport = Files.readString(tempDir.resolve("work/report.txt"));

        assertTrue(writtenReport.contains("Test generation report"));
        assertTrue(writtenReport.contains("Covered goals: 12"));

        Path generatedTestsRoot = tempDir.resolve("work/generated-tests");
        List<Path> generatedTests;
        try (Stream<Path> paths = Files.walk(generatedTestsRoot)) {
            generatedTests = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith("Test.java"))
                    .toList();
        }

        assertTrue(!generatedTests.isEmpty());

        String generatedCode = Files.readString(generatedTests.get(0));

        assertTrue(generatedCode.contains("import org.junit.jupiter.api.Test;"));
        assertTrue(generatedCode.contains("@Test"));
    }

    @Test
    void shouldUseObservedProfileHints() {
        ObservedProfile observedProfile = new ObservedProfile(
                List.of(new ArgumentValueHint(
                        "sample.maven.Order.<init>",
                        "status",
                        "String",
                        "\"PAID\"",
                        "runtime-observed"
                )),
                List.of()
        );

        String report = new GenerateTestsCommand().generateReport(
                Path.of("src/test/resources/sample-maven-project"),
                tempDir.resolve("work-with-profile"),
                observedProfile
        ).toText();

        assertTrue(report.contains("Covered goals: 13"));
    }

    @Test
    void shouldPrepareObservedProfileWorkspaceWithoutRunningMain() throws Exception {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Path workRoot = tempDir.resolve("prepared-profile-work");

        try {
            System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));

            Main.main(new String[]{
                    "prepare-profile",
                    "src/test/resources/sample-maven-project",
                    workRoot.toString()
            });
        } finally {
            System.setOut(originalOut);
        }

        String text = output.toString(StandardCharsets.UTF_8);

        assertTrue(Files.exists(workRoot.resolve("instrumented-src/sample/maven/DemoApplication.java")));
        assertTrue(Files.exists(workRoot.resolve("classes/sample/maven/DemoApplication.class")));
        assertTrue(text.contains("Instrumented sources written to: " + workRoot.resolve("instrumented-src")));
        assertTrue(text.contains("Instrumented classes written to: " + workRoot.resolve("classes")));
        assertTrue(text.contains("-Dtestgen.profile.dir=" + workRoot.resolve("profile-output")));
        assertTrue(text.contains("Observed profile will be written to: " + workRoot.resolve("profile-output/observed-profile.txt")));
    }

}
