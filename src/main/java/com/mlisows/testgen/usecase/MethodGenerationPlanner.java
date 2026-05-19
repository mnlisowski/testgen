package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.ClassStructure;
import com.mlisows.testgen.domain.ConstructorModel;
import com.mlisows.testgen.domain.GenerationRequirement;
import com.mlisows.testgen.domain.MethodGenerationPlan;
import com.mlisows.testgen.domain.MethodModel;
import com.mlisows.testgen.domain.ParameterModel;
import com.mlisows.testgen.domain.ProjectTypeIndex;
import com.mlisows.testgen.domain.TypeInfo;
import com.mlisows.testgen.domain.TypeKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class MethodGenerationPlanner {

    public List<MethodGenerationPlan> plan(ClassStructure classStructure) {
        return plan(classStructure, new ProjectTypeIndex(List.of()));
    }

    public List<MethodGenerationPlan> plan(ClassStructure classStructure, ProjectTypeIndex typeIndex) {
        Objects.requireNonNull(classStructure, "classStructure must not be null");
        Objects.requireNonNull(typeIndex, "typeIndex must not be null");

        List<MethodGenerationPlan> plans = new ArrayList<>();

        for (MethodModel method : classStructure.getMethods()) {
            List<GenerationRequirement> requirements = new ArrayList<>();

            if (hasNoArgConstructor(classStructure)) {
                requirements.add(GenerationRequirement.NO_ARG_CONSTRUCTOR);
            } else {
                requirements.add(GenerationRequirement.OBJECT_FIXTURE);
            }

            for (ParameterModel parameter : method.getParameters()) {
                requirements.add(requirementForType(parameter.getType(), typeIndex));
            }

            plans.add(new MethodGenerationPlan(
                    classStructure.getClassName(),
                    method.getName(),
                    requirements
            ));
        }

        return plans;
    }

    private boolean hasNoArgConstructor(ClassStructure classStructure) {
        if (classStructure.getConstructors().isEmpty()) {
            return true;
        }

        for (ConstructorModel constructor : classStructure.getConstructors()) {
            if (constructor.isPublicConstructor() && constructor.getParameters().isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private GenerationRequirement requirementForType(String type, ProjectTypeIndex typeIndex) {
        if (isPrimitiveType(type)) {
            return GenerationRequirement.PRIMITIVE_ARGUMENT;
        }

        if (type.equals("String") || type.equals("java.lang.String")) {
            return GenerationRequirement.STRING_ARGUMENT;
        }

        if (type.startsWith("List") || type.startsWith("Set") || type.startsWith("Collection")) {
            return GenerationRequirement.COLLECTION_FIXTURE;
        }

        if (type.startsWith("Map")) {
            return GenerationRequirement.MAP_FIXTURE;
        }

        Optional<TypeInfo> typeInfo = typeIndex.findByName(type);

        if (typeInfo.isPresent()) {
            TypeKind kind = typeInfo.get().getKind();

            if (kind == TypeKind.ENUM) {
                return GenerationRequirement.ENUM_ARGUMENT;
            }

            if (kind == TypeKind.INTERFACE) {
                return GenerationRequirement.INTERFACE_MOCK;
            }

            if (kind == TypeKind.CLASS) {
                return GenerationRequirement.OBJECT_FIXTURE;
            }
        }

        return GenerationRequirement.OBJECT_FIXTURE;
    }

    private boolean isPrimitiveType(String type) {
        return type.equals("int")
                || type.equals("long")
                || type.equals("double")
                || type.equals("float")
                || type.equals("boolean")
                || type.equals("short")
                || type.equals("byte")
                || type.equals("char");
    }
}
