package com.mlisows.testgen.domain;

import java.util.List;
import java.util.stream.Collectors;

public final class ExecutableSignature {
    private ExecutableSignature() {
    }

    public static String method(String className, MethodModel method) {
        return method(className, method.getName(), method.getParameterTypes());
    }

    public static String method(String className, String methodName, List<String> parameterTypes) {
        return className + "." + methodName + "(" + join(parameterTypes) + ")";
    }

    public static String constructor(String className, ConstructorModel constructor) {
        return constructor(className, parameterTypes(constructor.getParameters()));
    }

    public static String constructor(String className, List<String> parameterTypes) {
        return className + ".<init>(" + join(parameterTypes) + ")";
    }

    private static List<String> parameterTypes(List<ParameterModel> parameters) {
        return parameters.stream()
                .map(ParameterModel::getType)
                .toList();
    }

    private static String join(List<String> parameterTypes) {
        return parameterTypes.stream()
                .collect(Collectors.joining(","));
    }
}
