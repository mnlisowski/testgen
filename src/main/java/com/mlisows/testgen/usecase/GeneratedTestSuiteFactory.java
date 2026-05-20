package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.ClassStructure;
import com.mlisows.testgen.domain.GeneratedTestCase;
import com.mlisows.testgen.domain.GeneratedTestSuite;
import com.mlisows.testgen.domain.MethodGenerationPlan;
import com.mlisows.testgen.domain.MethodModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class GeneratedTestSuiteFactory {
    private final GeneratedTestCaseFactory testCaseFactory;
    private final GeneratableCoverageGoalSelector selector;

    public GeneratedTestSuiteFactory(
            GeneratedTestCaseFactory testCaseFactory,
            GeneratableCoverageGoalSelector selector
    ) {
        this.testCaseFactory = Objects.requireNonNull(testCaseFactory, "testCaseFactory must not be null");
        this.selector = Objects.requireNonNull(selector, "selector must not be null");
    }

    public GeneratedTestSuite create(ClassStructure classStructure, List<MethodGenerationPlan> methodPlans) {
        Objects.requireNonNull(classStructure, "classStructure must not be null");
        Objects.requireNonNull(methodPlans, "methodPlans must not be null");

        List<GeneratedTestCase> testCases = new ArrayList<>();

        for (MethodModel method : classStructure.getMethods()) {
            MethodGenerationPlan plan = findPlan(methodPlans, classStructure.getClassName(), method.getName());

            if (plan != null && selector.isSupportedByCurrentGenerator(plan)) {
                testCases.addAll(testCaseFactory.createAll(classStructure, method));
            }

        }

        return new GeneratedTestSuite(classStructure.getClassName(), testCases);
    }

    private MethodGenerationPlan findPlan(
            List<MethodGenerationPlan> methodPlans,
            String className,
            String methodName
    ) {
        for (MethodGenerationPlan plan : methodPlans) {
            if (plan.getClassName().equals(className) && plan.getMethodName().equals(methodName)) {
                return plan;
            }
        }

        return null;
    }
}
