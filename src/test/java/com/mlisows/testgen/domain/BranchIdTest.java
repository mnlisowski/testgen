package com.mlisows.testgen.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BranchIdTest {

    @Test
    void shouldFormatBranchIdWithoutDiscriminator() {
        BranchId branchId = new BranchId(
                "sample.SimpleDiscountCalculator",
                "calculate",
                6,
                BranchKind.IF,
                BranchType.TRUE,
                ""
        );

        assertEquals("sample.SimpleDiscountCalculator.calculate.L6.IF.TRUE", branchId.asString());
    }

    @Test
    void shouldFormatBranchIdWithDiscriminator() {
        BranchId branchId = new BranchId(
                "sample.DiscountCalculator",
                "calculate",
                20,
                BranchKind.SWITCH,
                BranchType.CASE,
                "GOLD"
        );

        assertEquals("sample.DiscountCalculator.calculate.L20.SWITCH.CASE.GOLD", branchId.asString());
    }
}
