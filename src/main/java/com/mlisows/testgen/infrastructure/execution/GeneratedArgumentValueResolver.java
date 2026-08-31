package com.mlisows.testgen.infrastructure.execution;

import com.mlisows.testgen.domain.GeneratedArgument;

import java.util.Map;
import java.util.Objects;

final class GeneratedArgumentValueResolver {

    Object resolve(GeneratedArgument argument, Class<?> targetType, Map<String, Object> variables) {
        Objects.requireNonNull(argument, "argument must not be null");
        Objects.requireNonNull(targetType, "targetType must not be null");
        Objects.requireNonNull(variables, "variables must not be null");

        String value = argument.getValue();

        if (value.equals("null")) {
            return null;
        }

        if (variables.containsKey(value)) {
            return variables.get(value);
        }

        if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(normalizeIntegerLiteral(value));
        }

        if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(normalizeLongLiteral(value));
        }

        if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(normalizeFloatingPointLiteral(value));
        }

        if (targetType == float.class || targetType == Float.class) {
            return Float.parseFloat(normalizeFloatingPointLiteral(value));
        }

        if (targetType == boolean.class || targetType == Boolean.class) {
            return resolveBoolean(value);
        }

        if (targetType == String.class) {
            return resolveString(value);
        }

        if (targetType.isEnum()) {
            return resolveEnum(targetType, value);
        }

        throw new IllegalArgumentException(
                "Cannot resolve generated argument " + value + " as " + targetType.getName()
        );
    }

    private String normalizeIntegerLiteral(String value) {
        return value.replace("_", "");
    }

    private String normalizeLongLiteral(String value) {
        return value.replace("_", "")
                .replaceAll("[lL]$", "");
    }

    private String normalizeFloatingPointLiteral(String value) {
        return value.replace("_", "")
                .replaceAll("[fFdD]$", "");
    }

    private Boolean resolveBoolean(String value) {
        if (value.equals("true")) {
            return true;
        }

        if (value.equals("false")) {
            return false;
        }

        throw new IllegalArgumentException("Cannot resolve generated argument " + value + " as boolean");
    }

    private String resolveString(String value) {
        if (value.length() < 2 || !value.startsWith("\"") || !value.endsWith("\"")) {
            throw new IllegalArgumentException("Cannot resolve generated argument " + value + " as String");
        }

        return unescapeJavaString(value.substring(1, value.length() - 1));
    }

    private String unescapeJavaString(String value) {
        StringBuilder result = new StringBuilder();
        boolean escaped = false;

        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);

            if (!escaped && current == '\\') {
                escaped = true;
                continue;
            }

            if (escaped) {
                result.append(unescapedCharacter(current));
                escaped = false;
                continue;
            }

            result.append(current);
        }

        if (escaped) {
            result.append('\\');
        }

        return result.toString();
    }

    private char unescapedCharacter(char character) {
        return switch (character) {
            case 'b' -> '\b';
            case 'f' -> '\f';
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            case '\\' -> '\\';
            case '"' -> '"';
            case '\'' -> '\'';
            default -> character;
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Enum<?> resolveEnum(Class<?> targetType, String value) {
        String constantName = value.contains(".")
                ? value.substring(value.lastIndexOf('.') + 1)
                : value;

        return Enum.valueOf((Class<? extends Enum>) targetType.asSubclass(Enum.class), constantName);
    }
}
