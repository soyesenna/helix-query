package com.soyesenna.helixquery.expression;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConstantExpressionTest {

    @Test
    void constructorRejectsNullWhenTypeIsNotProvided() {
        assertThrows(IllegalArgumentException.class, () -> new ConstantExpression<>(null));
    }

    @Test
    void ofNullCreatesNullableConstant() {
        ConstantExpression<String> expr = ConstantExpression.ofNull(String.class);
        assertTrue(expr.isNull());
        assertNull(expr.getValue());
        assertEquals(String.class, expr.getType());
    }

    @Test
    void ofCollectionWrapsCollection() {
        List<Integer> values = List.of(1, 2, 3);
        ConstantExpression<?> expr = ConstantExpression.ofCollection(values);
        assertEquals(values, expr.getValue());
        assertEquals(java.util.Collection.class, expr.getType());
    }
}

