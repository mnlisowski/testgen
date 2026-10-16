package com.mlisows.testgen.infrastructure.process;

import com.mlisows.testgen.domain.generation.ArgumentValueHint;
import com.mlisows.testgen.domain.generation.GenerationSettings;
import com.mlisows.testgen.domain.profile.ObservedProfile;
import com.mlisows.testgen.infrastructure.TestgenUseCases;
import com.mlisows.testgen.infrastructure.job.InMemoryGenerationJobStore;
import com.mlisows.testgen.usecase.generation.GenerationRequest;
import com.mlisows.testgen.usecase.generation.GenerationResult;
import com.mlisows.testgen.usecase.job.GenerationJob;
import com.mlisows.testgen.usecase.job.ExecuteGenerationJobUseCase;
import com.mlisows.testgen.usecase.job.JobStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JvmGenerationRunnerTest {
    @TempDir
    Path temporaryDirectory;

    private JvmGenerationRunner createRunner(Duration timeout) {
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        return new JvmGenerationRunner(Path.of(System.getProperty("java.home"), "bin", executable),
                System.getProperty("java.class.path"), timeout);
    }

    private GenerationRequest createRequest(Path project) {
        return new GenerationRequest(project, temporaryDirectory.resolve("jobs"), GenerationSettings.defaults(),
                new ObservedProfile(List.of(), List.of()));
    }

    @Test
    void shouldReturnTheSameTestsAndReportAsDirectGenerationWithProfile() throws Exception {
        Path project = Path.of("src/test/resources/sample-maven-project");
        var profile = new ObservedProfile(List.of(new ArgumentValueHint(
                "sample.maven.DiscountService.shippingFee(int)", "amount", "int", "99", "runtime-observed")), List.of());
        var request = new GenerationRequest(project, temporaryDirectory.resolve("jobs"), GenerationSettings.defaults(), profile);
        GenerationResult expectedResult = TestgenUseCases.generation().generate(new GenerationRequest(
                project, temporaryDirectory.resolve("direct"), request.settings(), profile));
        var jobStore = new InMemoryGenerationJobStore(Clock.systemUTC());
        GenerationJob generationJob = jobStore.create("sample", request);
        var executeJob = new ExecuteGenerationJobUseCase(jobStore, createRunner(Duration.ofSeconds(60)));

        GenerationJob completedJob = executeJob.execute(generationJob.id()).orElseThrow();

        assertEquals(JobStatus.SUCCEEDED, completedJob.status(), () -> completedJob.error().orElse(""));
        GenerationResult actualResult = completedJob.result().orElseThrow();
        assertFalse(actualResult.tests().isEmpty());
        assertEquals(expectedResult.tests(), actualResult.tests());
        assertEquals(expectedResult.reportText(), actualResult.reportText());
        assertEquals(expectedResult.metrics(), actualResult.metrics());
        assertEquals(expectedResult.coveredGoalIds(), actualResult.coveredGoalIds());
        Path jobDirectory = request.workRoot().resolve(generationJob.id().toString());
        assertTrue(Files.isRegularFile(jobDirectory.resolve("process.log")));
        assertTrue(Files.isRegularFile(jobDirectory.resolve("result.json")));
        assertTrue(Files.isDirectory(jobDirectory.resolve("workspace/classes")));
        assertThrows(FileAlreadyExistsException.class,
                () -> createRunner(Duration.ofSeconds(60)).run(generationJob.id(), request));
        // A finished job is not executed again, e.g. when its request is delivered twice.
        assertTrue(new ExecuteGenerationJobUseCase(jobStore, createRunner(Duration.ofSeconds(60)))
                .execute(generationJob.id()).isEmpty());
    }

    @Test
    void shouldRecordNonzeroExitAsFailureWithLogLocation() throws Exception {
        var jobStore = new InMemoryGenerationJobStore(Clock.systemUTC());
        GenerationJob generationJob = jobStore.create("missing", createRequest(temporaryDirectory.resolve("missing-project")));
        var executeJob = new ExecuteGenerationJobUseCase(jobStore, createRunner(Duration.ofSeconds(30)));

        GenerationJob completedJob = executeJob.execute(generationJob.id()).orElseThrow();
        assertEquals(JobStatus.FAILED, completedJob.status());
        assertTrue(completedJob.error().orElseThrow().contains("exited with code"));
        assertTrue(completedJob.error().orElseThrow().contains("process.log"));
        assertTrue(completedJob.result().isEmpty());
        assertFalse(Files.exists(generationJob.request().workRoot().resolve(generationJob.id().toString()).resolve("result.json")));
    }

    @Test
    void shouldRejectZeroExitWithoutAResult() throws Exception {
        Path project = temporaryDirectory.resolve("early-exit");
        Path source = project.resolve("src/main/java/sample/Exit.java");
        Files.createDirectories(source.getParent());
        Files.writeString(project.resolve("pom.xml"), """
                <project><modelVersion>4.0.0</modelVersion><groupId>sample</groupId>
                <artifactId>exit</artifactId><version>1</version></project>
                """);
        Files.writeString(source, """
                package sample;
                public class Exit {
                    public int exit() {
                        System.exit(0);
                        return 0;
                    }
                }
                """);
        var jobStore = new InMemoryGenerationJobStore(Clock.systemUTC());
        GenerationJob generationJob = jobStore.create("early-exit", createRequest(project));
        var executeJob = new ExecuteGenerationJobUseCase(jobStore, createRunner(Duration.ofSeconds(30)));

        GenerationJob completedJob = executeJob.execute(generationJob.id()).orElseThrow();
        assertEquals(JobStatus.FAILED, completedJob.status());
        assertTrue(completedJob.error().orElseThrow().contains("no result"));
    }

    @Test
    void shouldKillHangingGeneratorAndAllowNextJob() throws Exception {
        Path processIdPath = temporaryDirectory.resolve("pid");
        Path project = hangingProject(processIdPath);
        var jobStore = new InMemoryGenerationJobStore(Clock.systemUTC());
        GenerationJob generationJob = jobStore.create("hang", createRequest(project));
        var executeJob = new ExecuteGenerationJobUseCase(jobStore, createRunner(Duration.ofSeconds(8)));

        GenerationJob completedJob = executeJob.execute(generationJob.id()).orElseThrow();
        assertEquals(JobStatus.TIMED_OUT, completedJob.status(), () -> completedJob.error().orElse(""));
        assertTrue(Files.exists(processIdPath), "Candidate must actually start before timing out");
        long pid = Long.parseLong(Files.readString(processIdPath));
        assertFalse(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false));
        GenerationJob nextJob = jobStore.create("sample", createRequest(Path.of("src/test/resources/sample-maven-project")));
        var executeNextJob = new ExecuteGenerationJobUseCase(jobStore, createRunner(Duration.ofSeconds(60)));
        GenerationJob completedNextJob = executeNextJob.execute(nextJob.id()).orElseThrow();
        assertEquals(JobStatus.SUCCEEDED, completedNextJob.status());
    }

    @Test
    void shouldStopProcessOnThreadInterruption() throws Exception {
        Path processIdPath = temporaryDirectory.resolve("interrupted-pid");
        Path project = hangingProject(processIdPath);
        var jobStore = new InMemoryGenerationJobStore(Clock.systemUTC());
        GenerationJob generationJob = jobStore.create("hang", createRequest(project));
        var interruptionPreserved = new AtomicBoolean();
        Thread executionThread = new Thread(() -> {
            new ExecuteGenerationJobUseCase(jobStore, createRunner(Duration.ofSeconds(60))).execute(generationJob.id());
            interruptionPreserved.set(Thread.currentThread().isInterrupted());
        });
        executionThread.start();
        try {
            long startupDeadlineNanos = System.nanoTime() + Duration.ofSeconds(30).toNanos();
            while (!Files.exists(processIdPath) && executionThread.isAlive() && System.nanoTime() < startupDeadlineNanos) {
                Thread.sleep(25);
            }
            assertTrue(Files.exists(processIdPath), "Candidate must start before interrupting");
            executionThread.interrupt();
            executionThread.join(10_000);
            assertFalse(executionThread.isAlive());
            assertEquals(JobStatus.INTERRUPTED, jobStore.findById(generationJob.id()).orElseThrow().status());
            assertTrue(interruptionPreserved.get());
            long pid = Long.parseLong(Files.readString(processIdPath));
            assertFalse(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false));
        } finally {
            executionThread.interrupt();
            executionThread.join(10_000);
        }
    }

    private Path hangingProject(Path processIdPath) throws Exception {
        Path project = temporaryDirectory.resolve("hanging");
        Path source = project.resolve("src/main/java/sample/Hang.java");
        Files.createDirectories(source.getParent());
        Files.writeString(project.resolve("pom.xml"), """
                <project><modelVersion>4.0.0</modelVersion><groupId>sample</groupId>
                <artifactId>hang</artifactId><version>1</version></project>
                """);
        String escapedProcessIdPath = processIdPath.toString().replace("\\", "\\\\").replace("\"", "\\\"");
        Files.writeString(source, """
                package sample;
                public class Hang {
                    public int loop() throws Exception {
                        java.nio.file.Files.writeString(java.nio.file.Path.of("%s"),
                            Long.toString(ProcessHandle.current().pid()));
                        while (true) {
                            Thread.sleep(100);
                        }
                    }
                }
                """.formatted(escapedProcessIdPath));
        return project;
    }

}
