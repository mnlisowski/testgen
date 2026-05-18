package com.mlisows.testgen.infrastructure.parser;

import com.mlisows.testgen.domain.ClassStructure;
import com.mlisows.testgen.domain.ConstructorModel;
import com.mlisows.testgen.domain.MethodModel;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaParserClassStructureAnalyzerTest {

    @Test
    void shouldAnalyzeConstructorsAndMethods() {
        JavaParserClassStructureAnalyzer analyzer = new JavaParserClassStructureAnalyzer();

        ClassStructure structure = analyzer.analyze(Path.of(
                "src/test/resources/sample/StructureExample.java"
        ));

        assertEquals("sample.StructureExample", structure.getClassName());
        assertEquals(2, structure.getConstructors().size());
        assertEquals(2, structure.getMethods().size());

        ConstructorModel constructorWithParameter = structure.getConstructors().get(1);
        assertTrue(constructorWithParameter.isPublicConstructor());
        assertEquals(1, constructorWithParameter.getParameters().size());
        assertEquals("prefix", constructorWithParameter.getParameters().get(0).getName());
        assertEquals("String", constructorWithParameter.getParameters().get(0).getType());

        MethodModel formatMethod = structure.getMethods().stream()
                .filter(method -> method.getName().equals("format"))
                .findFirst()
                .orElseThrow();

        assertTrue(formatMethod.isPublicMethod());
        assertEquals("String", formatMethod.getReturnType());
        assertEquals(2, formatMethod.getParameters().size());
        assertEquals("value", formatMethod.getParameters().get(0).getName());
        assertEquals("String", formatMethod.getParameters().get(0).getType());
        assertEquals("count", formatMethod.getParameters().get(1).getName());
        assertEquals("int", formatMethod.getParameters().get(1).getType());
    }
}
