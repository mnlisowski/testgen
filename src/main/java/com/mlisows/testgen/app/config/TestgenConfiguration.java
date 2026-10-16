package com.mlisows.testgen.app.config;

import com.mlisows.testgen.app.project.ProjectRegistry;
import com.mlisows.testgen.app.job.GenerationJobQueue;
import com.mlisows.testgen.infrastructure.process.GenerationProcessMain;
import com.mlisows.testgen.domain.generation.GenerationSettings;
import com.mlisows.testgen.infrastructure.process.JvmGenerationRunner;
import com.mlisows.testgen.infrastructure.project.MavenProjectLayoutDetector;
import com.mlisows.testgen.usecase.job.ExecuteGenerationJobUseCase;
import com.mlisows.testgen.usecase.ports.GenerationJobStore;
import com.mlisows.testgen.usecase.ports.GenerationRunner;
import com.mlisows.testgen.infrastructure.runtime.TextObservedProfileReader;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TestgenProperties.class)
public class TestgenConfiguration {
    @Bean
    public ProjectRegistry projectRegistry(TestgenProperties properties) {
        Path workRoot = properties.workRootPath();
        if (Files.exists(workRoot) && !Files.isDirectory(workRoot)) {
            throw new IllegalArgumentException("testgen.work-root is not a directory: " + workRoot);
        }

        var detector = new MavenProjectLayoutDetector();
        Map<String, Path> projects = new LinkedHashMap<>();
        properties.projects().forEach((id, project) -> {
            try {
                Path root = Path.of(project.path()).toAbsolutePath().normalize();
                detector.detect(root);
                projects.put(id, root);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Invalid testgen.projects." + id + ".path: "
                        + exception.getMessage(), exception);
            }
        });
        return new ProjectRegistry(projects);
    }

    @Bean
    public GenerationSettings generationDefaults(TestgenProperties properties) {
        return properties.defaults().toGenerationSettings();
    }

    @Bean
    public TextObservedProfileReader observedProfileReader() {
        return new TextObservedProfileReader();
    }

    @Bean
    public GenerationRunner generationRunner(TestgenProperties properties) {
        String runnerClasspath = resolveRunnerClasspath(properties.runner().classpath());
        String executableName = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        Path javaExecutable = Path.of(System.getProperty("java.home"), "bin", executableName);
        Duration executionTimeout = Duration.ofSeconds(properties.runner().timeoutSeconds());

        return new JvmGenerationRunner(javaExecutable, runnerClasspath, executionTimeout);
    }

    private String resolveRunnerClasspath(String configuredClasspath) {
        if (configuredClasspath != null && !configuredClasspath.isBlank()) {
            return configuredClasspath;
        }

        URL runnerLocation = GenerationProcessMain.class.getProtectionDomain().getCodeSource().getLocation();
        if (!runnerLocation.getProtocol().equals("file")) {
            throw new IllegalArgumentException(
                    "Set testgen.runner.classpath to the ordinary testgen JAR and its dependencies "
                            + "when starting from a Spring Boot executable JAR");
        }
        return System.getProperty("java.class.path");
    }

    @Bean
    public ExecuteGenerationJobUseCase executeGenerationJob(GenerationJobStore jobStore, GenerationRunner generationRunner) {
        return new ExecuteGenerationJobUseCase(jobStore, generationRunner);
    }

    @Bean(destroyMethod = "close")
    public GenerationJobQueue generationJobQueue(GenerationJobStore jobStore,
                                                 ExecuteGenerationJobUseCase executeGenerationJob,
                                                 TestgenProperties properties) {
        return new GenerationJobQueue(jobStore, executeGenerationJob, properties.queueCapacity());
    }

}
