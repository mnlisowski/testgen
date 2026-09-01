package com.mlisows.testgen.infrastructure.project;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilderFactory;

public final class MavenDependencyClasspathResolver {

    public String resolve(MavenProjectLayout layout) {
        Objects.requireNonNull(layout, "layout must not be null");

        if (!hasDependencies(layout.getPomPath())) {
            return "";
        }

        Path outputPath = createClasspathOutputPath();

        try {
            Process process = new ProcessBuilder(command(outputPath))
                    .directory(layout.getProjectRoot().toFile())
                    .redirectErrorStream(true)
                    .start();

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new IllegalStateException("Cannot resolve Maven dependency classpath for "
                        + layout.getProjectRoot()
                        + System.lineSeparator()
                        + output);
            }

            if (!Files.isRegularFile(outputPath)) {
                return "";
            }

            return Files.readString(outputPath).trim();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot run Maven dependency classpath resolution", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Maven dependency classpath resolution was interrupted", exception);
        } finally {
            deleteIfExists(outputPath);
        }
    }

    private boolean hasDependencies(Path pomPath) {
        try (InputStream inputStream = Files.newInputStream(pomPath)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);

            return factory.newDocumentBuilder()
                    .parse(inputStream)
                    .getElementsByTagNameNS("*", "dependency")
                    .getLength() > 0;
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot read pom.xml: " + pomPath, exception);
        }
    }

    private Path createClasspathOutputPath() {
        try {
            return Files.createTempFile("testgen-maven-classpath", ".txt");
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot create Maven classpath output file", exception);
        }
    }

    private List<String> command(Path outputPath) {
        return List.of(
                "mvn",
                "-q",
                "-Denforcer.skip=true",
                "-Drat.skip=true",
                "-Dmdep.includeScope=compile",
                "-Dmdep.outputFile=" + outputPath.toAbsolutePath(),
                "-Dmdep.pathSeparator=" + File.pathSeparator,
                "dependency:build-classpath"
        );
    }

    private void deleteIfExists(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}
