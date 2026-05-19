package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.GeneratedArgument;
import com.mlisows.testgen.domain.ParameterModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleArgumentGeneratorTest {

    @Test
    void shouldGenerateArgumentsForSimpleTypes() {
        SimpleArgumentGenerator generator = new SimpleArgumentGenerator();

        List<GeneratedArgument> arguments = generator.generateArguments(List.of(
                new ParameterModel("amount", "int"),
                new ParameterModel("active", "boolean"),
                new ParameterModel("name", "String")
        ));

        assertEquals(3, arguments.size());

        assertEquals("int", arguments.get(0).getType());
        assertEquals("0", arguments.get(0).getValue());

        assertEquals("boolean", arguments.get(1).getType());
        assertEquals("false", arguments.get(1).getValue());

        assertEquals("String", arguments.get(2).getType());
        assertEquals("\"\"", arguments.get(2).getValue());
    }
}
