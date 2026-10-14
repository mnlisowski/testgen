package com.mlisows.testgen.infrastructure;

import com.mlisows.testgen.domain.generation.GenerationSettings;
import com.mlisows.testgen.usecase.generation.GenerationRequest;
import com.mlisows.testgen.usecase.profile.ProfilePreparationRequest;

import com.mlisows.testgen.domain.generation.ArgumentValueHint;
import com.mlisows.testgen.domain.profile.ObservedProfile;
import com.mlisows.testgen.infrastructure.runtime.TextObservedProfileReader;
import com.mlisows.testgen.infrastructure.writer.GeneratedCandidateTestFileWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class TestgenUseCasesTest {
    private static final Path PROJECT = Path.of("src/test/resources/sample-maven-project");
    private static final ObservedProfile EMPTY_PROFILE = new ObservedProfile(List.of(), List.of());

    @TempDir
    Path tempDir;

    @Test
    void shouldReturnRepeatableResultsAndLeaveOutputPersistenceToCaller() throws Exception {
        var generator = TestgenUseCases.generation();
        var settings = new GenerationSettings(42, 50, 10, 3, 10);
        Path workRoot = tempDir.resolve("first");
        var first = generator.generate(new GenerationRequest(PROJECT, workRoot, settings, EMPTY_PROFILE));
        generator.generate(new GenerationRequest(PROJECT, tempDir.resolve("other-seed"),
                new GenerationSettings(7, 20, 5, 1, 5), EMPTY_PROFILE));
        var repeated = generator.generate(new GenerationRequest(PROJECT, tempDir.resolve("repeat"), settings, EMPTY_PROFILE));

        assertFalse(first.tests().isEmpty());
        assertFalse(first.coveredGoalIds().isEmpty());
        assertEquals(first.tests(), repeated.tests());
        assertEquals(first.coveredGoalIds(), repeated.coveredGoalIds());
        assertEquals(first.reportText(), repeated.reportText());
        assertFalse(Files.exists(workRoot.resolve("generated-tests")));
        assertFalse(Files.exists(workRoot.resolve("report.txt")));
        assertFalse(Files.exists(workRoot.resolve("covered-goals.json")));

        Path outputRoot = tempDir.resolve("export");
        new GeneratedCandidateTestFileWriter().writeFiles(outputRoot, first.tests());
        for (var file : first.tests()) {
            assertEquals(file.content(), Files.readString(outputRoot.resolve(file.relativePath())));
        }
    }

    @Test
    void shouldUseInMemoryProfileWithoutRequiringAProfileFile() throws Exception {
        Path project = tempDir.resolve("project");
        Path source = project.resolve("src/main/java/sample/Secret.java");
        Files.createDirectories(source.getParent());
        Files.writeString(project.resolve("pom.xml"), """
                <project><modelVersion>4.0.0</modelVersion><groupId>sample</groupId>
                <artifactId>secret</artifactId><version>1</version></project>
                """);
        Files.writeString(source, """
                package sample;
                public class Secret {
                    public int check(int value) {
                        if (value == Integer.parseInt("1234567")) {
                            return 1;
                        }
                        return 0;
                    }
                }
                """);
        var profile = new ObservedProfile(List.of(new ArgumentValueHint(
                "sample.Secret.check(int)", "value", "int", "1234567", "runtime-observed")), List.of());
        var generator = TestgenUseCases.generation();
        var settings = GenerationSettings.defaults();
        var baseline = generator.generate(new GenerationRequest(project, tempDir.resolve("without"), settings, EMPTY_PROFILE));
        var observed = generator.generate(new GenerationRequest(project, tempDir.resolve("with"), settings, profile));

        assertTrue(observed.coveredGoalIds().size() > baseline.coveredGoalIds().size());
        assertTrue(observed.tests().stream().anyMatch(file -> file.content().contains("1234567")));
    }

    @Test
    void shouldPrepareRunnableProfileWorkspaceAndRecordArguments() throws Exception {
        Path workRoot = tempDir.resolve("profile");
        var result = TestgenUseCases.profilePreparation().prepare(new ProfilePreparationRequest(PROJECT, workRoot));
        assertTrue(Files.readString(result.sourceRoot().resolve("sample/maven/DiscountService.java"))
                .contains("ArgumentRecorder.record"));
        assertTrue(Files.exists(result.classesRoot().resolve("sample/maven/messages.txt")));
        assertFalse(Files.exists(result.profilePath()));

        Path log = tempDir.resolve("application.log");
        Process process = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Dtestgen.profile.dir=" + workRoot, "-cp", result.runtimeClasspath(), "sample.maven.DemoApplication")
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Instrumented application timed out");
            assertEquals(0, process.exitValue(), () -> readLog(log));
        } finally {
            if (process.isAlive()) process.destroyForcibly().waitFor();
        }
        var profile = new TextObservedProfileReader().read(result.profilePath());
        assertFalse(profile.getArgumentValueHints().isEmpty());
        assertFalse(profile.getObservedInvocations().isEmpty());
    }

    private static String readLog(Path log) {
        try {
            return Files.readString(log);
        } catch (java.io.IOException exception) {
            return exception.toString();
        }
    }
}
