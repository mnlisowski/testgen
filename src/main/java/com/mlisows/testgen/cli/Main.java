package com.mlisows.testgen.cli;

import com.mlisows.testgen.domain.generation.GenerationSettings;
import com.mlisows.testgen.usecase.generation.GenerationRequest;
import com.mlisows.testgen.usecase.generation.GenerationResult;
import com.mlisows.testgen.usecase.profile.ProfilePreparationRequest;
import com.mlisows.testgen.usecase.profile.ProfilePreparationResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import com.mlisows.testgen.domain.profile.ObservedProfile;
import com.mlisows.testgen.infrastructure.TestgenUseCases;
import com.mlisows.testgen.infrastructure.runtime.TextObservedProfileReader;
import com.mlisows.testgen.infrastructure.writer.GeneratedCandidateTestFileWriter;
import com.google.gson.GsonBuilder;
import com.mlisows.testgen.usecase.GenerationReport;
import com.mlisows.testgen.usecase.ports.ObservedProfileReader;

public final class Main {
    private static final String GENERATE_TESTS_USAGE =
            "Usage: testgen <maven-project-root> [work-root] [observed-profile|-]"
                    + " [seed] [max-candidates] [initial-candidates] [mutated-initial-candidates] [max-values-per-slot]\n"
                    + "Defaults: seed=0, max-candidates=50, initial-candidates=10,"
                    + " mutated-initial-candidates=3, max-values-per-slot=10";
    private static final String PREPARE_PROFILE_USAGE =
            "usage: testgen prepare-profile <maven-project-root> <work-root>";
    private static final String DEFAULT_TEST_WORK_DIRECTORY = "target/testgen-work";
    private static final String DEFAULT_PROFILE_WORK_DIRECTORY = "target/testgen-profile-work";

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("prepare-profile")) {
            prepareProfile(args);
            return;
        }

        generateTests(args);
    }

    private static void generateTests(String[] args) {
        if (args.length == 0 || args.length > 8 || args[0].equals("--help")) {
            System.out.println(GENERATE_TESTS_USAGE);
            return;
        }
        GenerationSettings defaults = GenerationSettings.defaults();
        long seed = args.length > 3 ? Long.parseLong(args[3]) : defaults.seed();
        int maxCandidates = args.length > 4 ? Integer.parseInt(args[4]) : defaults.maxCandidates();
        int initialCandidates = args.length > 5 ? Integer.parseInt(args[5]) : defaults.initialCandidates();
        int mutatedInitialCandidates = args.length > 6 ? Integer.parseInt(args[6]) : defaults.mutatedInitialCandidates();
        int maxValuesPerSlot = args.length > 7 ? Integer.parseInt(args[7]) : defaults.maxValuesPerSlot();
        GenerationSettings generationSettings = new GenerationSettings(
                seed, maxCandidates, initialCandidates, mutatedInitialCandidates, maxValuesPerSlot);

        Path projectRoot = Path.of(args[0]);
        Path workRoot = getWorkRoot(projectRoot, args);
        Path observedProfilePath = observedProfilePath(args);

        printGenerationStart(projectRoot, workRoot, observedProfilePath);

        ObservedProfile observedProfile = readObservedProfile(observedProfilePath);
        GenerationResult result = TestgenUseCases.generation().generate(
                new GenerationRequest(projectRoot, workRoot, generationSettings, observedProfile));
        Path outputRoot = workRoot.resolve("generated-tests");
        new GeneratedCandidateTestFileWriter().writeFiles(outputRoot, result.tests());

        System.out.println("Generated tests: " + outputRoot);

        String settings = "Ustawienia generowania\n"
                + "Ziarno: " + seed + "\n"
                + "Limit kandydatów na metodę: " + maxCandidates + "\n"
                + "Liczba prób losowania kandydatów: " + initialCandidates + "\n"
                + "Limit losowych startów do mutowania (oprócz bazowego): " + mutatedInitialCandidates + "\n"
                + "Limit wartości na argument: " + maxValuesPerSlot + "\n"
                + "Profil: " + (observedProfilePath == null ? "wyłączony"
                        : observedProfilePath.toAbsolutePath().normalize()) + "\n\n";
        writeReport(workRoot, result.report(), settings);
    }

    private static void prepareProfile(String[] args) {
        if (args.length < 2 || args.length > 3) {
            System.out.println(PREPARE_PROFILE_USAGE);
            return;
        }

        Path projectRoot = Path.of(args[1]);
        Path workRoot = args.length == 3
                ? Path.of(args[2])
                : projectRoot.resolve(DEFAULT_PROFILE_WORK_DIRECTORY);

        System.out.println("Preparing profile workspace");
        System.out.println("Project root: " + projectRoot);
        System.out.println("Work root: " + workRoot);

        ProfilePreparationResult result = TestgenUseCases.profilePreparation().prepare(
                new ProfilePreparationRequest(projectRoot, workRoot));
        printProfileRunInstructions(workRoot, result);
    }

    private static void printProfileRunInstructions(Path workRoot, ProfilePreparationResult result) {
        System.out.println("Instrumented sources: " + result.sourceRoot());
        System.out.println("Instrumented classes: " + result.classesRoot());
        System.out.println("Observed profile output: " + result.profilePath());
        System.out.println("Run instrumented application with:");
        System.out.println("java -Dtestgen.profile.dir=" + workRoot
                + " -cp \"" + result.runtimeClasspath() + "\" <main-class>");
    }

    private static ObservedProfile readObservedProfile(Path observedProfilePath) {
        if (observedProfilePath == null) {
            System.out.println("Observed profile: skipped");
            return new ObservedProfile(List.of(), List.of());
        }

        ObservedProfileReader observedProfileReader = new TextObservedProfileReader();
        ObservedProfile observedProfile = observedProfileReader.read(observedProfilePath);

        System.out.println("Observed profile hints: " + observedProfile.getArgumentValueHints().size());
        System.out.println("Observed invocations: " + observedProfile.getObservedInvocations().size());

        return observedProfile;
    }

    private static Path getWorkRoot(Path projectRoot, String[] args) {
        if (args.length >= 2) {
            return Path.of(args[1]);
        }

        return projectRoot.resolve(DEFAULT_TEST_WORK_DIRECTORY);
    }

    static Path observedProfilePath(String[] args) {
        if (args.length >= 3 && !args[2].equals("-")) {
            return Path.of(args[2]);
        }

        return null;
    }

    private static void printGenerationStart(
            Path projectRoot,
            Path workRoot,
            Path observedProfilePath
    ) {
        System.out.println("Generating tests");
        System.out.println("Project root: " + projectRoot);
        System.out.println("Work root: " + workRoot);

        if (observedProfilePath != null) {
            System.out.println("Observed profile: " + observedProfilePath);
        }
    }

    private static void writeReport(Path workRoot, GenerationReport report, String settings) {
        Path reportPath = workRoot.resolve("report.txt");
        Path coveredGoalsPath = workRoot.resolve("covered-goals.json");

        try {
            Files.createDirectories(workRoot);
            Files.writeString(reportPath, settings + report.toText());
            writeCoveredGoals(coveredGoalsPath, report.getCoveredBranchIds());
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write generation results in: " + workRoot, exception);
        }

        System.out.println("Report: " + reportPath);
        System.out.println("Covered goals: " + coveredGoalsPath);
    }

    static void writeCoveredGoals(Path path, Set<String> ids) throws IOException {
        String json = new GsonBuilder().setPrettyPrinting().create().toJson(ids.stream().sorted().toList());
        Files.writeString(path, json + "\n");
    }

}
