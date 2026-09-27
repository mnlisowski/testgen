package com.mlisows.testgen.infrastructure.runtime;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class ArgumentRecorder {
    private static final String SOURCE = "runtime-observed";
    private static final String PROFILE_DIR_PROPERTY = "testgen.profile.dir";
    private static final String PROFILE_FILE_NAME = "observed-profile.txt";
    private static final Set<String> profileLines = new LinkedHashSet<>();

    private static final Map<String, Integer> recordCounts = new HashMap<>();
    private static int maxValuesPerArgument = -1;
    private static int maxInvocationsPerOwner;

    private ArgumentRecorder() {
    }

    public static synchronized void record(
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

        initializeLimits();
        String invocationKey = "invocation|" + ownerId;
        boolean invocationCanBeReplayed = recordCounts.getOrDefault(invocationKey, 0)
                < maxInvocationsPerOwner;
        List<String> invocationFields = new ArrayList<>();
        if (invocationCanBeReplayed) {
            invocationFields.add("invocation");
            invocationFields.add(ownerId);
        }

        for (int index = 0; index < argumentValues.length; index++) {
            Object value = argumentValues[index];
            if (!isRecordable(value)) {
                invocationCanBeReplayed = false;
                continue;
            }

            String hintKey = "hint|" + ownerId + "|" + index;
            boolean needsHint = recordCounts.getOrDefault(hintKey, 0) < maxValuesPerArgument;
            if (!needsHint && !invocationCanBeReplayed) {
                continue;
            }

            String literal = javaLiteral(value);
            if (needsHint) {
                addProfileLine(
                        String.join("|", "hint", ownerId, argumentNames[index], argumentTypes[index], literal, SOURCE),
                        hintKey,
                        maxValuesPerArgument
                );
            }
            if (invocationCanBeReplayed) {
                invocationFields.add(argumentTypes[index]);
                invocationFields.add(literal);
            }
        }

        if (invocationCanBeReplayed) {
            addProfileLine(String.join("|", invocationFields), invocationKey, maxInvocationsPerOwner);
        }
    }

    private static void initializeLimits() {
        if (maxValuesPerArgument >= 0) {
            return;
        }
        int values = readLimit("testgen.profile.maxValuesPerArgument");
        int invocations = readLimit("testgen.profile.maxInvocationsPerOwner");
        maxValuesPerArgument = values;
        maxInvocationsPerOwner = invocations;
    }

    private static int readLimit(String property) {
        int value = Integer.parseInt(System.getProperty(property, "100"));
        if (value < 0) {
            throw new IllegalArgumentException(property + " must be nonnegative");
        }
        return value;
    }

    public static synchronized List<String> snapshotLines() {
        return List.copyOf(profileLines);
    }

    public static synchronized void writeTo(Path profilePath) {
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

    public static synchronized void reset() {
        profileLines.clear();
        recordCounts.clear();
        maxValuesPerArgument = -1;
    }

    private static void addProfileLine(String line, String key, int limit) {
        if (recordCounts.getOrDefault(key, 0) >= limit || profileLines.contains(line)) {
            return;
        }
        liveProfilePath().ifPresent(path -> appendLine(path, line));
        profileLines.add(line);
        recordCounts.merge(key, 1, Integer::sum);
    }

    private static Optional<Path> liveProfilePath() {
        String profileDir = System.getProperty(PROFILE_DIR_PROPERTY);

        if (profileDir == null || profileDir.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(Path.of(profileDir).resolve(PROFILE_FILE_NAME));
    }

    private static void appendLine(Path profilePath, String line) {
        try {
            Path parent = profilePath.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.writeString(
                    profilePath,
                    line + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot append observed profile line: " + profilePath, exception);
        }
    }

    private static boolean isRecordable(Object value) {
        if (value == null) {
            return true;
        }

        if (value instanceof String || value instanceof Character) {
            return true;
        }

        return value instanceof Byte
                || value instanceof Short
                || value instanceof Integer
                || value instanceof Long
                || value instanceof Float
                || value instanceof Double
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

        if (value instanceof Boolean booleanValue) {
            return Boolean.toString(booleanValue);
        }

        if (value instanceof Byte byteValue) {
            return Byte.toString(byteValue);
        }

        if (value instanceof Short shortValue) {
            return Short.toString(shortValue);
        }

        if (value instanceof Integer integerValue) {
            return Integer.toString(integerValue);
        }

        if (value instanceof Long longValue) {
            return Long.toString(longValue);
        }

        if (value instanceof Float floatValue) {
            return Float.toString(floatValue) + "f";
        }

        if (value instanceof Double doubleValue) {
            return Double.toString(doubleValue);
        }

        return value.toString();
    }

    private static String escapeString(String value) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            result.append(character == '"' ? "\\\"" : escapeCharacter(character));
        }
        return result.toString();
    }

    private static String escapeCharacter(char value) {
        return switch (value) {
            case '\\' -> "\\\\";
            case '\'' -> "\\'";
            case '\n' -> "\\n";
            case '\r' -> "\\r";
            case '\t' -> "\\t";
            case '\b' -> "\\b";
            case '\f' -> "\\f";
            default -> {
                if (Character.isISOControl(value) || Character.isSurrogate(value) || value == '|') {
                    String hex = Integer.toHexString(value);
                    yield "\\u" + "0".repeat(4 - hex.length()) + hex;
                }
                yield Character.toString(value);
            }
        };
    }
}
