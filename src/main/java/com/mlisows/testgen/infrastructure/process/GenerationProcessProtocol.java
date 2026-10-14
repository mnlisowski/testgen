package com.mlisows.testgen.infrastructure.process;

import com.google.gson.Gson;
import com.mlisows.testgen.domain.generation.GenerationSettings;
import com.mlisows.testgen.domain.profile.ObservedProfile;
import com.mlisows.testgen.usecase.generation.GeneratedTestFile;
import com.mlisows.testgen.usecase.generation.GenerationMetrics;
import com.mlisows.testgen.usecase.generation.GenerationRequest;
import com.mlisows.testgen.usecase.generation.GenerationResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Internal format for processes from the same build, not a public API or permanent storage format. */
public final class GenerationProcessProtocol {
    private static final Gson JSON = new Gson();
    private static final int PROTOCOL_VERSION = 2;

    private record ProcessRequest(int version, String projectRoot, String workRoot,
                                  GenerationSettings settings, ObservedProfile profile) {
    }

    private record ProcessTestFile(String path, String content) {
    }

    private record ProcessResult(int version, List<ProcessTestFile> tests, String reportText,
                                 GenerationMetrics metrics, Set<String> coveredGoalIds) {
    }

    public void writeRequest(Path requestPath, GenerationRequest generationRequest) throws IOException {
        String projectRoot = generationRequest.projectRoot().toAbsolutePath().normalize().toString();
        String workRoot = generationRequest.workRoot().toAbsolutePath().normalize().toString();

        ProcessRequest processRequest = new ProcessRequest(
                PROTOCOL_VERSION, projectRoot, workRoot,
                generationRequest.settings(), generationRequest.observedProfile()
        );
        Files.writeString(requestPath, JSON.toJson(processRequest));
    }

    public GenerationRequest readRequest(Path requestPath) throws IOException {
        String requestJson = Files.readString(requestPath);
        ProcessRequest processRequest = JSON.fromJson(requestJson, ProcessRequest.class);
        Objects.requireNonNull(processRequest, "request");
        checkVersion(processRequest.version());

        ObservedProfile observedProfile = new ObservedProfile(
                processRequest.profile().getArgumentValueHints(),
                processRequest.profile().getObservedInvocations()
        );
        return new GenerationRequest(
                Path.of(processRequest.projectRoot()),
                Path.of(processRequest.workRoot()),
                processRequest.settings(),
                observedProfile
        );
    }

    public void writeResult(Path resultPath, GenerationResult generationResult) throws IOException {
        List<ProcessTestFile> processTestFiles = new ArrayList<>();
        for (GeneratedTestFile generatedTest : generationResult.tests()) {
            processTestFiles.add(new ProcessTestFile(
                    generatedTest.relativePath().toString(), generatedTest.content()
            ));
        }

        ProcessResult processResult = new ProcessResult(
                PROTOCOL_VERSION, processTestFiles, generationResult.reportText(),
                generationResult.metrics(), generationResult.coveredGoalIds()
        );
        Path temporaryResultPath = resultPath.resolveSibling(resultPath.getFileName() + ".tmp");
        Files.writeString(temporaryResultPath, JSON.toJson(processResult));
        Files.move(temporaryResultPath, resultPath, StandardCopyOption.ATOMIC_MOVE);
    }

    public GenerationResult readResult(Path resultPath) throws IOException {
        String resultJson = Files.readString(resultPath);
        ProcessResult processResult = JSON.fromJson(resultJson, ProcessResult.class);
        Objects.requireNonNull(processResult, "result");
        checkVersion(processResult.version());

        List<GeneratedTestFile> generatedTests = new ArrayList<>();
        for (ProcessTestFile processTest : processResult.tests()) {
            generatedTests.add(new GeneratedTestFile(Path.of(processTest.path()), processTest.content()));
        }

        return new GenerationResult(generatedTests, processResult.reportText(),
                processResult.metrics(), processResult.coveredGoalIds());
    }

    private static void checkVersion(int version) {
        if (version != PROTOCOL_VERSION) {
            throw new IllegalArgumentException("Unsupported generation protocol version: " + version);
        }
    }
}
