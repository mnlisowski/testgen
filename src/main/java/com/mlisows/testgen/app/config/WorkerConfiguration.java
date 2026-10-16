package com.mlisows.testgen.app.config;

import com.mlisows.testgen.app.job.GenerationJobExecutor;
import com.mlisows.testgen.app.job.UnfinishedJobsRecovery;
import com.mlisows.testgen.app.messaging.JobRequestListener;
import com.mlisows.testgen.infrastructure.process.GenerationProcessMain;
import com.mlisows.testgen.infrastructure.process.JvmGenerationRunner;
import com.mlisows.testgen.usecase.job.ExecuteGenerationJobUseCase;
import com.mlisows.testgen.usecase.ports.GenerationJobStore;
import com.mlisows.testgen.usecase.ports.GenerationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Beans of the worker: it receives job requests from Kafka and runs the generator.
 * Active with the {@code worker} Spring profile.
 */
@Configuration(proxyBeanMethods = false)
@Profile("worker")
public class WorkerConfiguration {
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
    public ExecuteGenerationJobUseCase executeGenerationJob(GenerationJobStore jobStore,
                                                            GenerationRunner generationRunner) {
        return new ExecuteGenerationJobUseCase(jobStore, generationRunner);
    }

    @Bean
    public GenerationJobExecutor generationJobExecutor(ExecuteGenerationJobUseCase executeGenerationJob) {
        return new GenerationJobExecutor(executeGenerationJob);
    }

    @Bean
    public JobRequestListener jobRequestListener(GenerationJobExecutor jobExecutor) {
        return new JobRequestListener(jobExecutor);
    }

    // Only the worker runs jobs, so only the worker knows that after its own restart
    // no job of its previous run can still be running.
    @Bean
    public UnfinishedJobsRecovery unfinishedJobsRecovery(GenerationJobStore jobStore) {
        return new UnfinishedJobsRecovery(jobStore);
    }
}
