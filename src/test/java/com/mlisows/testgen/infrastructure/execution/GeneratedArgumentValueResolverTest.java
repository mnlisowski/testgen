package com.mlisows.testgen.infrastructure.execution;

import com.mlisows.testgen.domain.GeneratedArgument;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeneratedArgumentValueResolverTest {
    private final GeneratedArgumentValueResolver resolver = new GeneratedArgumentValueResolver();

    @Test
    void shouldResolvePrimitiveAndStringLiterals() {
        assertEquals(101, resolver.resolve(argument("int", "101"), int.class, Map.of()));
        assertEquals(1000L, resolver.resolve(argument("long", "1_000L"), long.class, Map.of()));
        assertEquals(10.5, resolver.resolve(argument("double", "10.5"), double.class, Map.of()));
        assertEquals(1.5f, resolver.resolve(argument("float", "1.5f"), float.class, Map.of()));
        assertEquals(true, resolver.resolve(argument("boolean", "true"), boolean.class, Map.of()));
        assertEquals("VIP", resolver.resolve(argument("String", "\"VIP\""), String.class, Map.of()));
    }

    @Test
    void shouldResolveObjectReferenceFromVariables() {
        Object order = new Object();

        Object result = resolver.resolve(argument("Order", "order"), Object.class, Map.of("order", order));

        assertSame(order, result);
    }

    @Test
    void shouldResolveEnumConstants() {
        Object result = resolver.resolve(
                argument("CustomerType", "sample.CustomerType.GOLD"),
                CustomerType.class,
                Map.of()
        );

        assertEquals(CustomerType.GOLD, result);
    }

    @Test
    void shouldFailWhenValueCannotBeResolved() {
        assertThrows(
                IllegalArgumentException.class,
                () -> resolver.resolve(argument("Order", "order"), Object.class, Map.of())
        );
    }

    private static GeneratedArgument argument(String type, String value) {
        return new GeneratedArgument(type, value);
    }

    private enum CustomerType {
        REGULAR,
        GOLD
    }
}
