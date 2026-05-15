package com.mlisows.testgen.cli;

import com.mlisows.testgen.domain.ClassAnalysisResult;
import com.mlisows.testgen.domain.CoverageGoal;
import com.mlisows.testgen.infrastructure.parser.JavaParserCodeAnalyzer;
import com.mlisows.testgen.usecase.GenerateTestsUseCase;
import com.mlisows.testgen.usecase.ports.CodeAnalyzer;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: testgen <source-file>");
            return;
        }

        Path sourcePath = Path.of(args[0]);

        CodeAnalyzer codeAnalyzer = new JavaParserCodeAnalyzer();
        GenerateTestsUseCase useCase = new GenerateTestsUseCase(codeAnalyzer);

        ClassAnalysisResult result = useCase.execute(sourcePath);

        System.out.println("Class: " + result.getClassName());
        System.out.println("Coverage goals:");

        for (CoverageGoal goal : result.getCoverageGoals()) {
            System.out.println("- "
                    + goal.getBranchId().asString()
                    + " condition="
                    + goal.getCondition());
        }

    }
}

