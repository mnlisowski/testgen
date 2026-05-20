package com.mlisows.testgen.usecase.ports;

import com.mlisows.testgen.domain.GeneratedTestSuite;

import java.nio.file.Path;

public interface TestFileWriter {
    Path write(GeneratedTestSuite testSuite, String code);
}
