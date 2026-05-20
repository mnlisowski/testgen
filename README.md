# testgen

`testgen` is a Java engineering thesis project about automatic generation of unit tests.

The project focuses on a limited but working pipeline: analyze Java source code,
detect branches, generate JUnit tests, use runtime coverage/profile data, and
apply a simple genetic algorithm to improve generated inputs.


## Idea

The generator starts with static analysis of Java source files. JavaParser is
used to read class structure, constructors, methods, parameters, and basic branch
points such as `if`, `for`, `while`, and `switch`.

Then the project checks which methods are possible to test with the current
implementation. For example, a method with primitive or `String` arguments and a
public no-argument constructor is easier to handle. Methods that require complex
object creation, collections, maps, mocks, or framework context can be skipped
and reported.

For supported methods, the generator creates test candidates. At first these
candidates are based on simple seed values, for example `-1`, `0`, `1`, `true`,
`false`, `""`, and `"test"`. Later these candidates can be mutated by the
genetic algorithm.

The dynamic part is based on runtime information about branch execution. In the
MVP, JaCoCo can be used as a simpler source of this data. It can show which
branches were covered or missed during an execution of the program or existing
tests. Later, a custom profiler may be used instead, so that the project can
count how many times each branch was executed.

This runtime profile is used to assign weights to coverage goals. Branches that
were executed in a real run of the program can be treated as more important than
branches that were never reached. The genetic algorithm can then prefer
candidates that cover these more important branches.

The generated tests are written as JUnit 5 tests. In the MVP, they are mostly
smoke/regression-style tests for simple public methods.

## Pipeline

```text
Java source file
 -> static analysis with JavaParser
      -> class and method structure
      -> branch detection
      -> coverage goals

Runtime profile input
 -> MVP: JaCoCo covered/missed branch data
 -> later: custom branch execution counters
 -> branch weights

Method generation planning
 -> supported methods
 -> initial candidate population

Evolutionary loop
 -> generate or mutate candidates
 -> write JUnit test code
 -> execute generated tests
 -> check candidate coverage
 -> calculate fitness from covered goals and branch weights
 -> select candidates for the next generation

Final generated test suite
```


## Architecture

The project follows Clean Architecture.

Main packages:

- `domain` - core data models
- `usecase` - application logic
- `usecase/ports` - interfaces used by the use cases
- `infrastructure` - JavaParser, file system, coverage and execution adapters
- `cli` - command-line entry point

The main rule is that domain and use case code should not depend directly on
JavaParser, the file system, or external tools.

## Scope

- public methods
- public no-argument constructors
- primitive method arguments
- `String` method arguments
- basic branch detection:
  - `if`
  - `for`
  - `while`
  - `switch`
- generated JUnit 5 test files
- seed-based input generation
- simple mutation and selection
- runtime profile input based on JaCoCo covered/missed branch data
- coverage-based fitness score

Planned or possible extensions:

- primitive constructor arguments
- enums
- simple boundary values
- simple assertions for primitive, boolean, and `String` return values
- better reports about skipped methods

Out of scope for the MVP:

- advanced fixtures
- collection and map generation
- Mockito stubbing
- Spring or other framework context
- symbolic execution
- long method-call sequences

## Genetic Algorithm

The genetic algorithm works on generated test candidates.

In the MVP, one candidate is one call of one public method with
generated arguments. The first population is created from simple seed values:

```text
numeric: -1, 0, 1, 10, 100
boolean: false, true
String: "", "test", "a"
```

The fitness score is based on coverage goals detected during static analysis and
on branch weights coming from runtime data. In the MVP, these weights can be
based on a JaCoCo report which says whether a branch was covered or missed during
a previous execution. Later, a custom profiler can provide more precise branch
execution counts.

During the evolutionary loop, generated candidates also need to be executed so
the system can check which coverage goals they actually cover. A candidate that
covers more valuable goals should receive a better fitness score.

Static analysis may simple input hints, but it is not supposed to solve
every branch condition. 

## Limitations

The generator should work best for simple classes, where branches depend mostly on method
arguments.

It will not fully support code where behavior depends on:

- hidden object state
- constructor-injected services
- complex domain objects
- external systems
- frameworks
- complicated mocks

These limitations are expected and should be visible in the generated report.

## Running Tests

```bash
mvn test
```

## CLI Usage


Analyze a file and generate a test file:

```bash
mvn exec:java -Dexec.mainClass=com.mlisows.testgen.cli.Main -Dexec.args="src/test/resources/sample/SimpleDiscountCalculator.java target/generated-test-sources"
```

Example output:

```text
target/generated-test-sources/sample/SimpleDiscountCalculatorTest.java
```


