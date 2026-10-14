package com.mlisows.testgen.usecase.ports;

import java.nio.file.Path;
import java.util.List;

public interface ProjectSourceLocator {
    List<Path> findJavaSources(Path projectRoot);
}
