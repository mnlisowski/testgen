package com.mlisows.testgen.infrastructure.runtime;

import com.mlisows.testgen.domain.generation.ArgumentValueHint;
import com.mlisows.testgen.domain.generation.GeneratedArgument;
import com.mlisows.testgen.domain.profile.ObservedInvocation;
import com.mlisows.testgen.domain.profile.ObservedProfile;
import com.mlisows.testgen.usecase.ports.ObservedProfileReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TextObservedProfileReader implements ObservedProfileReader {
    private static final String FIELD_SEPARATOR = "\\|";

    @Override
    public ObservedProfile read(Path profilePath) {
        Objects.requireNonNull(profilePath, "profilePath must not be null");

        List<ArgumentValueHint> argumentValueHints = new ArrayList<>();
        List<ObservedInvocation> observedInvocations = new ArrayList<>();

        List<String> lines = readLines(profilePath);

        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index).trim();

            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] fields = line.split(FIELD_SEPARATOR, -1);
            int lineNumber = index + 1;

            if (fields[0].equals("hint")) {
                argumentValueHints.add(parseHint(fields, lineNumber));
            } else if (fields[0].equals("invocation")) {
                observedInvocations.add(parseInvocation(fields, lineNumber));
            } else {
                throw new IllegalArgumentException("Unknown observed profile record at line " + lineNumber);
            }
        }

        return new ObservedProfile(argumentValueHints, observedInvocations);
    }

    private List<String> readLines(Path profilePath) {
        try {
            return Files.readAllLines(profilePath);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read observed profile: " + profilePath, exception);
        }
    }

    private ArgumentValueHint parseHint(String[] fields, int lineNumber) {
        if (fields.length != 6) {
            throw new IllegalArgumentException("Invalid hint record at line " + lineNumber);
        }

        return new ArgumentValueHint(
                fields[1],
                fields[2],
                fields[3],
                fields[4],
                fields[5]
        );
    }

    private ObservedInvocation parseInvocation(String[] fields, int lineNumber) {
        if (fields.length < 2 || (fields.length - 2) % 2 != 0) {
            throw new IllegalArgumentException("Invalid invocation record at line " + lineNumber);
        }

        List<GeneratedArgument> arguments = new ArrayList<>();

        for (int index = 2; index < fields.length; index += 2) {
            arguments.add(new GeneratedArgument(fields[index], fields[index + 1]));
        }

        return new ObservedInvocation(fields[1], arguments);
    }
}
