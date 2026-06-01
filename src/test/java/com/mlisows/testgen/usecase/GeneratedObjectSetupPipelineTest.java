package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.ClassStructure;
import com.mlisows.testgen.domain.GeneratedTestSuite;
import com.mlisows.testgen.domain.MethodGenerationPlan;
import com.mlisows.testgen.domain.ProjectClassStructureIndex;
import com.mlisows.testgen.domain.ProjectTypeIndex;
import com.mlisows.testgen.infrastructure.parser.JavaParserClassStructureAnalyzer;
import com.mlisows.testgen.infrastructure.parser.JavaParserTypeIndexAnalyzer;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratedObjectSetupPipelineTest {

    @Test
    void shouldGenerateTestSuiteWithNestedObjectSetup() {
        List<Path> sourcePaths = List.of(
                Path.of("src/test/resources/sample/Customer.java"),
                Path.of("src/test/resources/sample/Order.java"),
                Path.of("src/test/resources/sample/DiscountService.java")
        );

        JavaParserClassStructureAnalyzer classStructureAnalyzer = new JavaParserClassStructureAnalyzer();
        JavaParserTypeIndexAnalyzer typeIndexAnalyzer = new JavaParserTypeIndexAnalyzer();

        ProjectTypeIndex typeIndex = typeIndexAnalyzer.analyze(sourcePaths);

        ClassStructure customer = classStructureAnalyzer.analyze(sourcePaths.get(0));
        ClassStructure order = classStructureAnalyzer.analyze(sourcePaths.get(1));
        ClassStructure discountService = classStructureAnalyzer.analyze(sourcePaths.get(2));

        ProjectClassStructureIndex classIndex = new ProjectClassStructureIndex(List.of(
                customer,
                order,
                discountService
        ));

        MethodGenerationPlanner planner = new MethodGenerationPlanner();
        List<MethodGenerationPlan> methodPlans = planner.plan(discountService, typeIndex);

        GeneratedTestSuiteFactory suiteFactory = new GeneratedTestSuiteFactory(
                new GeneratedTestCaseFactory(new SimpleArgumentGenerator()),
                new GeneratableCoverageGoalSelector()
        );

        GeneratedTestSuite testSuite = suiteFactory.create(
                discountService,
                methodPlans,
                typeIndex,
                classIndex
        );

        String code = new JUnitTestWriter().write(testSuite);

        assertTrue(code.contains("Customer customer = new Customer(\"\");"));
        assertTrue(code.contains("Order order = new Order(-1, customer);"));
        assertTrue(code.contains("DiscountService discountService = new DiscountService();"));
        assertTrue(code.contains("int result = discountService.calculate(order);"));
    }
}
