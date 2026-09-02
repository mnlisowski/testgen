package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.ClassStructure;
import com.mlisows.testgen.domain.ConstructorModel;
import com.mlisows.testgen.domain.GeneratedSetupObject;
import com.mlisows.testgen.domain.ParameterModel;
import com.mlisows.testgen.domain.ProjectClassStructureIndex;
import com.mlisows.testgen.domain.ProjectTypeIndex;
import com.mlisows.testgen.domain.TypeInfo;
import com.mlisows.testgen.domain.TypeKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleSetupObjectGeneratorTest {

    @Test
    void shouldGenerateSetupObjectForPublicConstructorWithPrimitiveAndStringArguments() {
        ClassStructure order = new ClassStructure(
                "sample.Order",
                List.of(new ConstructorModel(
                        true,
                        List.of(
                                new ParameterModel("total", "int"),
                                new ParameterModel("code", "String")
                        )
                )),
                List.of()
        );

        SimpleSetupObjectGenerator generator = new SimpleSetupObjectGenerator(new SeedValueGenerator());

        Optional<GeneratedSetupObject> setupObject = generator.generate(
                new ParameterModel("order", "Order"),
                new ProjectClassStructureIndex(List.of(order)),
                new ProjectTypeIndex(List.of())
        );

        assertTrue(setupObject.isPresent());
        assertEquals("Order", setupObject.get().getType());
        assertEquals("order", setupObject.get().getVariableName());
        assertEquals(2, setupObject.get().getArguments().size());
        assertEquals("-1", setupObject.get().getArguments().get(0).getValue());
        assertEquals("\"\"", setupObject.get().getArguments().get(1).getValue());
    }

    @Test
    void shouldGenerateSetupObjectForImplicitNoArgConstructor() {
        ClassStructure order = new ClassStructure(
                "sample.Order",
                List.of(),
                List.of()
        );

        SimpleSetupObjectGenerator generator = new SimpleSetupObjectGenerator(new SeedValueGenerator());

        Optional<GeneratedSetupObject> setupObject = generator.generate(
                new ParameterModel("order", "Order"),
                new ProjectClassStructureIndex(List.of(order)),
                new ProjectTypeIndex(List.of())
        );

        assertTrue(setupObject.isPresent());
        assertEquals("Order", setupObject.get().getType());
        assertEquals("order", setupObject.get().getVariableName());
        assertTrue(setupObject.get().getArguments().isEmpty());
    }

    @Test
    void shouldGenerateSetupObjectForPublicConstructorWithEnumArgument() {
        ClassStructure customer = new ClassStructure(
                "sample.Customer",
                List.of(new ConstructorModel(
                        true,
                        List.of(new ParameterModel("type", "CustomerType"))
                )),
                List.of()
        );

        ProjectTypeIndex typeIndex = new ProjectTypeIndex(List.of(new TypeInfo(
                "CustomerType",
                "sample.CustomerType",
                TypeKind.ENUM,
                List.of("REGULAR", "GOLD")
        )));

        SimpleSetupObjectGenerator generator = new SimpleSetupObjectGenerator(new SeedValueGenerator());

        Optional<GeneratedSetupObject> setupObject = generator.generate(
                new ParameterModel("customer", "Customer"),
                new ProjectClassStructureIndex(List.of(customer)),
                typeIndex
        );

        assertTrue(setupObject.isPresent());
        assertEquals("Customer", setupObject.get().getType());
        assertEquals("customer", setupObject.get().getVariableName());
        assertEquals("sample.CustomerType.REGULAR", setupObject.get().getArguments().get(0).getValue());
    }


    @Test
    void shouldKeepConstructorParametersInGeneratedSetupObject() {
        ClassStructure order = new ClassStructure(
                "sample.Order",
                List.of(new ConstructorModel(
                        true,
                        List.of(
                                new ParameterModel("total", "int"),
                                new ParameterModel("code", "String")
                        )
                )),
                List.of()
        );

        SimpleSetupObjectGenerator generator = new SimpleSetupObjectGenerator(new SeedValueGenerator());

        Optional<GeneratedSetupObject> setupObject = generator.generate(
                new ParameterModel("order", "Order"),
                new ProjectClassStructureIndex(List.of(order)),
                new ProjectTypeIndex(List.of())
        );

        assertTrue(setupObject.isPresent());
        assertEquals(2, setupObject.get().getConstructorParameters().size());
        assertEquals("total", setupObject.get().getConstructorParameters().get(0).getName());
        assertEquals("int", setupObject.get().getConstructorParameters().get(0).getType());
        assertEquals("code", setupObject.get().getConstructorParameters().get(1).getName());
        assertEquals("String", setupObject.get().getConstructorParameters().get(1).getType());
    }

    @Test
    void shouldReturnEmptyWhenTypeIsNotProjectClass() {
        SimpleSetupObjectGenerator generator = new SimpleSetupObjectGenerator(new SeedValueGenerator());

        Optional<GeneratedSetupObject> setupObject = generator.generate(
                new ParameterModel("order", "Order"),
                new ProjectClassStructureIndex(List.of()),
                new ProjectTypeIndex(List.of())
        );

        assertTrue(setupObject.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenConstructorHasUnsupportedArgument() {
        ClassStructure order = new ClassStructure(
                "sample.Order",
                List.of(new ConstructorModel(
                        true,
                        List.of(new ParameterModel("items", "List<String>"))
                )),
                List.of()
        );

        SimpleSetupObjectGenerator generator = new SimpleSetupObjectGenerator(new SeedValueGenerator());

        Optional<GeneratedSetupObject> setupObject = generator.generate(
                new ParameterModel("order", "Order"),
                new ProjectClassStructureIndex(List.of(order)),
                new ProjectTypeIndex(List.of())
        );

        assertTrue(setupObject.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenClassHasNoPublicConstructor() {
        ClassStructure order = new ClassStructure(
                "sample.Order",
                List.of(new ConstructorModel(
                        false,
                        List.of(new ParameterModel("total", "int"))
                )),
                List.of()
        );

        SimpleSetupObjectGenerator generator = new SimpleSetupObjectGenerator(new SeedValueGenerator());

        Optional<GeneratedSetupObject> setupObject = generator.generate(
                new ParameterModel("order", "Order"),
                new ProjectClassStructureIndex(List.of(order)),
                new ProjectTypeIndex(List.of())
        );

        assertTrue(setupObject.isEmpty());
    }

    @Test
    void shouldGenerateNestedSetupObjectForConstructorObjectArgument() {
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

        SimpleSetupObjectGenerator generator = new SimpleSetupObjectGenerator(new SeedValueGenerator());

        Optional<SetupObjectResolution> resolution = generator.generateSetup(
                new ParameterModel("order", "Order"),
                new ProjectClassStructureIndex(List.of(order, customer)),
                new ProjectTypeIndex(List.of())
        );

        assertTrue(resolution.isPresent());
        assertEquals(2, resolution.get().getSetupObjects().size());

        GeneratedSetupObject customerSetup = resolution.get().getSetupObjects().get(0);
        assertEquals("Customer", customerSetup.getType());
        assertEquals("customer", customerSetup.getVariableName());
        assertEquals("\"\"", customerSetup.getArguments().get(0).getValue());

        GeneratedSetupObject orderSetup = resolution.get().getSetupObjects().get(1);
        assertEquals("Order", orderSetup.getType());
        assertEquals("order", orderSetup.getVariableName());
        assertEquals("-1", orderSetup.getArguments().get(0).getValue());
        assertEquals("customer", orderSetup.getArguments().get(1).getValue());

        assertEquals("Order", resolution.get().getReferenceArgument().getType());
        assertEquals("order", resolution.get().getReferenceArgument().getValue());
    }

    @Test
    void shouldReturnEmptyWhenObjectSetupHasCycle() {
        ClassStructure customer = new ClassStructure(
                "sample.Customer",
                List.of(new ConstructorModel(
                        true,
                        List.of(new ParameterModel("order", "Order"))
                )),
                List.of()
        );

        ClassStructure order = new ClassStructure(
                "sample.Order",
                List.of(new ConstructorModel(
                        true,
                        List.of(new ParameterModel("customer", "Customer"))
                )),
                List.of()
        );

        SimpleSetupObjectGenerator generator = new SimpleSetupObjectGenerator(new SeedValueGenerator());

        Optional<SetupObjectResolution> resolution = generator.generateSetup(
                new ParameterModel("order", "Order"),
                new ProjectClassStructureIndex(List.of(order, customer)),
                new ProjectTypeIndex(List.of())
        );

        assertTrue(resolution.isEmpty());
    }

}

