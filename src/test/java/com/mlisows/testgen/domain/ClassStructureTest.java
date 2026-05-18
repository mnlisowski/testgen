package com.mlisows.testgen.domain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClassStructureTest {

    @Test
    void shouldDefensivelyCopyMethods() {
        MethodModel method = new MethodModel(
                "calculate",
                "double",
                true,
                List.of(new ParameterModel("amount", "double"))
        );

        List<MethodModel> methods = new ArrayList<>();
        methods.add(method);

        ClassStructure structure = new ClassStructure(
                "sample.Calculator",
                List.of(),
                methods
        );

        methods.clear();

        assertEquals(1, structure.getMethods().size());
    }
}