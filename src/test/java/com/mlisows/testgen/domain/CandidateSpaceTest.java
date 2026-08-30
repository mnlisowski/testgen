package com.mlisows.testgen.domain;

import com.mlisows.testgen.domain.CandidateSpace.CandidateValuePool;
import com.mlisows.testgen.domain.CandidateSpace.CandidateValueSlot;
import com.mlisows.testgen.domain.CandidateSpace.CandidateValueSlotKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CandidateSpaceTest {

    @Test
    void shouldCreateCandidateSpaceWithValuePools() {
        TestCandidate baseCandidate = candidate();
        CandidateValuePool pool = pool("sample.Calculator.calculate.amount");

        CandidateSpace candidateSpace = new CandidateSpace(
                baseCandidate,
                List.of(pool)
        );

        assertEquals(baseCandidate, candidateSpace.getBaseCandidate());
        assertEquals(List.of(pool), candidateSpace.getValuePools());

        Optional<CandidateValuePool> foundPool = candidateSpace.findPoolBySlotId(
                "sample.Calculator.calculate.amount"
        );

        assertTrue(foundPool.isPresent());
        assertEquals(pool, foundPool.get());
        assertEquals("sample.Calculator.calculate.amount", foundPool.get().getSlot().id());
        assertEquals(CandidateValueSlotKind.METHOD_ARGUMENT, foundPool.get().getSlot().getKind());
        assertEquals(0, foundPool.get().getSlot().getArgumentIndex());
        assertEquals("int", foundPool.get().getValues().get(0).getType());
        assertEquals("-1", foundPool.get().getValues().get(0).getValue());
    }

    @Test
    void shouldReturnEmptyWhenPoolDoesNotExist() {
        CandidateSpace candidateSpace = new CandidateSpace(
                candidate(),
                List.of(pool("sample.Calculator.calculate.amount"))
        );

        Optional<CandidateValuePool> foundPool = candidateSpace.findPoolBySlotId(
                "sample.Calculator.calculate.missing"
        );

        assertTrue(foundPool.isEmpty());
    }

    private static CandidateValuePool pool(String slotId) {
        int lastDotIndex = slotId.lastIndexOf('.');

        CandidateValueSlot slot = new CandidateValueSlot(
                CandidateValueSlotKind.METHOD_ARGUMENT,
                slotId.substring(0, lastDotIndex),
                slotId.substring(lastDotIndex + 1),
                "int",
                0
        );

        return new CandidateValuePool(
                slot,
                List.of(new GeneratedArgument("int", "-1"))
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
}
