package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.CandidateSpace;
import com.mlisows.testgen.domain.CandidateSpace.CandidateValuePool;
import com.mlisows.testgen.domain.CandidateSpace.CandidateValueSlot;
import com.mlisows.testgen.domain.CandidateSpace.CandidateValueSlotKind;
import com.mlisows.testgen.domain.GeneratedArgument;
import com.mlisows.testgen.domain.GeneratedSetupObject;
import com.mlisows.testgen.domain.ParameterModel;
import com.mlisows.testgen.domain.TestCandidate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CandidateVariantGeneratorTest {

    @Test
    void shouldIncludeBaseCandidateFirst() {
        TestCandidate baseCandidate = calculatorCandidate(new GeneratedArgument("int", "-1"));
        CandidateSpace candidateSpace = new CandidateSpace(baseCandidate, List.of());

        List<TestCandidate> variants = new CandidateVariantGenerator().generateVariants(candidateSpace);

        assertEquals(1, variants.size());
        assertEquals(baseCandidate, variants.get(0));
    }

    @Test
    void shouldGenerateOneSlotMethodArgumentVariants() {
        TestCandidate baseCandidate = calculatorCandidate(new GeneratedArgument("int", "-1"));
        CandidateSpace candidateSpace = new CandidateSpace(
                baseCandidate,
                List.of(new CandidateValuePool(
                        new CandidateValueSlot(
                                CandidateValueSlotKind.METHOD_ARGUMENT,
                                "sample.Calculator.calculate",
                                "amount",
                                "int",
                                0
                        ),
                        List.of(
                                new GeneratedArgument("int", "-1"),
                                new GeneratedArgument("int", "0"),
                                new GeneratedArgument("int", "1")
                        )
                ))
        );

        List<TestCandidate> variants = new CandidateVariantGenerator().generateVariants(candidateSpace);

        assertEquals(3, variants.size());
        assertEquals("-1", variants.get(0).getMethodArguments().get(0).getValue());
        assertEquals("0", variants.get(1).getMethodArguments().get(0).getValue());
        assertEquals("1", variants.get(2).getMethodArguments().get(0).getValue());
    }

    @Test
    void shouldGenerateOneSlotSetupObjectArgumentVariants() {
        TestCandidate baseCandidate = discountCandidate();
        CandidateSpace candidateSpace = new CandidateSpace(
                baseCandidate,
                List.of(new CandidateValuePool(
                        new CandidateValueSlot(
                                CandidateValueSlotKind.SETUP_OBJECT_ARGUMENT,
                                "sample.Order.<init>",
                                "total",
                                "int",
                                0
                        ),
                        List.of(
                                new GeneratedArgument("int", "-1"),
                                new GeneratedArgument("int", "0"),
                                new GeneratedArgument("int", "100")
                        )
                ))
        );

        List<TestCandidate> variants = new CandidateVariantGenerator().generateVariants(candidateSpace);

        assertEquals(3, variants.size());
        assertEquals("-1", orderSetup(variants.get(0)).getArguments().get(0).getValue());
        assertEquals("0", orderSetup(variants.get(1)).getArguments().get(0).getValue());
        assertEquals("100", orderSetup(variants.get(2)).getArguments().get(0).getValue());
        assertEquals("order", variants.get(1).getMethodArguments().get(0).getValue());
    }

    @Test
    void shouldRespectCandidateLimit() {
        TestCandidate baseCandidate = calculatorCandidate(new GeneratedArgument("int", "-1"));
        CandidateSpace candidateSpace = new CandidateSpace(
                baseCandidate,
                List.of(new CandidateValuePool(
                        new CandidateValueSlot(
                                CandidateValueSlotKind.METHOD_ARGUMENT,
                                "sample.Calculator.calculate",
                                "amount",
                                "int",
                                0
                        ),
                        List.of(
                                new GeneratedArgument("int", "-1"),
                                new GeneratedArgument("int", "0"),
                                new GeneratedArgument("int", "1"),
                                new GeneratedArgument("int", "10")
                        )
                ))
        );

        List<TestCandidate> variants = new CandidateVariantGenerator(5, 2).generateVariants(candidateSpace);

        assertEquals(2, variants.size());
        assertEquals("-1", variants.get(0).getMethodArguments().get(0).getValue());
        assertEquals("0", variants.get(1).getMethodArguments().get(0).getValue());
    }

    private static TestCandidate calculatorCandidate(GeneratedArgument amount) {
        return new TestCandidate(
                "sample.Calculator",
                "calculate",
                "int",
                List.of(new GeneratedSetupObject("Calculator", "calculator", List.of())),
                "calculator",
                List.of(amount)
        );
    }

    private static TestCandidate discountCandidate() {
        GeneratedSetupObject customer = new GeneratedSetupObject(
                "Customer",
                "customer",
                List.of(new GeneratedArgument("String", "\"\"")),
                List.of(new ParameterModel("type", "String"))
        );

        GeneratedSetupObject order = new GeneratedSetupObject(
                "Order",
                "order",
                List.of(
                        new GeneratedArgument("int", "-1"),
                        new GeneratedArgument("Customer", "customer")
                ),
                List.of(
                        new ParameterModel("total", "int"),
                        new ParameterModel("customer", "Customer")
                )
        );

        GeneratedSetupObject discountService = new GeneratedSetupObject(
                "DiscountService",
                "discountService",
                List.of()
        );

        return new TestCandidate(
                "sample.DiscountService",
                "calculate",
                "int",
                List.of(customer, order, discountService),
                "discountService",
                List.of(new GeneratedArgument("Order", "order"))
        );
    }

    private static GeneratedSetupObject orderSetup(TestCandidate candidate) {
        return candidate.getSetupObjects().stream()
                .filter(setupObject -> setupObject.getVariableName().equals("order"))
                .findFirst()
                .orElseThrow();
    }
}
