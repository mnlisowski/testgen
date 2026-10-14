package com.mlisows.testgen.app.config;

import com.mlisows.testgen.app.project.ProjectRegistry;
import com.mlisows.testgen.infrastructure.job.InMemoryGenerationJobStore;
import com.mlisows.testgen.usecase.ports.GenerationJobStore;
import java.time.Clock;
import com.mlisows.testgen.domain.generation.GenerationSettings;
import com.mlisows.testgen.infrastructure.project.MavenProjectLayoutDetector;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
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
    public GenerationJobStore generationJobStore() {
        return new InMemoryGenerationJobStore(Clock.systemUTC());
    }

}
