package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.GeneratedArgument;
import com.mlisows.testgen.domain.ParameterModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SimpleArgumentGenerator {

    public List<GeneratedArgument> generateArguments(List<ParameterModel> parameters) {
        Objects.requireNonNull(parameters, "parameters must not be null");

        List<GeneratedArgument> arguments = new ArrayList<>();

        for (ParameterModel parameter : parameters) {
            arguments.add(generateArgument(parameter));
        }

        return arguments;
    }

    private GeneratedArgument generateArgument(ParameterModel parameter) {
        String type = parameter.getType();

        if (type.equals("int")) {
            return new GeneratedArgument(type, "0");
        }

        if (type.equals("long")) {
            return new GeneratedArgument(type, "0L");
        }

        if (type.equals("double")) {
            return new GeneratedArgument(type, "0.0");
        }

        if (type.equals("float")) {
            return new GeneratedArgument(type, "0.0f");
        }

        if (type.equals("boolean")) {
            return new GeneratedArgument(type, "false");
        }

        if (type.equals("String") || type.equals("java.lang.String")) {
            return new GeneratedArgument(type, "\"\"");
        }

        throw new IllegalArgumentException("Unsupported parameter type: " + type);
    }
}

