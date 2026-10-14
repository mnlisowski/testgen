package com.mlisows.testgen.infrastructure.project;

import com.mlisows.testgen.usecase.ports.ProjectSourceLocator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class MavenProjectSourceLocator implements ProjectSourceLocator {
    @Override
    public List<Path> findJavaSources(Path projectRoot) {
        Path sourceRoot = new MavenProjectLayoutDetector().detect(projectRoot).getMainSourceRoot();
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read source directory: " + sourceRoot, exception);
        }
    }

}
