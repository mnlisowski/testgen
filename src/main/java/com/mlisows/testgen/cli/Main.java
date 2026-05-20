package com.mlisows.testgen.cli;

import com.mlisows.testgen.domain.ClassAnalysisResult;
import com.mlisows.testgen.domain.ClassStructure;
import com.mlisows.testgen.domain.CoverageGoal;
import com.mlisows.testgen.domain.MethodGenerationPlan;
import com.mlisows.testgen.domain.ProjectTypeIndex;
import com.mlisows.testgen.infrastructure.parser.JavaParserClassStructureAnalyzer;
import com.mlisows.testgen.infrastructure.parser.JavaParserCodeAnalyzer;
import com.mlisows.testgen.infrastructure.parser.JavaParserTypeIndexAnalyzer;
import com.mlisows.testgen.usecase.GeneratableCoverageGoalSelector;
import com.mlisows.testgen.usecase.GenerateTestsUseCase;
import com.mlisows.testgen.usecase.MethodGenerationPlanner;
import com.mlisows.testgen.usecase.ports.ClassStructureAnalyzer;
import com.mlisows.testgen.usecase.ports.CodeAnalyzer;
import com.mlisows.testgen.usecase.ports.TypeIndexAnalyzer;
import com.mlisows.testgen.infrastructure.writer.GeneratedTestFileWriter;
import com.mlisows.testgen.usecase.GenerateTestSuiteUseCase;
import com.mlisows.testgen.usecase.GeneratedTestCaseFactory;
import com.mlisows.testgen.usecase.GeneratedTestSuiteFactory;
import com.mlisows.testgen.usecase.JUnitTestWriter;
import com.mlisows.testgen.usecase.SimpleArgumentGenerator;


import java.nio.file.Path;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            System.out.println("Usage: testgen <source-file> [output-test-root]");
            return;
        }


        Path sourcePath = Path.of(args[0]);
        Path outputRoot = args.length == 2 ? Path.of(args[1]) : null;


        CodeAnalyzer codeAnalyzer = new JavaParserCodeAnalyzer();
        ClassStructureAnalyzer classStructureAnalyzer = new JavaParserClassStructureAnalyzer();
        TypeIndexAnalyzer typeIndexAnalyzer = new JavaParserTypeIndexAnalyzer();

        GenerateTestsUseCase useCase = new GenerateTestsUseCase(codeAnalyzer);
        MethodGenerationPlanner planner = new MethodGenerationPlanner();
        GeneratableCoverageGoalSelector selector = new GeneratableCoverageGoalSelector();

        ClassAnalysisResult result = useCase.execute(sourcePath);
        ClassStructure classStructure = classStructureAnalyzer.analyze(sourcePath);
        ProjectTypeIndex typeIndex = typeIndexAnalyzer.analyze(List.of(sourcePath));

        List<MethodGenerationPlan> methodPlans = planner.plan(classStructure, typeIndex);

        System.out.println("Class: " + result.getClassName());
        System.out.println();

        System.out.println("Coverage goals:");
        for (CoverageGoal goal : result.getCoverageGoals()) {
            System.out.println("- "
                    + goal.getBranchId().asString()
                    + " condition="
                    + goal.getCondition());
        }

        System.out.println();
        System.out.println("Method generation plan:");
        for (MethodGenerationPlan plan : methodPlans) {
            String status = selector.isSupportedByCurrentGenerator(plan) ? "supported" : "skipped";

            System.out.println("- "
                    + plan.getClassName()
                    + "."
                    + plan.getMethodName()
                    + " "
                    + status
                    + " requirements="
                    + plan.getRequirements());
        }

        if (outputRoot != null) {
            GenerateTestSuiteUseCase generateTestSuiteUseCase = new GenerateTestSuiteUseCase(
                    new GeneratedTestSuiteFactory(
                            new GeneratedTestCaseFactory(new SimpleArgumentGenerator()),
                            selector
                    ),
                    new JUnitTestWriter(),
                    new GeneratedTestFileWriter(outputRoot)
            );

            Path writtenPath = generateTestSuiteUseCase.execute(classStructure, methodPlans);

            System.out.println();
            System.out.println("Generated test file: " + writtenPath);
        }

    }
}
