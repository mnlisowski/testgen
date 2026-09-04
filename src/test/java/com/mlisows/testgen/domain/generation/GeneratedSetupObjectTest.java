package com.mlisows.testgen.domain.generation;

import com.mlisows.testgen.domain.structure.ParameterModel;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeneratedSetupObjectTest {

    @Test
    void shouldStoreSetupObjectData() {
        GeneratedArgument argument = new GeneratedArgument("String", "\"PREMIUM\"");

        GeneratedSetupObject objectCreation = new GeneratedSetupObject(
                "Customer",
                "customer",
                List.of(argument)
        );

        assertEquals("Customer", objectCreation.getType());
        assertEquals("customer", objectCreation.getVariableName());
        assertEquals(List.of(argument), objectCreation.getArguments());
    }

    @Test
    void shouldKeepConstructorParameterMetadata() {
        ParameterModel parameter = new ParameterModel("total", "int");

        GeneratedSetupObject setupObject = new GeneratedSetupObject(
                "Order",
                "order",
                List.of(new GeneratedArgument("int", "-1")),
                List.of(parameter)
        );

        assertEquals(List.of(parameter), setupObject.getConstructorParameters());
    }
}
