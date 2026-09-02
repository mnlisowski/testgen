package com.mlisows.testgen.infrastructure.instrumentation;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithParameters;
import com.github.javaparser.ast.stmt.Statement;
import com.mlisows.testgen.domain.CoverageGoal;
import com.mlisows.testgen.infrastructure.parser.JavaParserLanguageLevel;
import com.mlisows.testgen.usecase.ports.SourceInstrumenter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class JavaParserArgumentInstrumenter implements SourceInstrumenter {
    private static final String RECORDER_CLASS =
            "com.mlisows.testgen.infrastructure.runtime.ArgumentRecorder";

    @Override
    public boolean shouldInstrument(Path sourcePath, List<CoverageGoal> coverageGoals) {
        return true;
    }

    @Override
    public Path instrument(Path sourcePath, Path outputPath, List<CoverageGoal> coverageGoals) {
        return instrument(sourcePath, outputPath);
    }

    public Path instrument(Path sourcePath, Path outputPath) {
        Objects.requireNonNull(sourcePath, "sourcePath must not be null");
        Objects.requireNonNull(outputPath, "outputPath must not be null");

        CompilationUnit compilationUnit = parseSource(sourcePath);

        findClassMatchingFileName(compilationUnit, sourcePath)
                .ifPresent(type -> addArgumentRecording(compilationUnit, type));

        writeSource(compilationUnit, outputPath);
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

    private Optional<TypeDeclaration<?>> findClassMatchingFileName(
            CompilationUnit compilationUnit,
            Path sourcePath
    ) {
        String expectedClassName = fileNameWithoutExtension(sourcePath);

        return compilationUnit.getTypes().stream()
                .filter(type -> type.getNameAsString().equals(expectedClassName))
                .findFirst();
    }

    private String fileNameWithoutExtension(Path sourcePath) {
        String fileName = sourcePath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex == -1) {
            return fileName;
        }

        return fileName.substring(0, dotIndex);
    }

    private void addArgumentRecording(CompilationUnit compilationUnit, TypeDeclaration<?> type) {
        String className = qualifiedClassName(compilationUnit, type);

        if (className.equals(RECORDER_CLASS)) {
            return;
        }

        for (BodyDeclaration<?> member : type.getMembers()) {
            if (member.isConstructorDeclaration()) {
                addRecorderToConstructor(className, member.asConstructorDeclaration());
            }

            if (member.isMethodDeclaration()) {
                addRecorderToMethod(className, member.asMethodDeclaration());
            }
        }
    }

    private String qualifiedClassName(CompilationUnit compilationUnit, TypeDeclaration<?> type) {
        return compilationUnit.getPackageDeclaration()
                .map(packageDeclaration -> packageDeclaration.getNameAsString() + "." + type.getNameAsString())
                .orElse(type.getNameAsString());
    }

    private void addRecorderToConstructor(String className, ConstructorDeclaration constructor) {
        Statement recorderCall = recorderCall(className + ".<init>", constructor);
        int insertIndex = constructorRecorderInsertIndex(constructor);

        constructor.getBody().addStatement(insertIndex, recorderCall);
    }

    private int constructorRecorderInsertIndex(ConstructorDeclaration constructor) {
        if (constructor.getBody().getStatements().isEmpty()) {
            return 0;
        }

        Statement firstStatement = constructor.getBody().getStatement(0);

        if (firstStatement.isExplicitConstructorInvocationStmt()) {
            return 1;
        }

        return 0;
    }

    private void addRecorderToMethod(String className, MethodDeclaration method) {
        method.getBody().ifPresent(body -> body.addStatement(
                0,
                recorderCall(className + "." + method.getNameAsString(), method)
        ));
    }

    private Statement recorderCall(String ownerId, NodeWithParameters<?> executable) {
        String code = RECORDER_CLASS
                + ".record("
                + javaString(ownerId)
                + ", "
                + stringArray(parameterNames(executable))
                + ", "
                + stringArray(parameterTypes(executable))
                + argumentValuesSuffix(executable)
                + ");";

        return StaticJavaParser.parseStatement(code);
    }

    private List<String> parameterNames(NodeWithParameters<?> executable) {
        return executable.getParameters().stream()
                .map(Parameter::getNameAsString)
                .toList();
    }

    private List<String> parameterTypes(NodeWithParameters<?> executable) {
        return executable.getParameters().stream()
                .map(Parameter::getTypeAsString)
                .toList();
    }

    private String stringArray(List<String> values) {
        return values.stream()
                .map(this::javaString)
                .collect(Collectors.joining(", ", "new String[]{", "}"));
    }

    private String argumentValuesSuffix(NodeWithParameters<?> executable) {
        if (executable.getParameters().isEmpty()) {
            return "";
        }

        String argumentValues = executable.getParameters().stream()
                .map(Parameter::getNameAsString)
                .collect(Collectors.joining(", "));

        return ", " + argumentValues;
    }

    private String javaString(String value) {
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                + "\"";
    }

    private void writeSource(CompilationUnit compilationUnit, Path outputPath) {
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
}
