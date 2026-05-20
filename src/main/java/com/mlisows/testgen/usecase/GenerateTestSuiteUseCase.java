package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.ClassStructure;
import com.mlisows.testgen.domain.GeneratedTestSuite;
import com.mlisows.testgen.domain.MethodGenerationPlan;
import com.mlisows.testgen.usecase.ports.TestFileWriter;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class GenerateTestSuiteUseCase {
    private final GeneratedTestSuiteFactory testSuiteFactory;
    private final JUnitTestWriter testWriter;
    private final TestFileWriter fileWriter;

    public GenerateTestSuiteUseCase(
            GeneratedTestSuiteFactory testSuiteFactory,
            JUnitTestWriter testWriter,
            TestFileWriter fileWriter
    ) {
        this.testSuiteFactory = Objects.requireNonNull(testSuiteFactory, "testSuiteFactory must not be null");
        this.testWriter = Objects.requireNonNull(testWriter, "testWriter must not be null");
        this.fileWriter = Objects.requireNonNull(fileWriter, "fileWriter must not be null");
    }

    public Path execute(ClassStructure classStructure, List<MethodGenerationPlan> methodPlans) {
        Objects.requireNonNull(classStructure, "classStructure must not be null");
        Objects.requireNonNull(methodPlans, "methodPlans must not be null");

        GeneratedTestSuite testSuite = testSuiteFactory.create(classStructure, methodPlans);
        String code = testWriter.write(testSuite);

        return fileWriter.write(testSuite, code);
    }
}
