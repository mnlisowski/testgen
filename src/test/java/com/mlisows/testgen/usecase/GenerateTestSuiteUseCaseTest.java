package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.ClassStructure;
import com.mlisows.testgen.domain.ConstructorModel;
import com.mlisows.testgen.domain.MethodGenerationPlan;
import com.mlisows.testgen.domain.MethodModel;
import com.mlisows.testgen.domain.ParameterModel;
import com.mlisows.testgen.infrastructure.writer.GeneratedTestFileWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerateTestSuiteUseCaseTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldGenerateAndWriteTestSuite() throws Exception {
        MethodModel method = new MethodModel(
                "calculate",
                "int",
                true,
                List.of(new ParameterModel("amount", "int"))
        );

        ClassStructure classStructure = new ClassStructure(
                "sample.Calculator",
                List.of(new ConstructorModel(true, List.of())),
                List.of(method)
        );

        MethodGenerationPlanner planner = new MethodGenerationPlanner();
        List<MethodGenerationPlan> methodPlans = planner.plan(classStructure);

        GenerateTestSuiteUseCase useCase = new GenerateTestSuiteUseCase(
                new GeneratedTestSuiteFactory(
                        new GeneratedTestCaseFactory(new SimpleArgumentGenerator()),
                        new GeneratableCoverageGoalSelector()
                ),
                new JUnitTestWriter(),
                new GeneratedTestFileWriter(tempDir)
        );

        Path writtenPath = useCase.execute(classStructure, methodPlans);

        String code = Files.readString(writtenPath);

        assertTrue(Files.exists(writtenPath));
        assertTrue(code.contains("class CalculatorTest"));
        assertTrue(code.contains("calculator.calculate(0);"));
    }
}
