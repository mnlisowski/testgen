package com.mlisows.testgen.infrastructure.execution;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public final class JavaSourceCompiler {
    private final String classpath;

    public JavaSourceCompiler() {
        this(System.getProperty("java.class.path"));
    }

    public JavaSourceCompiler(String classpath) {
        this.classpath = Objects.requireNonNull(classpath, "classpath must not be null");
    }

    public Path compile(Path sourceRoot, Path classesRoot) {
        Objects.requireNonNull(sourceRoot, "sourceRoot must not be null");
        Objects.requireNonNull(classesRoot, "classesRoot must not be null");

        if (!Files.isDirectory(sourceRoot)) {
            throw new IllegalArgumentException("Source root is not a directory: " + sourceRoot);
        }

        List<Path> sourceFiles = javaSourceFiles(sourceRoot);

        if (sourceFiles.isEmpty()) {
            return classesRoot;
        }

        createDirectories(classesRoot);
        runCompiler(sourceRoot, classesRoot, sourceFiles);

        return classesRoot;
    }

    private List<Path> javaSourceFiles(Path sourceRoot) {
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot read source directory: " + sourceRoot, exception);
        }
    }

    private void createDirectories(Path classesRoot) {
        try {
            Files.createDirectories(classesRoot);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot create classes directory: " + classesRoot, exception);
        }
    }

    private void runCompiler(Path sourceRoot, Path classesRoot, List<Path> sourceFiles) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        if (compiler == null) {
            throw new IllegalStateException("JDK compiler must be available");
        }

        List<String> arguments = new ArrayList<>();
        arguments.add("-classpath");
        arguments.add(classpath);
        arguments.add("-d");
        arguments.add(classesRoot.toString());
        sourceFiles.stream()
                .map(Path::toString)
                .forEach(arguments::add);

        ByteArrayOutputStream compilerOutput = new ByteArrayOutputStream();

        int exitCode = compiler.run(
                null,
                compilerOutput,
                compilerOutput,
                arguments.toArray(String[]::new)
        );

        if (exitCode != 0) {
            throw new IllegalStateException(
                    "Cannot compile source directory: "
                            + sourceRoot
                            + System.lineSeparator()
                            + compilerOutput.toString(StandardCharsets.UTF_8)
            );
        }
    }
}
