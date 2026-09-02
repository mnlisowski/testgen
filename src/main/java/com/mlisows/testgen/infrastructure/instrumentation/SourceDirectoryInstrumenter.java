package com.mlisows.testgen.infrastructure.instrumentation;

import com.mlisows.testgen.domain.CoverageGoal;
import com.mlisows.testgen.usecase.ports.SourceInstrumenter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SourceDirectoryInstrumenter {
    private final SourceInstrumenter sourceInstrumenter;

    public SourceDirectoryInstrumenter(SourceInstrumenter sourceInstrumenter) {
        this.sourceInstrumenter = Objects.requireNonNull(sourceInstrumenter, "sourceInstrumenter must not be null");
    }

    public List<Path> instrument(
            Path sourceRoot,
            Path outputRoot,
            Map<Path, List<CoverageGoal>> coverageGoalsBySourcePath
    ) {
        Objects.requireNonNull(sourceRoot, "sourceRoot must not be null");
        Objects.requireNonNull(outputRoot, "outputRoot must not be null");
        Objects.requireNonNull(coverageGoalsBySourcePath, "coverageGoalsBySourcePath must not be null");

        validateSourceRoot(sourceRoot);
        validateOutputRoot(sourceRoot, outputRoot);

        Map<Path, List<CoverageGoal>> normalizedGoals = normalizePaths(coverageGoalsBySourcePath);

        try {
            return Files.walk(sourceRoot)
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(Path::toString))
                    .map(sourcePath -> instrumentOrCopy(sourceRoot, outputRoot, sourcePath, normalizedGoals))
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot instrument source directory: " + sourceRoot, exception);
        }
    }

    private void validateSourceRoot(Path sourceRoot) {
        if (!Files.isDirectory(sourceRoot)) {
            throw new IllegalArgumentException("Source root is not a directory: " + sourceRoot);
        }
    }

    private void validateOutputRoot(Path sourceRoot, Path outputRoot) {
        Path normalizedSourceRoot = sourceRoot.toAbsolutePath().normalize();
        Path normalizedOutputRoot = outputRoot.toAbsolutePath().normalize();

        if (normalizedOutputRoot.startsWith(normalizedSourceRoot)) {
            throw new IllegalArgumentException("Output root must not be inside source root: " + outputRoot);
        }
    }

    private Map<Path, List<CoverageGoal>> normalizePaths(Map<Path, List<CoverageGoal>> coverageGoalsBySourcePath) {
        return coverageGoalsBySourcePath.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        entry -> entry.getKey().toAbsolutePath().normalize(),
                        Map.Entry::getValue
                ));
    }

    private Path instrumentOrCopy(
            Path sourceRoot,
            Path outputRoot,
            Path sourcePath,
            Map<Path, List<CoverageGoal>> coverageGoalsBySourcePath
    ) {
        Path outputPath = outputRoot.resolve(sourceRoot.relativize(sourcePath));
        List<CoverageGoal> coverageGoals = coverageGoalsBySourcePath.getOrDefault(
                sourcePath.toAbsolutePath().normalize(),
                List.of()
        );

        if (isJavaSource(sourcePath) && sourceInstrumenter.shouldInstrument(sourcePath, coverageGoals)) {
            return sourceInstrumenter.instrument(sourcePath, outputPath, coverageGoals);
        }


        copy(sourcePath, outputPath);
        return outputPath;
    }

    private boolean isJavaSource(Path path) {
        return path.toString().endsWith(".java");
    }

    private void copy(Path sourcePath, Path outputPath) {
        try {
            Path parent = outputPath.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.copy(sourcePath, outputPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot copy source file: " + sourcePath, exception);
        }
    }
}
