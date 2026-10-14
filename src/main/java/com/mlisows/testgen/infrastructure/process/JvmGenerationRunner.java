package com.mlisows.testgen.infrastructure.process;

import com.mlisows.testgen.usecase.generation.GenerationRequest;
import com.mlisows.testgen.usecase.generation.GenerationResult;
import com.mlisows.testgen.usecase.ports.GenerationRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class JvmGenerationRunner implements GenerationRunner {
    private static final long PROCESS_POLL_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(100);
    private static final long PROCESS_CLEANUP_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(5);

    private final Path javaExecutable;
    private final String classpath;
    private final Duration timeout;
    private final GenerationProcessProtocol protocol = new GenerationProcessProtocol();

    public JvmGenerationRunner(Path javaExecutable, String classpath, Duration timeout) {
        this.javaExecutable = Objects.requireNonNull(javaExecutable, "javaExecutable").toAbsolutePath();
        this.classpath = Objects.requireNonNull(classpath, "classpath");
        this.timeout = Objects.requireNonNull(timeout, "timeout");

        if (classpath.isBlank()) {
            throw new IllegalArgumentException("Runner classpath must not be blank");
        }
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("Runner timeout must be positive");
        }
    }

    @Override
    public GenerationResult run(UUID jobId, GenerationRequest generationRequest)
            throws IOException, InterruptedException, TimeoutException {
        Path jobDirectory = createJobDirectory(jobId, generationRequest.workRoot());
        Path requestPath = jobDirectory.resolve("request.json");
        Path resultPath = jobDirectory.resolve("result.json");
        Path processLogPath = jobDirectory.resolve("process.log");

        writeProcessRequest(generationRequest, jobDirectory, requestPath);

        Process generatorProcess = startProcess(requestPath, resultPath, processLogPath);
        Set<ProcessHandle> observedDescendants = new LinkedHashSet<>();
        try {
            waitForCompletion(generatorProcess, observedDescendants, processLogPath);
            verifyProcessResult(generatorProcess, resultPath, processLogPath);
            return protocol.readResult(resultPath);
        } finally {
            stopProcessTree(generatorProcess, observedDescendants);
        }
    }

    private Path createJobDirectory(UUID jobId, Path workRoot) throws IOException {
        Path baseDirectory = workRoot.toAbsolutePath().normalize();
        Files.createDirectories(baseDirectory);
        // An existing directory could contain a stale result from an earlier execution.
        return Files.createDirectory(baseDirectory.resolve(jobId.toString()));
    }

    private void writeProcessRequest(GenerationRequest generationRequest, Path jobDirectory, Path requestPath)
            throws IOException {
        GenerationRequest processRequest = new GenerationRequest(
                generationRequest.projectRoot(),
                jobDirectory.resolve("workspace"),
                generationRequest.settings(),
                generationRequest.observedProfile()
        );
        protocol.writeRequest(requestPath, processRequest);
    }

    private Process startProcess(Path requestPath, Path resultPath, Path processLogPath) throws IOException {
        List<String> command = List.of(
                javaExecutable.toString(),
                "-cp",
                classpath,
                GenerationProcessMain.class.getName(),
                requestPath.toString(),
                resultPath.toString()
        );
        return new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(processLogPath.toFile())
                .start();
    }

    private void waitForCompletion(Process generatorProcess, Set<ProcessHandle> observedDescendants,
                                   Path processLogPath) throws InterruptedException, TimeoutException {
        long deadlineNanos = System.nanoTime() + timeout.toNanos();

        while (true) {
            generatorProcess.descendants().forEach(observedDescendants::add);

            long remainingNanos = deadlineNanos - System.nanoTime();
            if (remainingNanos <= 0) {
                throw new TimeoutException("Generation exceeded " + timeout + "; log: " + processLogPath);
            }

            long waitTimeNanos = Math.min(remainingNanos, PROCESS_POLL_INTERVAL_NANOS);
            boolean finished = generatorProcess.waitFor(waitTimeNanos, TimeUnit.NANOSECONDS);
            if (finished) {
                return;
            }
        }
    }

    private void verifyProcessResult(Process generatorProcess, Path resultPath, Path processLogPath)
            throws IOException {
        int exitCode = generatorProcess.exitValue();
        if (exitCode != 0) {
            throw new IOException("Generator exited with code " + exitCode + "; log: " + processLogPath);
        }
        if (!Files.isRegularFile(resultPath)) {
            throw new IOException("Generator produced no result; log: " + processLogPath);
        }
    }

    private void stopProcessTree(Process generatorProcess, Set<ProcessHandle> observedDescendants)
            throws IOException {
        generatorProcess.descendants().forEach(observedDescendants::add);
        for (ProcessHandle descendant : observedDescendants) {
            if (descendant.isAlive()) {
                descendant.destroyForcibly();
            }
        }
        if (generatorProcess.isAlive()) {
            generatorProcess.destroyForcibly();
        }

        waitForProcessTreeToStop(generatorProcess, observedDescendants);
    }

    private void waitForProcessTreeToStop(Process generatorProcess, Set<ProcessHandle> observedDescendants)
            throws IOException {
        boolean interruptedDuringCleanup = false;
        long deadlineNanos = System.nanoTime() + PROCESS_CLEANUP_TIMEOUT_NANOS;

        try {
            while (isProcessTreeAlive(generatorProcess, observedDescendants)) {
                if (System.nanoTime() >= deadlineNanos) {
                    throw new IOException("Could not stop generator process tree");
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException exception) {
                    // Finish cleanup before restoring the caller's interruption signal.
                    interruptedDuringCleanup = true;
                }
            }
        } finally {
            if (interruptedDuringCleanup) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean isProcessTreeAlive(Process generatorProcess, Set<ProcessHandle> observedDescendants) {
        if (generatorProcess.isAlive()) {
            return true;
        }
        for (ProcessHandle descendant : observedDescendants) {
            if (descendant.isAlive()) {
                return true;
            }
        }
        return false;
    }
}
