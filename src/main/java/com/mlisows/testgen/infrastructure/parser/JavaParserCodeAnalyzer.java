package com.mlisows.testgen.infrastructure.parser;

import com.github.javaparser.Position;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.mlisows.testgen.domain.BranchId;
import com.mlisows.testgen.domain.BranchType;
import com.mlisows.testgen.domain.ClassAnalysisResult;
import com.mlisows.testgen.domain.CoverageGoal;
import com.mlisows.testgen.usecase.ports.CodeAnalyzer;
import com.mlisows.testgen.domain.BranchKind;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JavaParserCodeAnalyzer implements CodeAnalyzer {

    @Override
    public ClassAnalysisResult analyze(Path sourcePath) {
        CompilationUnit compilationUnit = parseSource(sourcePath);
        ClassOrInterfaceDeclaration classDeclaration = findClassDeclaration(compilationUnit, sourcePath);

        Optional<String> fullyQualifiedName = classDeclaration.getFullyQualifiedName();
        String className;
        if (fullyQualifiedName.isPresent()) {
            className = fullyQualifiedName.get();
        } else {
            className = classDeclaration.getNameAsString();
        }


        List<CoverageGoal> coverageGoals = new ArrayList<>();
        List<MethodDeclaration> methods = classDeclaration.findAll(MethodDeclaration.class);

        for (MethodDeclaration method : methods) {
            collectIfGoals(className, method, coverageGoals);
            collectForGoals(className, method, coverageGoals);
            collectWhileGoals(className, method, coverageGoals);
        }

        return new ClassAnalysisResult(className, coverageGoals);
    }

    private CompilationUnit parseSource(Path sourcePath) {
        try {
            return StaticJavaParser.parse(sourcePath);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Cannot parse source file: " + sourcePath, exception);
        }
    }

    private ClassOrInterfaceDeclaration findClassDeclaration(CompilationUnit compilationUnit, Path sourcePath) {
        Optional<ClassOrInterfaceDeclaration> classDeclaration =
                compilationUnit.findFirst(ClassOrInterfaceDeclaration.class);

        if (classDeclaration.isEmpty()) {
            throw new IllegalArgumentException("No class found in source file: " + sourcePath);
        }

        return classDeclaration.get();
    }

    private void collectIfGoals(String className, MethodDeclaration method, List<CoverageGoal> coverageGoals) {
        String methodName = method.getNameAsString();
        List<IfStmt> ifStatements = method.findAll(IfStmt.class);

        for (IfStmt ifStatement : ifStatements) {
            int lineNumber = getLineNumber(ifStatement);
            String condition = ifStatement.getCondition().toString();

            BranchId trueBranchId = new BranchId(className, methodName, lineNumber, BranchKind.IF, BranchType.TRUE, "");

            CoverageGoal trueGoal = new CoverageGoal(trueBranchId, condition);
            coverageGoals.add(trueGoal);

            BranchId falseBranchId = new BranchId(className, methodName, lineNumber, BranchKind.IF, BranchType.FALSE, "");

            CoverageGoal falseGoal = new CoverageGoal(falseBranchId, condition);
            coverageGoals.add(falseGoal);
        }
    }

    private int getLineNumber(Node node) {
        Optional<Position> beginPosition = node.getBegin();

        if (beginPosition.isEmpty()) {
            throw new IllegalStateException("If statement has no source position");
        }

        return beginPosition.get().line;
    }

    private void collectWhileGoals(String className, MethodDeclaration method, List<CoverageGoal> coverageGoals) {
        String methodName = method.getNameAsString();
        List<WhileStmt> whileStatements = method.findAll(WhileStmt.class);

        for (WhileStmt whileStatement : whileStatements) {
            int lineNumber = getLineNumber(whileStatement);
            String condition = whileStatement.getCondition().toString();

            BranchId trueBranchId = new BranchId(className, methodName, lineNumber, BranchKind.IF, BranchType.TRUE, "");
            CoverageGoal trueGoal = new CoverageGoal(trueBranchId, condition);
            coverageGoals.add(trueGoal);

            BranchId falseBranchId = new BranchId(className, methodName, lineNumber, BranchKind.IF, BranchType.FALSE, "");
            CoverageGoal falseGoal = new CoverageGoal(falseBranchId, condition);
            coverageGoals.add(falseGoal);
        }
    }

    private void collectForGoals(String className, MethodDeclaration method, List<CoverageGoal> coverageGoals) {
        String methodName = method.getNameAsString();
        List<ForStmt> forStatements = method.findAll(ForStmt.class);

        for (ForStmt forStatement : forStatements) {
            int lineNumber = getLineNumber(forStatement);
            String condition = forStatement.getCompare()
                    .map(Object::toString)
                    .orElse("");

            BranchId trueBranchId = new BranchId(className, methodName, lineNumber, BranchKind.IF, BranchType.TRUE, "");
            CoverageGoal trueGoal = new CoverageGoal(trueBranchId, condition);
            coverageGoals.add(trueGoal);

            BranchId falseBranchId = new BranchId(className, methodName, lineNumber, BranchKind.IF, BranchType.FALSE, "");
            CoverageGoal falseGoal = new CoverageGoal(falseBranchId, condition);
            coverageGoals.add(falseGoal);
        }
    }


}
