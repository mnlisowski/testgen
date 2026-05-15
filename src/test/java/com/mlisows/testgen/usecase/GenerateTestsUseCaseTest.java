package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.ClassAnalysisResult;
import com.mlisows.testgen.usecase.ports.CodeAnalyzer;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GenerateTestsUseCaseTest {

    @Test
    void shouldStartGenerationByAnalyzingSourceCode() {
        CodeAnalyzer codeAnalyzer = mock(CodeAnalyzer.class);
        GenerateTestsUseCase useCase = new GenerateTestsUseCase(codeAnalyzer);

        Path sourcePath = Path.of("src/main/java/Example.java");
        ClassAnalysisResult expectedResult = new ClassAnalysisResult("Example", List.of());

        when(codeAnalyzer.analyze(sourcePath)).thenReturn(expectedResult);

        ClassAnalysisResult result = useCase.execute(sourcePath);

        assertSame(expectedResult, result);
        verify(codeAnalyzer).analyze(sourcePath);
    }
}
