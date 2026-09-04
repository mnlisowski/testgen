package com.mlisows.testgen.domain.analysis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

        assertEquals("sample.SimpleDiscountCalculator|calculate|6|IF|TRUE", branchId.asString());
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

        assertEquals("sample.DiscountCalculator|calculate|20|SWITCH|CASE|GOLD", branchId.asString());
    }

    @Test
    void shouldReturnMethodSignature() {
        BranchId branchId = new BranchId(
                "sample.SimpleDiscountCalculator",
                "calculate(int)",
                6,
                BranchKind.IF,
                BranchType.TRUE,
                ""
        );

        assertEquals("sample.SimpleDiscountCalculator.calculate(int)", branchId.methodSignature());
    }

    @Test
    void shouldParseBranchIdWithoutDiscriminator() {
        BranchId branchId = BranchId.parse("sample.Calculator|calculate|5|IF|TRUE");

        assertEquals("sample.Calculator", branchId.getClassName());
        assertEquals("calculate", branchId.getMethodName());
        assertEquals(5, branchId.getLineNumber());
        assertEquals(BranchKind.IF, branchId.getBranchKind());
        assertEquals(BranchType.TRUE, branchId.getBranchType());
        assertEquals("", branchId.getDiscriminator());
    }

    @Test
    void shouldParseBranchIdWithDiscriminator() {
        BranchId branchId = BranchId.parse("sample.Calculator|calculate|5|SWITCH|CASE|1");

        assertEquals("sample.Calculator", branchId.getClassName());
        assertEquals("calculate", branchId.getMethodName());
        assertEquals(5, branchId.getLineNumber());
        assertEquals(BranchKind.SWITCH, branchId.getBranchKind());
        assertEquals(BranchType.CASE, branchId.getBranchType());
        assertEquals("1", branchId.getDiscriminator());
    }

    @Test
    void shouldRejectInvalidBranchId() {
        assertThrows(IllegalArgumentException.class, () -> BranchId.parse("sample.Calculator|calculate"));
    }
}
