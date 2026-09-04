package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.generation.CandidateSpace;
import com.mlisows.testgen.domain.generation.CandidateSpace.CandidateValueOption;
import com.mlisows.testgen.domain.generation.CandidateSpace.CandidateValuePool;
import com.mlisows.testgen.domain.generation.CandidateSpace.CandidateValueSlotKind;
import com.mlisows.testgen.domain.generation.CandidateSpace.CandidateValueTier;
import com.mlisows.testgen.domain.structure.ClassStructure;
import com.mlisows.testgen.domain.structure.ConstructorModel;
import com.mlisows.testgen.domain.generation.GeneratedArgument;
import com.mlisows.testgen.domain.structure.MethodModel;
import com.mlisows.testgen.domain.structure.ParameterModel;
import com.mlisows.testgen.domain.structure.ProjectClassStructureIndex;
import com.mlisows.testgen.domain.structure.ProjectTypeIndex;
import com.mlisows.testgen.domain.generation.ArgumentValueHint;
import com.mlisows.testgen.domain.structure.TypeInfo;
import com.mlisows.testgen.domain.structure.TypeKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CandidateSpaceFactoryTest {

    @Test
    void shouldCreateSpaceWithPrimitiveMethodArgumentPool() {
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

        Optional<CandidateSpace> candidateSpace = new CandidateSpaceFactory().create(
                calculator,
                method,
                new ProjectTypeIndex(List.of()),
                new ProjectClassStructureIndex(List.of(calculator))
        );

        assertTrue(candidateSpace.isPresent());
        assertEquals("sample.Calculator", candidateSpace.get().getBaseCandidate().getClassName());
        assertEquals(1, candidateSpace.get().getValuePools().size());

        CandidateValuePool amountPool = candidateSpace.get()
                .findPoolBySlotId("sample.Calculator.calculate(int).amount")
                .orElseThrow();

        assertEquals(CandidateValueSlotKind.METHOD_ARGUMENT, amountPool.getSlot().getKind());
        assertEquals(0, amountPool.getSlot().getArgumentIndex());
        assertEquals("int", amountPool.getSlot().getType());
        assertValues(amountPool, "-1", "0", "1", "10", "100");
        assertTiers(amountPool,
                CandidateValueTier.FALLBACK,
                CandidateValueTier.FALLBACK,
                CandidateValueTier.FALLBACK,
                CandidateValueTier.FALLBACK,
                CandidateValueTier.FALLBACK
        );
    }


    @Test
    void shouldAddStaticHintsAsExactMethodArgumentValues() {
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

        Optional<CandidateSpace> candidateSpace = new CandidateSpaceFactory().create(
                calculator,
                method,
                new ProjectTypeIndex(List.of()),
                new ProjectClassStructureIndex(List.of(calculator)),
                List.of(new ArgumentValueHint(
                        "sample.Calculator.calculate(int)",
                        "amount",
                        "int",
                        "101",
                        "direct-static-condition"
                ))
        );

        assertTrue(candidateSpace.isPresent());

        CandidateValuePool amountPool = candidateSpace.get()
                .findPoolBySlotId("sample.Calculator.calculate(int).amount")
                .orElseThrow();

        assertValues(amountPool, "101", "-1", "0", "1", "10", "100");
        assertTiers(amountPool,
                CandidateValueTier.EXACT,
                CandidateValueTier.FALLBACK,
                CandidateValueTier.FALLBACK,
                CandidateValueTier.FALLBACK,
                CandidateValueTier.FALLBACK,
                CandidateValueTier.FALLBACK
        );
    }

    @Test
    void shouldCreateSpaceWithEnumMethodArgumentPool() {
        MethodModel method = new MethodModel(
                "calculate",
                "int",
                true,
                List.of(new ParameterModel("type", "CustomerType"))
        );

        ClassStructure calculator = new ClassStructure(
                "sample.Calculator",
                List.of(new ConstructorModel(true, List.of())),
                List.of(method)
        );

        ProjectTypeIndex typeIndex = new ProjectTypeIndex(List.of(new TypeInfo(
                "CustomerType",
                "sample.CustomerType",
                TypeKind.ENUM,
                List.of("REGULAR", "GOLD")
        )));

        Optional<CandidateSpace> candidateSpace = new CandidateSpaceFactory().create(
                calculator,
                method,
                typeIndex,
                new ProjectClassStructureIndex(List.of(calculator))
        );

        assertTrue(candidateSpace.isPresent());

        CandidateValuePool typePool = candidateSpace.get()
                .findPoolBySlotId("sample.Calculator.calculate(CustomerType).type")
                .orElseThrow();

        assertValues(typePool, "sample.CustomerType.REGULAR", "sample.CustomerType.GOLD");
    }

    @Test
    void shouldCreateSpaceWithNestedSetupObjectArgumentPools() {
        MethodModel method = new MethodModel(
                "calculate",
                "int",
                true,
                List.of(new ParameterModel("order", "Order"))
        );

        ClassStructure discountService = new ClassStructure(
                "sample.DiscountService",
                List.of(new ConstructorModel(true, List.of())),
                List.of(method)
        );

        ClassStructure customer = new ClassStructure(
                "sample.Customer",
                List.of(new ConstructorModel(
                        true,
                        List.of(new ParameterModel("type", "String"))
                )),
                List.of()
        );

        ClassStructure order = new ClassStructure(
                "sample.Order",
                List.of(new ConstructorModel(
                        true,
                        List.of(
                                new ParameterModel("total", "int"),
                                new ParameterModel("customer", "Customer")
                        )
                )),
                List.of()
        );

        Optional<CandidateSpace> candidateSpace = new CandidateSpaceFactory().create(
                discountService,
                method,
                new ProjectTypeIndex(List.of()),
                new ProjectClassStructureIndex(List.of(discountService, order, customer))
        );

        assertTrue(candidateSpace.isPresent());
        assertEquals(4, candidateSpace.get().getValuePools().size());

        CandidateValuePool methodOrderPool = candidateSpace.get()
                .findPoolBySlotId("sample.DiscountService.calculate(Order).order")
                .orElseThrow();
        CandidateValuePool orderTotalPool = candidateSpace.get()
                .findPoolBySlotId("sample.Order.<init>(int,Customer).total")
                .orElseThrow();
        CandidateValuePool orderCustomerPool = candidateSpace.get()
                .findPoolBySlotId("sample.Order.<init>(int,Customer).customer")
                .orElseThrow();
        CandidateValuePool customerTypePool = candidateSpace.get()
                .findPoolBySlotId("sample.Customer.<init>(String).type")
                .orElseThrow();

        assertEquals(CandidateValueSlotKind.METHOD_ARGUMENT, methodOrderPool.getSlot().getKind());
        assertValues(methodOrderPool, "order", "null");

        assertEquals(CandidateValueSlotKind.SETUP_OBJECT_ARGUMENT, orderTotalPool.getSlot().getKind());
        assertEquals(0, orderTotalPool.getSlot().getArgumentIndex());
        assertValues(orderTotalPool, "-1", "0", "1", "10", "100");

        assertEquals(CandidateValueSlotKind.SETUP_OBJECT_ARGUMENT, orderCustomerPool.getSlot().getKind());
        assertEquals(1, orderCustomerPool.getSlot().getArgumentIndex());
        assertValues(orderCustomerPool, "customer", "null");
        assertTiers(orderCustomerPool,
                CandidateValueTier.FALLBACK,
                CandidateValueTier.NULL
        );

        assertEquals(CandidateValueSlotKind.SETUP_OBJECT_ARGUMENT, customerTypePool.getSlot().getKind());
        assertValues(customerTypePool, "\"\"", "null", "\"test\"", "\"a\"");
        assertTiers(customerTypePool,
                CandidateValueTier.FALLBACK,
                CandidateValueTier.NULL,
                CandidateValueTier.FALLBACK,
                CandidateValueTier.FALLBACK
        );
    }

    @Test
    void shouldReturnEmptyWhenInitialCandidateCannotBeCreated() {
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

        Optional<CandidateSpace> candidateSpace = new CandidateSpaceFactory().create(
                calculator,
                method,
                new ProjectTypeIndex(List.of()),
                new ProjectClassStructureIndex(List.of(calculator))
        );

        assertTrue(candidateSpace.isEmpty());
    }

    private static void assertValues(CandidateValuePool pool, String... expectedValues) {
        List<String> actualValues = pool.getValues().stream()
                .map(GeneratedArgument::getValue)
                .toList();

        assertEquals(List.of(expectedValues), actualValues);
    }

    private static void assertTiers(CandidateValuePool pool, CandidateValueTier... expectedTiers) {
        List<CandidateValueTier> actualTiers = pool.getOptions().stream()
                .map(CandidateValueOption::getTier)
                .toList();

        assertEquals(List.of(expectedTiers), actualTiers);
    }
}
