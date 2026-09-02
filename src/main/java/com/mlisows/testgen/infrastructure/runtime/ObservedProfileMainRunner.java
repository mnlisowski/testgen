package com.mlisows.testgen.infrastructure.runtime;

import com.mlisows.testgen.infrastructure.project.InstrumentedProjectWorkspace;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public final class ObservedProfileMainRunner {

    public void run(
            InstrumentedProjectWorkspace workspace,
            String mainClassName,
            String[] applicationArgs,
            Path outputProfilePath
    ) {
        Objects.requireNonNull(workspace, "workspace must not be null");
        Objects.requireNonNull(mainClassName, "mainClassName must not be null");
        Objects.requireNonNull(applicationArgs, "applicationArgs must not be null");
        Objects.requireNonNull(outputProfilePath, "outputProfilePath must not be null");

        ArgumentRecorder.reset();

        try (URLClassLoader classLoader = new URLClassLoader(
                classpathUrls(workspace),
                getClass().getClassLoader()
        )) {
            Class<?> mainClass = classLoader.loadClass(mainClassName);
            Method mainMethod = mainClass.getMethod("main", String[].class);

            mainMethod.invoke(null, (Object) Arrays.copyOf(applicationArgs, applicationArgs.length));

            ArgumentRecorder.writeTo(outputProfilePath);
        } catch (InvocationTargetException exception) {
            Throwable targetException = exception.getTargetException();
            throw new IllegalStateException("Main class failed: " + mainClassName, targetException);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot run main class: " + mainClassName, exception);
        }
    }

    private URL[] classpathUrls(InstrumentedProjectWorkspace workspace) throws Exception {
        List<URL> urls = new ArrayList<>();
        urls.add(workspace.getClassesRoot().toUri().toURL());

        for (String classpathEntry : workspace.getClasspath().split(Pattern.quote(File.pathSeparator))) {
            if (!classpathEntry.isBlank()) {
                urls.add(Path.of(classpathEntry).toUri().toURL());
            }
        }

        return urls.toArray(URL[]::new);
    }
}
