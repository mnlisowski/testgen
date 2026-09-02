package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.BranchId;
import com.mlisows.testgen.domain.BranchKind;
import com.mlisows.testgen.domain.BranchType;
import com.mlisows.testgen.domain.ClassStructure;
import com.mlisows.testgen.domain.ConstructorModel;
import com.mlisows.testgen.domain.MethodModel;
import com.mlisows.testgen.domain.ParameterModel;
import com.mlisows.testgen.domain.ProjectClassStructureIndex;
import com.mlisows.testgen.domain.ProjectTypeIndex;
import com.mlisows.testgen.domain.ArgumentValueHint;
import com.mlisows.testgen.domain.TestCandidate;
import com.mlisows.testgen.domain.TestCandidateExecutionResult;
import com.mlisows.testgen.usecase.ports.TestCandidateExecutor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CandidateGenerationUseCaseTest {

    @Test
    void shouldGenerateVariantsAndKeepOnlyCoverageImprovingResults() {
        MethodModel method = new MethodModel(
                "calculate",
                "int",
                true,
                List.of(new ParameterModel("amount", "int"))
        );
        ClassStructure calculator = new ClassStructure(
                "sample.Calculator",
                List.of(new ConstructorModel(true, List.of())),
                List.of(method)
        );

        CandidateGenerationUseCase useCase = new CandidateGenerationUseCase(
                new CandidateSpaceFactory(),
                new CandidateVariantGenerator(3, 5),
                new CandidateCoverageEvaluator(new BranchByAmountExecutor())
        );

        List<TestCandidateExecutionResult> results = useCase.execute(
                calculator,
                method,
                new ProjectTypeIndex(List.of()),
                new ProjectClassStructureIndex(List.of(calculator)),
                List.of(new ArgumentValueHint(
                        "sample.Calculator.calculate",
                        "amount",
                        "int",
                        "101",
                        "direct-static-condition"
                ))
        );

        List<String> coveredBranchIds = results.stream()
                .flatMap(result -> result.getCoveredBranches().stream())
                .map(BranchId::asString)
                .toList();

        assertEquals(2, results.size());
        assertTrue(coveredBranchIds.contains(branch(BranchType.TRUE).asString()));
        assertTrue(coveredBranchIds.contains(branch(BranchType.FALSE).asString()));
    }


    @Test
    void shouldExposeFullCandidateEvaluation() {
        MethodModel method = new MethodModel(
                "calculate",
                "int",
                true,
                List.of(new ParameterModel("amount", "int"))
        );
        ClassStructure calculator = new ClassStructure(
                "sample.Calculator",
                List.of(new ConstructorModel(true, List.of())),
                List.of(method)
        );

        CandidateGenerationUseCase useCase = new CandidateGenerationUseCase(
                new CandidateSpaceFactory(),
                new CandidateVariantGenerator(3, 5),
                new CandidateCoverageEvaluator(new BranchByAmountExecutor())
        );

        CandidateCoverageEvaluation evaluation = useCase.evaluate(
                calculator,
                method,
                new ProjectTypeIndex(List.of()),
                new ProjectClassStructureIndex(List.of(calculator)),
                List.of(new ArgumentValueHint(
                        "sample.Calculator.calculate",
                        "amount",
                        "int",
                        "101",
                        "direct-static-condition"
                ))
        );

        assertEquals(4, evaluation.getExecutedCandidateCount());
        assertEquals(2, evaluation.getSelectedCandidateCount());
    }

    @Test
    void shouldReturnEmptyWhenCandidateSpaceCannotBeCreated() {
        MethodModel method = new MethodModel(
                "calculate",
                "int",
                true,
                List.of(new ParameterModel("items", "List<String>"))
        );
        ClassStructure calculator = new ClassStructure(
                "sample.Calculator",
                List.of(new ConstructorModel(true, List.of())),
                List.of(method)
        );

        CandidateGenerationUseCase useCase = new CandidateGenerationUseCase(
                new CandidateSpaceFactory(),
                new CandidateVariantGenerator(),
                new CandidateCoverageEvaluator(candidate -> {
                    throw new AssertionError("Executor should not be called");
                })
        );

        List<TestCandidateExecutionResult> results = useCase.execute(
                calculator,
                method,
                new ProjectTypeIndex(List.of()),
                new ProjectClassStructureIndex(List.of(calculator)),
                List.of()
        );

        assertTrue(results.isEmpty());
    }

    private static BranchId branch(BranchType branchType) {
        return new BranchId(
                "sample.Calculator",
                "calculate",
                10,
                BranchKind.IF,
                branchType,
                ""
        );
    }

    private static final class BranchByAmountExecutor implements TestCandidateExecutor {
        @Override
        public TestCandidateExecutionResult execute(TestCandidate candidate) {
            int amount = Integer.parseInt(candidate.getMethodArguments().get(0).getValue());
            BranchType branchType = amount > 100 ? BranchType.TRUE : BranchType.FALSE;

            return TestCandidateExecutionResult.returned(
                    candidate,
                    List.of(branch(branchType)),
                    String.valueOf(amount)
            );
        }
    }
}
