package com.mlisows.testgen.infrastructure.instrumentation;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.mlisows.testgen.domain.BranchId;
import com.mlisows.testgen.domain.BranchKind;
import com.mlisows.testgen.domain.BranchType;
import com.mlisows.testgen.domain.CoverageGoal;
import com.mlisows.testgen.infrastructure.parser.JavaParserLanguageLevel;
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

        new BranchInstrumentationVisitor(coverageGoals).visit(compilationUnit, null);

        writeInstrumentedSource(compilationUnit, outputPath);
        return outputPath;
    }

    private CompilationUnit parseSource(Path sourcePath) {
        try {
            JavaParserLanguageLevel.configure();
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

    private static final class BranchInstrumentationVisitor extends ModifierVisitor<Void> {
        private final List<CoverageGoal> coverageGoals;

        private BranchInstrumentationVisitor(List<CoverageGoal> coverageGoals) {
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
                    BranchKind.IF,
                    BranchType.TRUE
            );

            Optional<CoverageGoal> falseGoal = findGoal(
                    method.get().getNameAsString(),
                    lineNumber,
                    BranchKind.IF,
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

        @Override
        public Visitable visit(ForStmt forStatement, Void argument) {
            super.visit(forStatement, argument);

            Optional<MethodDeclaration> method = forStatement.findAncestor(MethodDeclaration.class);

            if (method.isEmpty()) {
                return forStatement;
            }

            int lineNumber = forStatement.getBegin()
                    .orElseThrow(() -> new IllegalStateException("For statement has no source position"))
                    .line;

            Optional<CoverageGoal> trueGoal = findGoal(
                    method.get().getNameAsString(),
                    lineNumber,
                    BranchKind.FOR,
                    BranchType.TRUE
            );

            Optional<CoverageGoal> falseGoal = findGoal(
                    method.get().getNameAsString(),
                    lineNumber,
                    BranchKind.FOR,
                    BranchType.FALSE
            );

            if (trueGoal.isEmpty() || falseGoal.isEmpty()) {
                return forStatement;
            }

            forStatement.setBody(withHitFirst(
                    forStatement.getBody(),
                    trueGoal.get().getBranchId().asString()
            ));

            if (isDirectlyLabeled(forStatement)) {
                return forStatement;
            }

            return blockWithLoopAndFalseHit(
                    forStatement,
                    falseGoal.get().getBranchId().asString()
            );
        }

        @Override
        public Visitable visit(WhileStmt whileStatement, Void argument) {
            super.visit(whileStatement, argument);

            Optional<MethodDeclaration> method = whileStatement.findAncestor(MethodDeclaration.class);

            if (method.isEmpty()) {
                return whileStatement;
            }

            int lineNumber = whileStatement.getBegin()
                    .orElseThrow(() -> new IllegalStateException("While statement has no source position"))
                    .line;

            Optional<CoverageGoal> trueGoal = findGoal(
                    method.get().getNameAsString(),
                    lineNumber,
                    BranchKind.WHILE,
                    BranchType.TRUE
            );

            Optional<CoverageGoal> falseGoal = findGoal(
                    method.get().getNameAsString(),
                    lineNumber,
                    BranchKind.WHILE,
                    BranchType.FALSE
            );

            if (trueGoal.isEmpty() || falseGoal.isEmpty()) {
                return whileStatement;
            }

            whileStatement.setBody(withHitFirst(
                    whileStatement.getBody(),
                    trueGoal.get().getBranchId().asString()
            ));

            if (isDirectlyLabeled(whileStatement)) {
                return whileStatement;
            }

            return blockWithLoopAndFalseHit(
                    whileStatement,
                    falseGoal.get().getBranchId().asString()
            );
        }

        @Override
        public Visitable visit(LabeledStmt labeledStatement, Void argument) {
            super.visit(labeledStatement, argument);

            Statement statement = labeledStatement.getStatement();
            if (!statement.isForStmt() && !statement.isWhileStmt()) {
                return labeledStatement;
            }

            Optional<MethodDeclaration> method = labeledStatement.findAncestor(MethodDeclaration.class);
            if (method.isEmpty()) {
                return labeledStatement;
            }

            BranchKind branchKind = statement.isForStmt() ? BranchKind.FOR : BranchKind.WHILE;
            int lineNumber = getLineNumber(statement);

            Optional<CoverageGoal> falseGoal = findGoal(
                    method.get().getNameAsString(),
                    lineNumber,
                    branchKind,
                    BranchType.FALSE
            );

            if (falseGoal.isEmpty()) {
                return labeledStatement;
            }

            BlockStmt block = new BlockStmt();
            block.addStatement(labeledStatement.clone());
            block.addStatement(hitStatement(falseGoal.get().getBranchId().asString()));
            return block;
        }

        @Override
        public Visitable visit(SwitchStmt switchStatement, Void argument) {
            super.visit(switchStatement, argument);

            Optional<MethodDeclaration> method = switchStatement.findAncestor(MethodDeclaration.class);

            if (method.isEmpty()) {
                return switchStatement;
            }

            int lineNumber = switchStatement.getBegin()
                    .orElseThrow(() -> new IllegalStateException("Switch statement has no source position"))
                    .line;

            for (SwitchEntry entry : switchStatement.getEntries()) {
                Optional<CoverageGoal> goal = findSwitchGoal(
                        method.get().getNameAsString(),
                        lineNumber,
                        entry
                );

                goal.ifPresent(coverageGoal -> addSwitchHit(
                        entry,
                        coverageGoal.getBranchId().asString()
                ));
            }

            return switchStatement;
        }

        private Optional<CoverageGoal> findSwitchGoal(
                String methodName,
                int lineNumber,
                SwitchEntry entry
        ) {
            if (entry.getLabels().isEmpty()) {
                return findGoal(methodName, lineNumber, BranchKind.SWITCH, BranchType.DEFAULT, "");
            }

            String discriminator = switchLabelDiscriminator(entry.getLabels().get(0));
            return findGoal(methodName, lineNumber, BranchKind.SWITCH, BranchType.CASE, discriminator);

        }

        private String switchLabelDiscriminator(Expression expression) {
            if (expression.isStringLiteralExpr()) {
                return expression.asStringLiteralExpr().asString();
            }

            return expression.toString();
        }

        private void addSwitchHit(SwitchEntry entry, String branchId) {
            Statement hit = hitStatement(branchId);

            if (entry.getType() == SwitchEntry.Type.STATEMENT_GROUP) {
                entry.getStatements().add(0, hit);
                return;
            }

            if (entry.getType() == SwitchEntry.Type.BLOCK
                    && entry.getStatements().size() == 1
                    && entry.getStatements().get(0).isBlockStmt()) {
                entry.getStatements().get(0).asBlockStmt().addStatement(0, hit);
                return;
            }

            BlockStmt block = new BlockStmt();
            block.addStatement(hit);

            for (Statement statement : entry.getStatements()) {
                block.addStatement(statement.clone());
            }

            entry.getStatements().clear();
            entry.getStatements().add(block);
            entry.setType(SwitchEntry.Type.BLOCK);
        }

        private Optional<CoverageGoal> findGoal(
                String methodName,
                int lineNumber,
                BranchKind branchKind,
                BranchType branchType
        ) {
            return findGoal(methodName, lineNumber, branchKind, branchType, "");
        }

        private Optional<CoverageGoal> findGoal(
                String methodName,
                int lineNumber,
                BranchKind branchKind,
                BranchType branchType,
                String discriminator
        ) {
            return coverageGoals.stream()
                    .filter(goal -> matchesGoal(
                            goal.getBranchId(),
                            methodName,
                            lineNumber,
                            branchKind,
                            branchType,
                            discriminator
                    ))
                    .findFirst();
        }

        private boolean matchesGoal(
                BranchId branchId,
                String methodName,
                int lineNumber,
                BranchKind branchKind,
                BranchType branchType,
                String discriminator
        ) {
            return branchId.getMethodName().equals(methodName)
                    && branchId.getLineNumber() == lineNumber
                    && branchId.getBranchKind() == branchKind
                    && branchId.getBranchType() == branchType
                    && branchId.getDiscriminator().equals(discriminator);
        }

        private int getLineNumber(Node node) {
            return node.getBegin()
                    .orElseThrow(() -> new IllegalStateException("Statement has no source position"))
                    .line;
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

        private Statement blockWithLoopAndFalseHit(Statement loopStatement, String falseBranchId) {
            BlockStmt block = new BlockStmt();
            block.addStatement(loopStatement.clone());
            block.addStatement(hitStatement(falseBranchId));
            return block;
        }

        private boolean isDirectlyLabeled(Statement statement) {
            return statement.getParentNode()
                    .filter(parent -> parent instanceof LabeledStmt)
                    .isPresent();
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
