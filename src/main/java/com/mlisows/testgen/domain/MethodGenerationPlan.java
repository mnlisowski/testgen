package com.mlisows.testgen.domain;

import java.util.List;
import java.util.Objects;

public final class MethodGenerationPlan {
    private final String className;
    private final String methodName;
    private final List<String> parameterTypes;
    private final List<GenerationRequirement> requirements;

    public MethodGenerationPlan(
            String className,
            String methodName,
            List<String> parameterTypes,
            List<GenerationRequirement> requirements
    ) {
        this.className = Objects.requireNonNull(className, "className must not be null");
        this.methodName = Objects.requireNonNull(methodName, "methodName must not be null");
        this.parameterTypes = List.copyOf(Objects.requireNonNull(parameterTypes, "parameterTypes must not be null"));
        this.requirements = List.copyOf(Objects.requireNonNull(requirements, "requirements must not be null"));
    }

    public List<String> getParameterTypes() {return parameterTypes;}

    public String signature() {return ExecutableSignature.method(className, methodName, parameterTypes);}

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public List<GenerationRequirement> getRequirements() {
        return requirements;
    }
}
