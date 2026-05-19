package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.ClassStructure;
import com.mlisows.testgen.domain.ConstructorModel;
import com.mlisows.testgen.domain.GeneratedTestCase;
import com.mlisows.testgen.domain.MethodModel;
import com.mlisows.testgen.domain.ParameterModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeneratedTestCaseFactoryTest {

    @Test
    void shouldCreateGeneratedTestCaseForMethod() {
        MethodModel method = new MethodModel(
                "calculate",
                "int",
                true,
                List.of(new ParameterModel("amount", "int"))
        );

        ClassStructure classStructure = new ClassStructure(
                "sample.Calculator",
                List.of(new ConstructorModel(true, List.of())),
                List.of(method)
        );

        GeneratedTestCaseFactory factory = new GeneratedTestCaseFactory(new SimpleArgumentGenerator());

        GeneratedTestCase testCase = factory.create(classStructure, method);

        assertEquals("sample.Calculator", testCase.getClassName());
        assertEquals("calculate", testCase.getMethodName());
        assertEquals("shouldCallCalculate", testCase.getTestName());
        assertEquals(List.of(), testCase.getConstructorArguments());

        assertEquals(1, testCase.getMethodArguments().size());
        assertEquals("int", testCase.getMethodArguments().get(0).getType());
        assertEquals("0", testCase.getMethodArguments().get(0).getValue());
    }
}
