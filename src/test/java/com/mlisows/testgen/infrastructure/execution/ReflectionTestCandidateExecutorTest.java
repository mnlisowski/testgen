package com.mlisows.testgen.infrastructure.execution;

import com.mlisows.testgen.domain.BranchId;
import com.mlisows.testgen.domain.BranchKind;
import com.mlisows.testgen.domain.BranchType;
import com.mlisows.testgen.domain.ExecutionOutcome;
import com.mlisows.testgen.domain.GeneratedArgument;
import com.mlisows.testgen.domain.GeneratedSetupObject;
import com.mlisows.testgen.domain.TestCandidate;
import com.mlisows.testgen.domain.TestCandidateExecutionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReflectionTestCandidateExecutorTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldExecuteCandidateAndCollectCoveredBranches() throws Exception {
        Path sourceRoot = tempDir.resolve("src");
        Path classesRoot = tempDir.resolve("classes");
        writeSource(sourceRoot, "sample/Calculator.java", """
                  package sample;

                  public class Calculator {
                      public int calculate(int amount) {
                          if (amount > 100) {
                              com.mlisows.testgen.infrastructure.runtime.BranchRecorder.hit("%s");
                              return 10;
                          }

                          com.mlisows.testgen.infrastructure.runtime.BranchRecorder.hit("%s");
                          return 0;
                      }
                  }
                  """.formatted(
                          branchId("sample.Calculator", "calculate", BranchType.TRUE),
                          branchId("sample.Calculator", "calculate", BranchType.FALSE)
                  ));

        new JavaSourceCompiler().compile(sourceRoot, classesRoot);

        TestCandidate candidate = new TestCandidate(
                "sample.Calculator",
                "calculate",
                "int",
                List.of(new GeneratedSetupObject("Calculator", "calculator", List.of())),
                "calculator",
                List.of(new GeneratedArgument("int", "150"))
        );

        TestCandidateExecutionResult result = new ReflectionTestCandidateExecutor(
                classesRoot,
                System.getProperty("java.class.path")
        ).execute(candidate);

        assertEquals(ExecutionOutcome.RETURNED, result.getOutcome());
        assertEquals("10", result.getReturnValue().orElseThrow());
        assertEquals(List.of(branchId("sample.Calculator", "calculate", BranchType.TRUE)), result.getCoveredBranches().stream()
                .map(BranchId::asString)
                .toList());
    }

    @Test
    void shouldCaptureNullReturnValue() throws Exception {
        Path sourceRoot = tempDir.resolve("src");
        Path classesRoot = tempDir.resolve("classes");
        writeSource(sourceRoot, "sample/NullableService.java", """
                  package sample;

                  public class NullableService {
                      public String findName() {
                          return null;
                      }
                  }
                  """);

        new JavaSourceCompiler().compile(sourceRoot, classesRoot);

        TestCandidate candidate = new TestCandidate(
                "sample.NullableService",
                "findName",
                "String",
                List.of(new GeneratedSetupObject("NullableService", "nullableService", List.of())),
                "nullableService",
                List.of()
        );

        TestCandidateExecutionResult result = new ReflectionTestCandidateExecutor(
                classesRoot,
                System.getProperty("java.class.path")
        ).execute(candidate);

        assertEquals(ExecutionOutcome.RETURNED, result.getOutcome());
        assertTrue(result.getReturnValue().isEmpty());
    }

    @Test
    void shouldCreateNestedSetupObjectsBeforeMethodCall() throws Exception {
        Path sourceRoot = tempDir.resolve("src");
        Path classesRoot = tempDir.resolve("classes");
        writeSource(sourceRoot, "sample/Customer.java", """
                  package sample;

                  public class Customer {
                      private final String segment;

                      public Customer(String segment) {
                          this.segment = segment;
                      }

                      public String getSegment() {
                          return segment;
                      }
                  }
                  """);
        writeSource(sourceRoot, "sample/Order.java", """
                  package sample;

                  public class Order {
                      private final int total;
                      private final Customer customer;

                      public Order(int total, Customer customer) {
                          this.total = total;
                          this.customer = customer;
                      }

                      public int getTotal() {
                          return total;
                      }

                      public Customer getCustomer() {
                          return customer;
                      }
                  }
                  """);
        writeSource(sourceRoot, "sample/DiscountService.java", """
                  package sample;

                  public class DiscountService {
                      public int calculate(Order order) {
                          if (order.getTotal() > 100 && order.getCustomer().getSegment().equals("VIP")) {
                              com.mlisows.testgen.infrastructure.runtime.BranchRecorder.hit("%s");
                              return 20;
                          }

                          return 5;
                      }
                  }
                  """.formatted(branchId("sample.DiscountService", "calculate", BranchType.TRUE)));

        new JavaSourceCompiler().compile(sourceRoot, classesRoot);

        TestCandidate candidate = new TestCandidate(
                "sample.DiscountService",
                "calculate",
                "int",
                List.of(
                        new GeneratedSetupObject(
                                "Customer",
                                "customer",
                                List.of(new GeneratedArgument("String", "\"VIP\""))
                        ),
                        new GeneratedSetupObject(
                                "Order",
                                "order",
                                List.of(
                                        new GeneratedArgument("int", "150"),
                                        new GeneratedArgument("Customer", "customer")
                                )
                        ),
                        new GeneratedSetupObject("DiscountService", "discountService", List.of())
                ),
                "discountService",
                List.of(new GeneratedArgument("Order", "order"))
        );

        TestCandidateExecutionResult result = new ReflectionTestCandidateExecutor(
                classesRoot,
                System.getProperty("java.class.path")
        ).execute(candidate);

        assertEquals(ExecutionOutcome.RETURNED, result.getOutcome());
        assertEquals("20", result.getReturnValue().orElseThrow());
        assertEquals(List.of(
                branchId("sample.DiscountService", "calculate", BranchType.TRUE)
        ), result.getCoveredBranches().stream()
                .map(BranchId::asString)
                .toList());
    }

    @Test
    void shouldCaptureExceptionThrownByTargetMethod() throws Exception {
        Path sourceRoot = tempDir.resolve("src");
        Path classesRoot = tempDir.resolve("classes");
        writeSource(sourceRoot, "sample/Validator.java", """
                  package sample;

                  public class Validator {
                      public void validate(int amount) {
                          com.mlisows.testgen.infrastructure.runtime.BranchRecorder.hit("%s");
                          throw new IllegalArgumentException("amount must be positive");
                      }
                  }
                  """.formatted(branchId("sample.Validator", "validate", BranchType.TRUE)));

        new JavaSourceCompiler().compile(sourceRoot, classesRoot);

        TestCandidate candidate = new TestCandidate(
                "sample.Validator",
                "validate",
                "void",
                List.of(new GeneratedSetupObject("Validator", "validator", List.of())),
                "validator",
                List.of(new GeneratedArgument("int", "-1"))
        );

        TestCandidateExecutionResult result = new ReflectionTestCandidateExecutor(
                classesRoot,
                System.getProperty("java.class.path")
        ).execute(candidate);

        assertEquals(ExecutionOutcome.THREW_EXCEPTION, result.getOutcome());
        assertEquals("java.lang.IllegalArgumentException", result.getExceptionType().orElseThrow());
        assertEquals("amount must be positive", result.getExceptionMessage().orElseThrow());
        assertEquals(List.of(branchId("sample.Validator", "validate", BranchType.TRUE)), result.getCoveredBranches().stream()
                .map(BranchId::asString)
                .toList());
    }

    private static void writeSource(Path sourceRoot, String relativePath, String source) throws Exception {
        Path sourcePath = sourceRoot.resolve(relativePath);
        Files.createDirectories(sourcePath.getParent());
        Files.writeString(sourcePath, source);
    }

    private static String branchId(String className, String methodName, BranchType branchType) {
        return new BranchId(
                className,
                methodName,
                5,
                BranchKind.IF,
                branchType,
                ""
        ).asString();
    }
}
