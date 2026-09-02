package com.mlisows.testgen.infrastructure.runtime;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ArgumentRecorder {
    private static final String SOURCE = "runtime-observed";
    private static final Set<String> profileLines = new LinkedHashSet<>();

    private ArgumentRecorder() {
    }

    public static void record(
            String ownerId,
            String[] argumentNames,
            String[] argumentTypes,
            Object[] argumentValues
    ) {
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        Objects.requireNonNull(argumentNames, "argumentNames must not be null");
        Objects.requireNonNull(argumentTypes, "argumentTypes must not be null");
        Objects.requireNonNull(argumentValues, "argumentValues must not be null");

        if (argumentNames.length != argumentTypes.length || argumentNames.length != argumentValues.length) {
            throw new IllegalArgumentException("Argument data must have the same size as argument values");
        }

        List<String> invocationFields = new ArrayList<>();
        invocationFields.add("invocation");
        invocationFields.add(ownerId);

        boolean invocationCanBeReplayed = true;

        for (int index = 0; index < argumentValues.length; index++) {
            Object argumentValue = argumentValues[index];

            if (isRecordable(argumentValue)) {
                String literal = javaLiteral(argumentValue);

                profileLines.add(String.join(
                        "|",
                        "hint",
                        ownerId,
                        argumentNames[index],
                        argumentTypes[index],
                        literal,
                        SOURCE
                ));

                invocationFields.add(argumentTypes[index]);
                invocationFields.add(literal);
            } else {
                invocationCanBeReplayed = false;
            }
        }

        if (invocationCanBeReplayed) {
            profileLines.add(String.join("|", invocationFields));
        }
    }

    public static List<String> snapshotLines() {
        return List.copyOf(profileLines);
    }

    public static void writeTo(Path profilePath) {
        Objects.requireNonNull(profilePath, "profilePath must not be null");

        try {
            Path parent = profilePath.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.write(profilePath, profileLines, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write observed profile: " + profilePath, exception);
        }
    }

    public static void reset() {
        profileLines.clear();
    }

    private static boolean isRecordable(Object value) {
        return value == null
                || value instanceof String
                || value instanceof Character
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Enum<?>;
    }

    private static String javaLiteral(Object value) {
        if (value == null) {
            return "null";
        }

        if (value instanceof String stringValue) {
            return "\"" + escapeString(stringValue) + "\"";
        }

        if (value instanceof Character characterValue) {
            return "'" + escapeCharacter(characterValue) + "'";
        }

        if (value instanceof Enum<?> enumValue) {
            return enumValue.getDeclaringClass().getCanonicalName() + "." + enumValue.name();
        }

        return value.toString();
    }

    private static String escapeString(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String escapeCharacter(char value) {
        return switch (value) {
            case '\\' -> "\\\\";
            case '\'' -> "\\'";
            case '\n' -> "\\n";
            case '\r' -> "\\r";
            case '\t' -> "\\t";
            default -> Character.toString(value);
        };
    }
}
