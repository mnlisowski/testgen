package com.mlisows.testgen.infrastructure.parser;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.mlisows.testgen.domain.ClassStructure;
import com.mlisows.testgen.domain.ConstructorModel;
import com.mlisows.testgen.domain.MethodModel;
import com.mlisows.testgen.domain.ParameterModel;
import com.mlisows.testgen.usecase.ports.ClassStructureAnalyzer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JavaParserClassStructureAnalyzer implements ClassStructureAnalyzer {

    @Override
    public ClassStructure analyze(Path sourcePath) {
        CompilationUnit compilationUnit = parseSource(sourcePath);
        ClassOrInterfaceDeclaration classDeclaration = findClassDeclaration(compilationUnit, sourcePath);

        String className = resolveClassName(classDeclaration);

        List<ConstructorModel> constructors = new ArrayList<>();
        for (ConstructorDeclaration constructor : classDeclaration.getConstructors()) {
            constructors.add(new ConstructorModel(
                    constructor.isPublic(),
                    toParameters(constructor)
            ));
        }

        List<MethodModel> methods = new ArrayList<>();
        for (MethodDeclaration method : classDeclaration.getMethods()) {
            methods.add(new MethodModel(
                    method.getNameAsString(),
                    method.getType().asString(),
                    method.isPublic(),
                    toParameters(method)
            ));
        }

        return new ClassStructure(className, constructors, methods);
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

    private String resolveClassName(ClassOrInterfaceDeclaration classDeclaration) {
        Optional<String> fullyQualifiedName = classDeclaration.getFullyQualifiedName();

        if (fullyQualifiedName.isPresent()) {
            return fullyQualifiedName.get();
        }

        return classDeclaration.getNameAsString();
    }

    private List<ParameterModel> toParameters(CallableDeclaration<?> callableDeclaration) {
        List<ParameterModel> parameters = new ArrayList<>();

        for (Parameter parameter : callableDeclaration.getParameters()) {
            parameters.add(new ParameterModel(
                    parameter.getNameAsString(),
                    parameter.getType().asString()
            ));
        }

        return parameters;
    }
}
