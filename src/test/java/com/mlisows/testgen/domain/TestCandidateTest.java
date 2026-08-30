package com.mlisows.testgen.domain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestCandidateTest {

    @Test
    void shouldCreateTestCandidate() {
        GeneratedSetupObject setupObject = new GeneratedSetupObject(
                "Calculator",
                "calculator",
                List.of()
        );

        GeneratedArgument argument = new GeneratedArgument("int", "-1");

        TestCandidate candidate = new TestCandidate(
                "sample.Calculator",
                "calculate",
                "int",
                List.of(setupObject),
                "calculator",
                List.of(argument)
        );

        assertEquals("sample.Calculator", candidate.getClassName());
        assertEquals("calculate", candidate.getMethodName());
        assertEquals("int", candidate.getReturnType());
        assertEquals(List.of(setupObject), candidate.getSetupObjects());
        assertEquals("calculator", candidate.getTargetVariableName());
        assertEquals(List.of(argument), candidate.getMethodArguments());
    }

    @Test
    void shouldDefensivelyCopyLists() {
        List<GeneratedSetupObject> setupObjects = new ArrayList<>();
        List<GeneratedArgument> methodArguments = new ArrayList<>();

        setupObjects.add(new GeneratedSetupObject("Calculator", "calculator", List.of()));
        methodArguments.add(new GeneratedArgument("int", "-1"));

        TestCandidate candidate = new TestCandidate(
                "sample.Calculator",
                "calculate",
                "int",
                setupObjects,
                "calculator",
                methodArguments
        );

        setupObjects.clear();
        methodArguments.clear();

        assertEquals(1, candidate.getSetupObjects().size());
        assertEquals(1, candidate.getMethodArguments().size());
    }
}
