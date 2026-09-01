package com.mlisows.testgen.infrastructure.project;

import com.mlisows.testgen.domain.CoverageGoal;
import com.mlisows.testgen.infrastructure.execution.JavaSourceCompiler;
import com.mlisows.testgen.infrastructure.instrumentation.JavaParserBranchInstrumenter;
import com.mlisows.testgen.infrastructure.instrumentation.SourceDirectoryInstrumenter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public final class InstrumentedProjectWorkspacePreparer {
    private final SourceDirectoryInstrumenter instrumenter;
    private final JavaSourceCompiler compiler;
    private final String classpath;

    public InstrumentedProjectWorkspacePreparer(
            SourceDirectoryInstrumenter instrumenter,
            JavaSourceCompiler compiler,
            String classpath
    ) {
        this.instrumenter = Objects.requireNonNull(instrumenter, "instrumenter must not be null");
        this.compiler = Objects.requireNonNull(compiler, "compiler must not be null");
        this.classpath = Objects.requireNonNull(classpath, "classpath must not be null");
    }

    public InstrumentedProjectWorkspacePreparer() {
        this(
                new SourceDirectoryInstrumenter(new JavaParserBranchInstrumenter()),
                new JavaSourceCompiler(),
                System.getProperty("java.class.path")
        );
    }

    public InstrumentedProjectWorkspace prepare(
            MavenProjectLayout layout,
            Path workRoot,
            Map<Path, List<CoverageGoal>> coverageGoalsBySourcePath
    ) {
        Objects.requireNonNull(layout, "layout must not be null");
        Objects.requireNonNull(workRoot, "workRoot must not be null");
        Objects.requireNonNull(coverageGoalsBySourcePath, "coverageGoalsBySourcePath must not be null");

        Path instrumentedSourceRoot = workRoot.resolve("instrumented-src");
        Path classesRoot = workRoot.resolve("classes");

        createDirectory(workRoot);

        instrumenter.instrument(
                layout.getMainSourceRoot(),
                instrumentedSourceRoot,
                coverageGoalsBySourcePath
        );

        compiler.compile(instrumentedSourceRoot, classesRoot);
        layout.getMainResourceRoot().ifPresent(resourceRoot -> copyResources(resourceRoot, classesRoot));

        return new InstrumentedProjectWorkspace(
                instrumentedSourceRoot,
                classesRoot,
                classpath
        );
    }

    private void copyResources(Path resourceRoot, Path classesRoot) {
        try (Stream<Path> paths = Files.walk(resourceRoot)) {
            paths.filter(Files::isRegularFile)
                    .forEach(sourcePath -> copyResource(resourceRoot, classesRoot, sourcePath));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot copy resources from directory: " + resourceRoot, exception);
        }
    }

    private void copyResource(Path resourceRoot, Path classesRoot, Path sourcePath) {
        Path targetPath = classesRoot.resolve(resourceRoot.relativize(sourcePath));

        try {
            Files.createDirectories(targetPath.getParent());
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot copy resource file: " + sourcePath, exception);
        }
    }

    private void createDirectory(Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot create work directory: " + directory, exception);
        }
    }
}
