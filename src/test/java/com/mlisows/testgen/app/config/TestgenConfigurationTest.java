package com.mlisows.testgen.app.config;

import com.mlisows.testgen.app.project.ProjectRegistry;
import com.mlisows.testgen.domain.generation.GenerationSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TestgenConfigurationTest {
    @TempDir
    Path tempDir;

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(TestgenConfiguration.class)
            .withPropertyValues(
                    "testgen.projects.sample.path=src/test/resources/sample-maven-project",
                    "testgen.work-root=target/config-test-work",
                    "testgen.defaults.seed=42",
                    "testgen.defaults.max-candidates=80",
                    "testgen.defaults.initial-candidates=0",
                    "testgen.defaults.mutated-initial-candidates=0",
                    "testgen.defaults.max-values-per-slot=7");

    @Test
    void shouldBindSettingsAndResolveOnlyConfiguredProjects() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(GenerationSettings.class))
                    .isEqualTo(new GenerationSettings(42, 80, 0, 0, 7));
            var registry = context.getBean(ProjectRegistry.class);
            assertThat(registry.projectIds()).containsExactly("sample");
            assertThat(registry.findProjectRoot("sample")).contains(
                    Path.of("src/test/resources/sample-maven-project").toAbsolutePath().normalize());
            assertThat(registry.findProjectRoot("../other-project")).isEmpty();
            assertThat(context.getBean(TestgenProperties.class).workRootPath())
                    .isEqualTo(Path.of("target/config-test-work").toAbsolutePath().normalize());
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "testgen.defaults.max-candidates=0",
            "testgen.runner.timeout-seconds=0",
            "testgen.queue-capacity=0",
            "testgen.defaults.initial-candidates=-1",
            "testgen.defaults.mutated-initial-candidates=-1",
            "testgen.defaults.max-values-per-slot=0",
            "testgen.defaults.seed=not-a-number",
            "testgen.projects.sample.path=",
            "testgen.work-root=",
            "testgen.defaults.max-candidate=100"
    })
    void shouldRejectInvalidOrMisspelledConfiguration(String property) {
        runner.withPropertyValues(property).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasStackTraceContaining("testgen");
        });
    }

    @Test
    void shouldRejectMissingConfiguration() {
        new ApplicationContextRunner().withUserConfiguration(TestgenConfiguration.class)
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void shouldRejectProjectWithoutMavenLayout() {
        runner.withPropertyValues("testgen.projects.sample.path=" + tempDir)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining("Invalid testgen.projects.sample.path")
                            .hasStackTraceContaining("pom.xml not found");
                });
    }

    @Test
    void shouldRejectWorkRootPointingToAFile() throws Exception {
        Path file = Files.createFile(tempDir.resolve("file"));
        runner.withPropertyValues("testgen.work-root=" + file)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining("testgen.work-root is not a directory");
                });
    }

    @Test
    void shouldNotCreateWorkDirectoryDuringStartup() {
        Path workRoot = tempDir.resolve("future-jobs");
        runner.withPropertyValues("testgen.work-root=" + workRoot)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(workRoot).doesNotExist();
                });
    }
}
