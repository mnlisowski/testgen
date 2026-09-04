package com.mlisows.testgen.infrastructure.execution;

import com.mlisows.testgen.domain.analysis.BranchId;
import com.mlisows.testgen.domain.generation.GeneratedArgument;
import com.mlisows.testgen.domain.generation.GeneratedSetupObject;
import com.mlisows.testgen.domain.generation.TestCandidate;
import com.mlisows.testgen.domain.execution.TestCandidateExecutionResult;
import com.mlisows.testgen.infrastructure.project.InstrumentedProjectWorkspace;
import com.mlisows.testgen.infrastructure.runtime.BranchRecorder;
import com.mlisows.testgen.usecase.ports.TestCandidateExecutor;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public final class ReflectionTestCandidateExecutor implements TestCandidateExecutor {
    private final Path classesRoot;
    private final String classpath;
    private final GeneratedArgumentValueResolver argumentValueResolver;
    private final RecordedBranchCoverageResolver coverageResolver;

    public ReflectionTestCandidateExecutor(InstrumentedProjectWorkspace workspace) {
        this(workspace.getClassesRoot(), workspace.getClasspath());
    }

    public ReflectionTestCandidateExecutor(Path classesRoot, String classpath) {
        this(
                classesRoot,
                classpath,
                new GeneratedArgumentValueResolver(),
                new RecordedBranchCoverageResolver()
        );
    }

    ReflectionTestCandidateExecutor(
            Path classesRoot,
            String classpath,
            GeneratedArgumentValueResolver argumentValueResolver,
            RecordedBranchCoverageResolver coverageResolver
    ) {
        this.classesRoot = Objects.requireNonNull(classesRoot, "classesRoot must not be null");
        this.classpath = Objects.requireNonNull(classpath, "classpath must not be null");
        this.argumentValueResolver = Objects.requireNonNull(
                argumentValueResolver,
                "argumentValueResolver must not be null"
        );
        this.coverageResolver = Objects.requireNonNull(coverageResolver, "coverageResolver must not be null");
    }

    @Override
    public TestCandidateExecutionResult execute(TestCandidate candidate) {
        Objects.requireNonNull(candidate, "candidate must not be null");

        BranchRecorder.reset();

        try (URLClassLoader classLoader = new URLClassLoader(classpathUrls(), getClass().getClassLoader())) {
            Map<String, Object> variables = createSetupObjects(candidate, classLoader);
            Object target = targetObject(candidate, variables);
            Method method = matchingMethod(target.getClass(), candidate);
            Object[] arguments = resolveArguments(candidate.getMethodArguments(), method.getParameterTypes(), variables);

            try {
                Object returnValue = method.invoke(target, arguments);

                return TestCandidateExecutionResult.returned(
                        candidate,
                        coveredBranches(),
                        returnValue == null ? null : String.valueOf(returnValue)
                );
            } catch (InvocationTargetException exception) {
                Throwable targetException = exception.getTargetException();

                return TestCandidateExecutionResult.threwException(
                        candidate,
                        coveredBranches(),
                        targetException.getClass().getName(),
                        targetException.getMessage()
                );
            }
        } catch (Exception exception) {
            return TestCandidateExecutionResult.failedToExecute(
                    candidate,
                    safeCoveredBranches(),
                    exception.getClass().getName(),
                    exception.getMessage()
            );
        } finally {
            BranchRecorder.reset();
        }
    }

    private Map<String, Object> createSetupObjects(TestCandidate candidate, ClassLoader classLoader) throws Exception {
        Map<String, Object> variables = new LinkedHashMap<>();

        for (GeneratedSetupObject setupObject : candidate.getSetupObjects()) {
            Class<?> setupClass = loadSetupClass(candidate, setupObject, classLoader);
            Constructor<?> constructor = matchingConstructor(setupClass, setupObject);
            Object[] arguments = resolveArguments(setupObject.getArguments(), constructor.getParameterTypes(), variables);

            Object instance = constructor.newInstance(arguments);
            variables.put(setupObject.getVariableName(), instance);
        }

        return variables;
    }

    private Object targetObject(TestCandidate candidate, Map<String, Object> variables) {
        Object target = variables.get(candidate.getTargetVariableName());

        if (target == null) {
            throw new IllegalArgumentException("Target variable was not created: " + candidate.getTargetVariableName());
        }

        return target;
    }

    private Constructor<?> matchingConstructor(Class<?> setupClass, GeneratedSetupObject setupObject) {
        if (setupObject.getConstructorParameters().isEmpty()) {
            return matchingConstructorByArgumentCount(setupClass, setupObject.getArguments().size());
        }

        return Arrays.stream(setupClass.getConstructors())
                .filter(constructor -> constructor.getParameterCount() == setupObject.getConstructorParameters().size())
                .filter(constructor -> constructorParameterTypesMatch(constructor, setupObject))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No public constructor matching generated setup object in " + setupClass.getName()
                ));
    }

    private Constructor<?> matchingConstructorByArgumentCount(Class<?> setupClass, int argumentCount) {
        return Arrays.stream(setupClass.getConstructors())
                .filter(constructor -> constructor.getParameterCount() == argumentCount)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No public constructor with " + argumentCount + " arguments in " + setupClass.getName()
                ));
    }

    private boolean constructorParameterTypesMatch(Constructor<?> constructor, GeneratedSetupObject setupObject) {
        Class<?>[] actualTypes = constructor.getParameterTypes();

        for (int index = 0; index < actualTypes.length; index++) {
            String expectedType = setupObject.getConstructorParameters().get(index).getType();

            if (!typeNameMatches(expectedType, actualTypes[index])) {
                return false;
            }
        }

        return true;
    }

    private boolean typeNameMatches(String expectedType, Class<?> actualType) {
        if (expectedType.equals(actualType.getSimpleName()) || expectedType.equals(actualType.getName())) {
            return true;
        }

        String canonicalName = actualType.getCanonicalName();
        return canonicalName != null && expectedType.equals(canonicalName);
    }

    private Method matchingMethod(Class<?> targetClass, TestCandidate candidate) {
        if (candidate.getMethodParameterTypes().isEmpty()) {
            return matchingMethodByArgumentCount(targetClass, candidate);
        }

        return Arrays.stream(targetClass.getMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> method.getName().equals(candidate.getMethodName()))
                .filter(method -> method.getParameterCount() == candidate.getMethodParameterTypes().size())
                .filter(method -> methodParameterTypesMatch(method, candidate))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No public method " + candidate.getMethodName()
                                + " matching candidate parameter types in " + targetClass.getName()
                ));
    }

    private Method matchingMethodByArgumentCount(Class<?> targetClass, TestCandidate candidate) {
        return Arrays.stream(targetClass.getMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> method.getName().equals(candidate.getMethodName()))
                .filter(method -> method.getParameterCount() == candidate.getMethodArguments().size())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No public method " + candidate.getMethodName()
                                + " with " + candidate.getMethodArguments().size()
                                + " arguments in " + targetClass.getName()
                ));
    }

    private boolean methodParameterTypesMatch(Method method, TestCandidate candidate) {
        Class<?>[] actualTypes = method.getParameterTypes();

        for (int index = 0; index < actualTypes.length; index++) {
            String expectedType = candidate.getMethodParameterTypes().get(index);

            if (!typeNameMatches(expectedType, actualTypes[index])) {
                return false;
            }
        }

        return true;
    }

    private Object[] resolveArguments(
            List<GeneratedArgument> generatedArguments,
            Class<?>[] targetTypes,
            Map<String, Object> variables
    ) {
        Object[] arguments = new Object[generatedArguments.size()];

        for (int index = 0; index < generatedArguments.size(); index++) {
            arguments[index] = argumentValueResolver.resolve(
                    generatedArguments.get(index),
                    targetTypes[index],
                    variables
            );
        }

        return arguments;
    }

    private Class<?> loadSetupClass(
            TestCandidate candidate,
            GeneratedSetupObject setupObject,
            ClassLoader classLoader
    ) throws ClassNotFoundException {
        if (setupObject.getVariableName().equals(candidate.getTargetVariableName())) {
            return Class.forName(candidate.getClassName(), true, classLoader);
        }

        String type = setupObject.getType();

        if (type.contains(".")) {
            return Class.forName(type, true, classLoader);
        }

        String packageName = packageName(candidate.getClassName());

        if (!packageName.isEmpty()) {
            return Class.forName(packageName + "." + type, true, classLoader);
        }

        return Class.forName(type, true, classLoader);
    }

    private URL[] classpathUrls() throws Exception {
        List<URL> urls = new ArrayList<>();
        urls.add(classesRoot.toUri().toURL());

        for (String classpathEntry : classpath.split(Pattern.quote(File.pathSeparator))) {
            if (!classpathEntry.isBlank()) {
                urls.add(Path.of(classpathEntry).toUri().toURL());
            }
        }

        return urls.toArray(URL[]::new);
    }

    private List<BranchId> coveredBranches() {
        return coverageResolver.resolve(BranchRecorder.snapshot());
    }

    private List<BranchId> safeCoveredBranches() {
        try {
            return coveredBranches();
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private String packageName(String className) {
        int lastDotIndex = className.lastIndexOf('.');

        if (lastDotIndex == -1) {
            return "";
        }

        return className.substring(0, lastDotIndex);
    }
}
