package com.mlisows.testgen.infrastructure.parser;

import com.github.javaparser.Position;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.mlisows.testgen.domain.*;
import com.mlisows.testgen.domain.analysis.BranchId;
import com.mlisows.testgen.domain.analysis.BranchKind;
import com.mlisows.testgen.domain.analysis.BranchType;
import com.mlisows.testgen.domain.analysis.ClassAnalysisResult;
import com.mlisows.testgen.domain.analysis.CoverageGoal;
import com.mlisows.testgen.usecase.ports.CodeAnalyzer;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
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
        List<ArgumentValueHint> staticArgumentValueHints = new ArrayList<>();
        List<MethodDeclaration> methods = classDeclaration.getMethods();

        for (MethodDeclaration method : methods) {
            collectIfGoals(className, method, coverageGoals);
            collectForGoals(className, method, coverageGoals);
            collectWhileGoals(className, method, coverageGoals);
            collectSwitchGoals(className, method, coverageGoals);
            collectNumericStaticArgumentValueHints(className, method, staticArgumentValueHints);
            collectStringStaticArgumentValueHints(className, method, staticArgumentValueHints);
            collectSwitchStaticArgumentValueHints(className, method, staticArgumentValueHints);
        }

        return new ClassAnalysisResult(className, coverageGoals, staticArgumentValueHints);
    }

    private CompilationUnit parseSource(Path sourcePath) {
        try {
            JavaParserLanguageLevel.configure();
            return StaticJavaParser.parse(sourcePath);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Cannot parse source file: " + sourcePath, exception);
        }
    }

    private ClassOrInterfaceDeclaration findClassDeclaration(CompilationUnit compilationUnit, Path sourcePath) {
        List<ClassOrInterfaceDeclaration> topLevelClasses = compilationUnit.findAll(ClassOrInterfaceDeclaration.class)
                .stream()
                .filter(this::isTopLevel)
                .toList();

        if (topLevelClasses.isEmpty()) {
            throw new IllegalArgumentException("No class found in source file: " + sourcePath);
        }

        String fileName = sourcePath.getFileName().toString().replaceFirst("\\.java$", "");

        return topLevelClasses.stream()
                .filter(classDeclaration -> classDeclaration.getNameAsString().equals(fileName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No top-level class matching file name found in source file: " + sourcePath
                ));
    }

    private boolean isTopLevel(ClassOrInterfaceDeclaration classDeclaration) {
        return classDeclaration.findAncestor(ClassOrInterfaceDeclaration.class).isEmpty();
    }

    private void collectIfGoals(String className, MethodDeclaration method, List<CoverageGoal> coverageGoals) {
        String methodName = methodNameWithParameterTypes(method);
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
        String methodName = methodNameWithParameterTypes(method);
        List<WhileStmt> whileStatements = method.findAll(WhileStmt.class);

        for (WhileStmt whileStatement : whileStatements) {
            int lineNumber = getLineNumber(whileStatement);
            String condition = whileStatement.getCondition().toString();

            BranchId trueBranchId = new BranchId(className, methodName, lineNumber, BranchKind.WHILE, BranchType.TRUE, "");
            CoverageGoal trueGoal = new CoverageGoal(trueBranchId, condition);
            coverageGoals.add(trueGoal);

            BranchId falseBranchId = new BranchId(className, methodName, lineNumber, BranchKind.WHILE, BranchType.FALSE, "");
            CoverageGoal falseGoal = new CoverageGoal(falseBranchId, condition);
            coverageGoals.add(falseGoal);
        }
    }

    private void collectForGoals(String className, MethodDeclaration method, List<CoverageGoal> coverageGoals) {
        String methodName = methodNameWithParameterTypes(method);
        List<ForStmt> forStatements = method.findAll(ForStmt.class);

        for (ForStmt forStatement : forStatements) {
            int lineNumber = getLineNumber(forStatement);
            String condition = forStatement.getCompare()
                    .map(Object::toString)
                    .orElse("");

            BranchId trueBranchId = new BranchId(className, methodName, lineNumber, BranchKind.FOR, BranchType.TRUE, "");
            CoverageGoal trueGoal = new CoverageGoal(trueBranchId, condition);
            coverageGoals.add(trueGoal);

            BranchId falseBranchId = new BranchId(className, methodName, lineNumber, BranchKind.FOR, BranchType.FALSE, "");
            CoverageGoal falseGoal = new CoverageGoal(falseBranchId, condition);
            coverageGoals.add(falseGoal);
        }
    }

    private void collectSwitchGoals(String className, MethodDeclaration method, List<CoverageGoal> coverageGoals) {
        String methodName = methodNameWithParameterTypes(method);
        List<SwitchStmt> switchStatements = method.findAll(SwitchStmt.class);

        for (SwitchStmt switchStatement : switchStatements) {
            int lineNumber = getLineNumber(switchStatement);
            String selector = switchStatement.getSelector().toString();

            for (SwitchEntry entry : switchStatement.getEntries()) {
                if (entry.getLabels().isEmpty()) {
                    BranchId defaultBranchId = new BranchId(
                            className,
                            methodName,
                            lineNumber,
                            BranchKind.SWITCH,
                            BranchType.DEFAULT,
                            ""
                    );

                    CoverageGoal defaultGoal = new CoverageGoal(defaultBranchId, selector);
                    coverageGoals.add(defaultGoal);
                } else {
                    String discriminator = switchLabelDiscriminator(entry.getLabels().get(0));

                    BranchId caseBranchId = new BranchId(
                            className,
                            methodName,
                            lineNumber,
                            BranchKind.SWITCH,
                            BranchType.CASE,
                            discriminator
                    );

                    CoverageGoal caseGoal = new CoverageGoal(caseBranchId, selector);
                    coverageGoals.add(caseGoal);
                }
            }
        }
    }

    private String switchLabelDiscriminator(Expression expression) {
        if (expression.isStringLiteralExpr()) {
            return expression.asStringLiteralExpr().asString();
        }

        return expression.toString();
    }

    private void collectNumericStaticArgumentValueHints(
            String className,
            MethodDeclaration method,
            List<ArgumentValueHint> hints
    ) {
        for (IfStmt ifStatement : method.findAll(IfStmt.class)) {
            collectNumericExpressionHints(className, method, ifStatement.getCondition(), hints);
        }
    }

    private void collectNumericExpressionHints(
            String className,
            MethodDeclaration method,
            Expression expression,
            List<ArgumentValueHint> hints
    ) {
        if (!expression.isBinaryExpr()) {
            return;
        }

        BinaryExpr binaryExpression = expression.asBinaryExpr();

        addNumericHintsForParameterAndLiteral(
                className,
                method,
                binaryExpression.getLeft(),
                binaryExpression.getRight(),
                binaryExpression.getOperator(),
                hints
        );
        addNumericHintsForParameterAndLiteral(
                className,
                method,
                binaryExpression.getRight(),
                binaryExpression.getLeft(),
                binaryExpression.getOperator(),
                hints
        );

        collectNumericExpressionHints(className, method, binaryExpression.getLeft(), hints);
        collectNumericExpressionHints(className, method, binaryExpression.getRight(), hints);
    }

    private void addNumericHintsForParameterAndLiteral(
            String className,
            MethodDeclaration method,
            Expression possibleParameter,
            Expression possibleLiteral,
            BinaryExpr.Operator operator,
            List<ArgumentValueHint> hints
    ) {
        Optional<Parameter> parameter = directParameterReference(possibleParameter, method);

        if (parameter.isEmpty() || !isIntegerLikeType(parameter.get().getType().asString())) {
            return;
        }

        Optional<Long> literalValue = integerLiteralValue(possibleLiteral);

        if (literalValue.isEmpty() || !isComparisonOperator(operator)) {
            return;
        }

        for (String value : valuesForIntegerComparison(operator, literalValue.get())) {
            addHintIfMissing(
                    hints,
                    new ArgumentValueHint(
                            methodSignature(className, method),
                            parameter.get().getNameAsString(),
                            parameter.get().getType().asString(),
                            value,
                            "direct-static-condition"
                    )

            );
        }
    }

    private void collectStringStaticArgumentValueHints(
            String className,
            MethodDeclaration method,
            List<ArgumentValueHint> hints
    ) {
        for (IfStmt ifStatement : method.findAll(IfStmt.class)) {
            collectStringExpressionHints(className, method, ifStatement.getCondition(), hints);
        }
    }

    private void collectStringExpressionHints(
            String className,
            MethodDeclaration method,
            Expression expression,
            List<ArgumentValueHint> hints
    ) {
        if (expression.isBinaryExpr()) {
            BinaryExpr binaryExpression = expression.asBinaryExpr();
            collectStringExpressionHints(className, method, binaryExpression.getLeft(), hints);
            collectStringExpressionHints(className, method, binaryExpression.getRight(), hints);
            return;
        }

        if (expression.isMethodCallExpr()) {
            collectMethodCallStringHints(className, method, expression.asMethodCallExpr(), hints);
        }
    }

    private void collectMethodCallStringHints(
            String className,
            MethodDeclaration method,
            MethodCallExpr methodCall,
            List<ArgumentValueHint> hints
    ) {
        if (!isStringValueHintMethod(methodCall.getNameAsString()) || methodCall.getScope().isEmpty()) {
            return;
        }

        Expression scope = methodCall.getScope().get();
        Optional<Parameter> scopedParameter = directParameterReference(scope, method);

        if (scopedParameter.isPresent()) {
            for (Expression argument : methodCall.getArguments()) {
                if (isSupportedLiteral(argument)) {
                    addHintIfMissing(
                            hints,
                            hint(className, method, scopedParameter.get(), argument.toString(), "direct-static-call")
                    );
                }
            }
            return;
        }

        if (isEqualsMethod(methodCall.getNameAsString()) && isSupportedLiteral(scope)) {
            methodCall.getArguments().stream()
                    .map(argument -> directParameterReference(argument, method))
                    .flatMap(Optional::stream)
                    .forEach(parameter -> addHintIfMissing(
                            hints,
                            hint(className, method, parameter, scope.toString(), "direct-static-call")
                    ));
        }
    }

    private void collectSwitchStaticArgumentValueHints(
            String className,
            MethodDeclaration method,
            List<ArgumentValueHint> hints
    ) {
        for (SwitchStmt switchStatement : method.findAll(SwitchStmt.class)) {
            Optional<Parameter> selectorParameter = directParameterReference(switchStatement.getSelector(), method);

            if (selectorParameter.isEmpty()) {
                continue;
            }

            for (SwitchEntry entry : switchStatement.getEntries()) {
                for (Expression label : entry.getLabels()) {
                    if (isSupportedLiteral(label)) {
                        addHintIfMissing(
                                hints,
                                hint(
                                        className,
                                        method,
                                        selectorParameter.get(),
                                        label.toString(),
                                        "static-switch-case"
                                )
                        );
                    }
                }
            }
        }
    }

    private Optional<Parameter> directParameterReference(Expression expression, MethodDeclaration method) {
        if (!expression.isNameExpr()) {
            return Optional.empty();
        }

        String name = expression.asNameExpr().getNameAsString();

        return method.getParameters().stream()
                .filter(parameter -> parameter.getNameAsString().equals(name))
                .findFirst();
    }

    private Optional<Long> integerLiteralValue(Expression expression) {
        if (!expression.isIntegerLiteralExpr() && !expression.isLongLiteralExpr()) {
            return Optional.empty();
        }

        try {
            String literal = expression.toString()
                    .replace("_", "")
                    .replaceAll("[lL]$", "");

            return Optional.of(Long.parseLong(literal));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private boolean isComparisonOperator(BinaryExpr.Operator operator) {
        return operator == BinaryExpr.Operator.GREATER
                || operator == BinaryExpr.Operator.GREATER_EQUALS
                || operator == BinaryExpr.Operator.LESS
                || operator == BinaryExpr.Operator.LESS_EQUALS
                || operator == BinaryExpr.Operator.EQUALS
                || operator == BinaryExpr.Operator.NOT_EQUALS;
    }

    private List<String> valuesForIntegerComparison(BinaryExpr.Operator operator, long value) {
        if (isOrderedComparison(operator)) {
            return List.of(
                    Long.toString(value - 1),
                    Long.toString(value),
                    Long.toString(value + 1)
            );
        }

        return List.of(Long.toString(value));
    }

    private boolean isOrderedComparison(BinaryExpr.Operator operator) {
        return operator == BinaryExpr.Operator.GREATER
                || operator == BinaryExpr.Operator.GREATER_EQUALS
                || operator == BinaryExpr.Operator.LESS
                || operator == BinaryExpr.Operator.LESS_EQUALS;
    }

    private boolean isIntegerLikeType(String type) {
        return type.equals("int")
                || type.equals("long")
                || type.equals("Integer")
                || type.equals("Long")
                || type.equals("java.lang.Integer")
                || type.equals("java.lang.Long");
    }

    private boolean isSupportedLiteral(Expression expression) {
        return expression.isLiteralExpr() && !expression.isNullLiteralExpr();
    }

    private boolean isStringValueHintMethod(String methodName) {
        return isEqualsMethod(methodName)
                || methodName.equals("contains")
                || methodName.equals("startsWith")
                || methodName.equals("endsWith");
    }

    private boolean isEqualsMethod(String methodName) {
        return methodName.equals("equals") || methodName.equals("equalsIgnoreCase");
    }

    private ArgumentValueHint hint(
            String className,
            MethodDeclaration method,
            Parameter parameter,
            String value,
            String source
    ) {
        return new ArgumentValueHint(
                methodSignature(className, method),
                parameter.getNameAsString(),
                parameter.getType().asString(),
                value,
                source
        );
    }

    private String methodSignature(String className, MethodDeclaration method) {
        return className + "." + methodNameWithParameterTypes(method);
    }

    private String methodNameWithParameterTypes(MethodDeclaration method) {
        List<String> parameterTypes = method.getParameters().stream()
                .map(parameter -> parameter.getType().asString())
                .toList();

        return ExecutableSignature.methodName(method.getNameAsString(), parameterTypes);
    }

    private void addHintIfMissing(
            List<ArgumentValueHint> hints,
            ArgumentValueHint candidate
    ) {
        boolean alreadyExists = hints.stream()
                .anyMatch(hint -> hint.slotId().equals(candidate.slotId())
                        && hint.getType().equals(candidate.getType())
                        && hint.getValue().equals(candidate.getValue()));

        if (!alreadyExists) {
            hints.add(candidate);
        }
  }
}
