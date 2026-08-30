package com.mlisows.testgen.infrastructure.runtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BranchRecorderTest {

    @BeforeEach
    void resetRecorder() {
        BranchRecorder.reset();
    }

    @Test
    void shouldRecordBranchHit() {
        BranchRecorder.hit("sample.Calculator.calculate.L10.IF.TRUE");

        assertEquals(Map.of(
                "sample.Calculator.calculate.L10.IF.TRUE",
                1
        ), BranchRecorder.snapshot());
    }

    @Test
    void shouldCountMultipleHitsForSameBranch() {
        BranchRecorder.hit("sample.Calculator.calculate.L10.IF.TRUE");
        BranchRecorder.hit("sample.Calculator.calculate.L10.IF.TRUE");

        assertEquals(2, BranchRecorder.snapshot().get("sample.Calculator.calculate.L10.IF.TRUE"));
    }

    @Test
    void shouldResetRecordedHits() {
        BranchRecorder.hit("sample.Calculator.calculate.L10.IF.TRUE");

        BranchRecorder.reset();

        assertTrue(BranchRecorder.snapshot().isEmpty());
    }
}
