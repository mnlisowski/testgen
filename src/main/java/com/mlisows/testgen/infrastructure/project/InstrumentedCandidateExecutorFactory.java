package com.mlisows.testgen.infrastructure.project;

import com.mlisows.testgen.domain.analysis.CoverageGoal;
import com.mlisows.testgen.infrastructure.execution.JavaSourceCompiler;
import com.mlisows.testgen.infrastructure.execution.ReflectionTestCandidateExecutor;
import com.mlisows.testgen.infrastructure.instrumentation.JavaParserBranchInstrumenter;
import com.mlisows.testgen.infrastructure.instrumentation.SourceDirectoryInstrumenter;
import com.mlisows.testgen.usecase.ports.CandidateExecutorFactory;
import com.mlisows.testgen.usecase.ports.TestCandidateExecutor;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class InstrumentedCandidateExecutorFactory implements CandidateExecutorFactory {
    @Override
    public TestCandidateExecutor prepare(Path projectRoot, Path workRoot, Map<Path, List<CoverageGoal>> goals) {
        MavenProjectLayout layout = new MavenProjectLayoutDetector().detect(projectRoot);
        String classpath = new MavenProjectClasspath().resolve(layout);
        var preparer = new InstrumentedProjectWorkspacePreparer(
                new SourceDirectoryInstrumenter(new JavaParserBranchInstrumenter()),
                new JavaSourceCompiler(classpath), classpath);
        return new ReflectionTestCandidateExecutor(preparer.prepare(layout, workRoot, goals));
    }
}
