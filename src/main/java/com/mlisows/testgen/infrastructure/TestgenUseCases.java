package com.mlisows.testgen.infrastructure;

import com.mlisows.testgen.infrastructure.parser.JavaParserClassStructureAnalyzer;
import com.mlisows.testgen.infrastructure.parser.JavaParserCodeAnalyzer;
import com.mlisows.testgen.infrastructure.parser.JavaParserTypeIndexAnalyzer;
import com.mlisows.testgen.infrastructure.project.InstrumentedCandidateExecutorFactory;
import com.mlisows.testgen.infrastructure.project.InstrumentedProfileWorkspacePreparer;
import com.mlisows.testgen.infrastructure.project.MavenProjectSourceLocator;
import com.mlisows.testgen.usecase.generation.GenerateTestsUseCase;
import com.mlisows.testgen.usecase.profile.PrepareProfileUseCase;

/** Default wiring for local Maven projects. Core use cases depend only on ports. */
public final class TestgenUseCases {
    private TestgenUseCases() { }

    public static GenerateTestsUseCase generation() {
        return new GenerateTestsUseCase(new MavenProjectSourceLocator(), new JavaParserTypeIndexAnalyzer(),
                new JavaParserCodeAnalyzer(), new JavaParserClassStructureAnalyzer(),
                new InstrumentedCandidateExecutorFactory());
    }

    public static PrepareProfileUseCase profilePreparation() {
        return new PrepareProfileUseCase(new InstrumentedProfileWorkspacePreparer());
    }
}
