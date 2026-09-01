package com.mlisows.testgen.infrastructure.project;

import com.mlisows.testgen.domain.ClassAnalysisResult;
import com.mlisows.testgen.infrastructure.parser.JavaParserCodeAnalyzer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstrumentedProjectWorkspacePreparerTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldPrepareInstrumentedWorkspaceForMavenProject() throws Exception {
        Path projectRoot = Path.of("src/test/resources/sample-maven-project");
        MavenProjectLayout layout = new MavenProjectLayoutDetector().detect(projectRoot);
        Path workRoot = tempDir.resolve("workspace");

        Path discountServicePath = layout.getMainSourceRoot()
                .resolve("sample/maven/DiscountService.java");

        ClassAnalysisResult analysisResult = new JavaParserCodeAnalyzer()
                .analyze(discountServicePath);

        InstrumentedProjectWorkspace workspace = new InstrumentedProjectWorkspacePreparer()
                .prepare(
                        layout,
                        workRoot,
                        Map.of(discountServicePath, analysisResult.getCoverageGoals())
                );

        Path instrumentedDiscountService = workspace.getSourceRoot()
                .resolve("sample/maven/DiscountService.java");

        assertEquals(workRoot.resolve("instrumented-src"), workspace.getSourceRoot());
        assertEquals(workRoot.resolve("classes"), workspace.getClassesRoot());
        assertTrue(Files.exists(instrumentedDiscountService));
        assertTrue(Files.exists(workspace.getClassesRoot().resolve("sample/maven/DiscountService.class")));
        assertTrue(Files.exists(workspace.getClassesRoot().resolve("sample/maven/messages.txt")));
        assertTrue(Files.readString(instrumentedDiscountService).contains("BranchRecorder.hit"));
    }
}
