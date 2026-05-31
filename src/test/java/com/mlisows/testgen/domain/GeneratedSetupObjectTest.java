package com.mlisows.testgen.domain;

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
}
