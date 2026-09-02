package com.mlisows.testgen.infrastructure.project;

import com.mlisows.testgen.infrastructure.execution.JavaSourceCompiler;

import java.util.Objects;

public final class MavenProjectClasspath {
    private final MavenDependencyClasspathResolver dependencyClasspathResolver;

    public MavenProjectClasspath() {
        this(new MavenDependencyClasspathResolver());
    }

    public MavenProjectClasspath(MavenDependencyClasspathResolver dependencyClasspathResolver) {
        this.dependencyClasspathResolver = Objects.requireNonNull(
                dependencyClasspathResolver,
                "dependencyClasspathResolver must not be null"
        );
    }

    public String resolve(MavenProjectLayout layout) {
        Objects.requireNonNull(layout, "layout must not be null");

        return JavaSourceCompiler.joinClasspaths(
                JavaSourceCompiler.defaultClasspath(),
                dependencyClasspathResolver.resolve(layout)
        );
    }
}
