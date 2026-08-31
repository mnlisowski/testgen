package com.mlisows.testgen.infrastructure.instrumentation;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.mlisows.testgen.domain.BranchId;
import com.mlisows.testgen.domain.BranchKind;
import com.mlisows.testgen.domain.BranchType;
import com.mlisows.testgen.domain.CoverageGoal;
import com.mlisows.testgen.usecase.ports.SourceInstrumenter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class JavaParserBranchInstrumenter implements SourceInstrumenter {

    @Override
    public Path instrument(Path sourcePath, Path outputPath, List<CoverageGoal> coverageGoals) {
        Objects.requireNonNull(sourcePath, "sourcePath must not be null");
        Objects.requireNonNull(outputPath, "outputPath must not be null");
        Objects.requireNonNull(coverageGoals, "coverageGoals must not be null");

        CompilationUnit compilationUnit = parseSource(sourcePath);

        new IfBranchInstrumentationVisitor(coverageGoals).visit(compilationUnit, null);

        writeInstrumentedSource(compilationUnit, outputPath);
        return outputPath;
    }

    private CompilationUnit parseSource(Path sourcePath) {
        try {
            return StaticJavaParser.parse(sourcePath);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Cannot parse source file: " + sourcePath, exception);
        }
    }

    private void writeInstrumentedSource(CompilationUnit compilationUnit, Path outputPath) {
        try {
            Path parent = outputPath.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.writeString(outputPath, compilationUnit.toString());
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write instrumented source file: " + outputPath, exception);
        }
    }

    private static final class IfBranchInstrumentationVisitor extends ModifierVisitor<Void> {
        private final List<CoverageGoal> coverageGoals;

        private IfBranchInstrumentationVisitor(List<CoverageGoal> coverageGoals) {
            this.coverageGoals = coverageGoals;
        }

        @Override
        public Visitable visit(IfStmt ifStatement, Void argument) {
            super.visit(ifStatement, argument);

            Optional<MethodDeclaration> method = ifStatement.findAncestor(MethodDeclaration.class);

            if (method.isEmpty()) {
                return ifStatement;
            }

            int lineNumber = ifStatement.getBegin()
                    .orElseThrow(() -> new IllegalStateException("If statement has no source position"))
                    .line;

            Optional<CoverageGoal> trueGoal = findGoal(
                    method.get().getNameAsString(),
                    lineNumber,
                    BranchType.TRUE
            );

            Optional<CoverageGoal> falseGoal = findGoal(
                    method.get().getNameAsString(),
                    lineNumber,
                    BranchType.FALSE
            );

            if (trueGoal.isEmpty() || falseGoal.isEmpty()) {
                return ifStatement;
            }

            ifStatement.setThenStmt(withHitFirst(
                    ifStatement.getThenStmt(),
                    trueGoal.get().getBranchId().asString()
            ));

            Statement elseStatement = ifStatement.getElseStmt()
                    .map(statement -> withHitFirst(
                            statement,
                            falseGoal.get().getBranchId().asString()
                    ))
                    .orElseGet(() -> blockWithHit(falseGoal.get().getBranchId().asString()));

            ifStatement.setElseStmt(elseStatement);

            return ifStatement;
        }

        private Optional<CoverageGoal> findGoal(String methodName, int lineNumber, BranchType branchType) {
            return coverageGoals.stream()
                    .filter(goal -> matchesGoal(goal.getBranchId(), methodName, lineNumber, branchType))
                    .findFirst();
        }

        private boolean matchesGoal(
                BranchId branchId,
                String methodName,
                int lineNumber,
                BranchType branchType
        ) {
            return branchId.getMethodName().equals(methodName)
                    && branchId.getLineNumber() == lineNumber
                    && branchId.getBranchKind() == BranchKind.IF
                    && branchId.getBranchType() == branchType;
        }

        private Statement withHitFirst(Statement statement, String branchId) {
            BlockStmt block;

            if (statement.isBlockStmt()) {
                block = statement.asBlockStmt();
            } else {
                block = new BlockStmt();
                block.addStatement(statement.clone());
            }

            block.addStatement(0, hitStatement(branchId));
            return block;
        }

        private BlockStmt blockWithHit(String branchId) {
            BlockStmt block = new BlockStmt();
            block.addStatement(hitStatement(branchId));
            return block;
        }

        private Statement hitStatement(String branchId) {
            Expression recorderClass = StaticJavaParser.parseExpression(
                    "com.mlisows.testgen.infrastructure.runtime.BranchRecorder"
            );

            MethodCallExpr hitCall = new MethodCallExpr(recorderClass, "hit");
            hitCall.addArgument(new StringLiteralExpr(branchId));

            return new ExpressionStmt(hitCall);
        }
    }
}
