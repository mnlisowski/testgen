package com.mlisows.testgen.infrastructure.execution;

import com.mlisows.testgen.domain.BranchId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RecordedBranchCoverageResolverTest {
    private final RecordedBranchCoverageResolver resolver = new RecordedBranchCoverageResolver();

    @Test
    void shouldResolveRecordedBranchIds() {
        List<BranchId> branches = resolver.resolve(Map.of(
                "sample.Calculator|calculate|5|IF|FALSE", 1,
                "sample.Calculator|calculate|5|IF|TRUE", 2
        ));

        assertEquals(List.of(
                "sample.Calculator|calculate|5|IF|FALSE",
                "sample.Calculator|calculate|5|IF|TRUE"
        ), branches.stream().map(BranchId::asString).toList());
    }

    @Test
    void shouldRejectInvalidRecordedBranchId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> resolver.resolve(Map.of("sample.Calculator|calculate", 1))
        );
    }
}
