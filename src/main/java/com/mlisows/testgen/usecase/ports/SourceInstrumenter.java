package com.mlisows.testgen.usecase.ports;

import java.nio.file.Path;

public interface SourceInstrumenter {
    Path instrument(Path sourcePath, Path outputPath);
}
