package com.mlisows.testgen.domain;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BranchWeightProfileTest {

    @Test
    void shouldFindWeightForBranchId() {
        BranchId branchId = new BranchId(
                "sample.Calculator",
                "calculate",
                6,
                BranchKind.IF,
                BranchType.TRUE,
                ""
        );

        BranchWeightProfile profile = new BranchWeightProfile(Map.of(
                branchId.asString(), new BranchWeight(10.0)
        ));

        Optional<BranchWeight> weight = profile.findWeight(branchId);

        assertTrue(weight.isPresent());
        assertEquals(10.0, weight.get().getValue());
    }

    @Test
    void shouldReturnEmptyWhenBranchIdIsUnknown() {
        BranchWeightProfile profile = new BranchWeightProfile(Map.of());

        BranchId branchId = new BranchId(
                "sample.Calculator",
                "calculate",
                6,
                BranchKind.IF,
                BranchType.TRUE,
                ""
        );

        Optional<BranchWeight> weight = profile.findWeight(branchId);

        assertTrue(weight.isEmpty());
    }
}
