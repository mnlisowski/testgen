package com.mlisows.testgen.infrastructure.parser;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.mlisows.testgen.domain.ProjectTypeIndex;
import com.mlisows.testgen.domain.TypeInfo;
import com.mlisows.testgen.domain.TypeKind;
import com.mlisows.testgen.usecase.ports.TypeIndexAnalyzer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JavaParserTypeIndexAnalyzer implements TypeIndexAnalyzer {

    @Override
    public ProjectTypeIndex analyze(List<Path> sourcePaths) {
        List<TypeInfo> types = new ArrayList<>();

        for (Path sourcePath : sourcePaths) {
            CompilationUnit compilationUnit = parseSource(sourcePath);
            types.addAll(extractTypes(compilationUnit));
        }

        return new ProjectTypeIndex(types);
    }

    private CompilationUnit parseSource(Path sourcePath) {
        try {
            return StaticJavaParser.parse(sourcePath);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Cannot parse source file: " + sourcePath, exception);
        }
    }

    private List<TypeInfo> extractTypes(CompilationUnit compilationUnit) {
        List<TypeInfo> types = new ArrayList<>();

        for (ClassOrInterfaceDeclaration declaration : compilationUnit.findAll(ClassOrInterfaceDeclaration.class)) {
            TypeKind kind = declaration.isInterface() ? TypeKind.INTERFACE : TypeKind.CLASS;

            types.add(new TypeInfo(
                    declaration.getNameAsString(),
                    declaration.getFullyQualifiedName().orElse(declaration.getNameAsString()),
                    kind
            ));
        }

        for (EnumDeclaration declaration : compilationUnit.findAll(EnumDeclaration.class)) {
            types.add(new TypeInfo(
                    declaration.getNameAsString(),
                    declaration.getFullyQualifiedName().orElse(declaration.getNameAsString()),
                    TypeKind.ENUM
            ));
        }

        return types;
    }


}
