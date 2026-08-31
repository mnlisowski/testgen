package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.BranchId;
import com.mlisows.testgen.domain.BranchKind;
import com.mlisows.testgen.domain.BranchType;
import com.mlisows.testgen.domain.GeneratedArgument;
import com.mlisows.testgen.domain.GeneratedSetupObject;
import com.mlisows.testgen.domain.TestCandidate;
import com.mlisows.testgen.domain.TestCandidateExecutionResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CandidateArchiveTest {

    @Test
    void shouldKeepResultWhenItCoversNewBranch() {
        CandidateArchive archive = new CandidateArchive();

        TestCandidateExecutionResult result = resultCovering(branch("TRUE"));

        boolean added = archive.addIfImprovesCoverage(result);

        assertTrue(added);
        assertEquals(List.of(result), archive.getSelectedResults());
        assertTrue(archive.getCoveredBranchIds().contains(
                "sample.Calculator|calculate|10|IF|TRUE|TRUE"
        ));
    }

    @Test
    void shouldDropResultWhenItDoesNotCoverNewBranch() {
        CandidateArchive archive = new CandidateArchive();

        TestCandidateExecutionResult firstResult = resultCovering(branch("TRUE"));
        TestCandidateExecutionResult duplicateResult = resultCovering(branch("TRUE"));

        assertTrue(archive.addIfImprovesCoverage(firstResult));
        assertFalse(archive.addIfImprovesCoverage(duplicateResult));

        assertEquals(List.of(firstResult), archive.getSelectedResults());
        assertEquals(1, archive.getCoveredBranchIds().size());
    }

    @Test
    void shouldKeepResultWhenItAddsOneNewBranchAmongAlreadyCoveredBranches() {
        CandidateArchive archive = new CandidateArchive();

        TestCandidateExecutionResult firstResult = resultCovering(branch("TRUE"));
        TestCandidateExecutionResult secondResult = resultCovering(
                branch("TRUE"),
                branch("FALSE")
        );

        assertTrue(archive.addIfImprovesCoverage(firstResult));
        assertTrue(archive.addIfImprovesCoverage(secondResult));

        assertEquals(List.of(firstResult, secondResult), archive.getSelectedResults());
        assertEquals(2, archive.getCoveredBranchIds().size());
    }

    @Test
    void shouldDropResultWithNoCoveredBranches() {
        CandidateArchive archive = new CandidateArchive();

        TestCandidateExecutionResult result = resultCovering();

        assertFalse(archive.addIfImprovesCoverage(result));
        assertTrue(archive.getSelectedResults().isEmpty());
        assertTrue(archive.getCoveredBranchIds().isEmpty());
    }

    private static TestCandidateExecutionResult resultCovering(BranchId... branches) {
        return TestCandidateExecutionResult.returned(
                candidate(),
                List.of(branches),
                "10"
        );
    }

    private static TestCandidate candidate() {
        return new TestCandidate(
                "sample.Calculator",
                "calculate",
                "int",
                List.of(new GeneratedSetupObject("Calculator", "calculator", List.of())),
                "calculator",
                List.of(new GeneratedArgument("int", "-1"))
        );
    }

    private static BranchId branch(String discriminator) {
        return new BranchId(
                "sample.Calculator",
                "calculate",
                10,
                BranchKind.IF,
                BranchType.TRUE,
                discriminator
        );
    }
}
