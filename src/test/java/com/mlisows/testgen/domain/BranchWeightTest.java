package com.mlisows.testgen.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BranchWeightTest {

    @Test
    void shouldCreateBranchWeight() {
        BranchWeight weight = new BranchWeight(2.5);

        assertEquals(2.5, weight.getValue());
    }

    @Test
    void shouldRejectNegativeWeight() {
        assertThrows(IllegalArgumentException.class, () -> new BranchWeight(-1.0));
    }
}
