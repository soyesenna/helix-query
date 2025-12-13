package com.soyesenna.helixquery.projection;

import com.soyesenna.helixquery.expression.ConstantExpression;
import com.soyesenna.helixquery.expression.Expression;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TupleResultTest {

    @Test
    void supportsIndexAliasAndExpressionAccess() {
        Tuple tuple = mock(Tuple.class);
        when(tuple.get(0)).thenReturn("Alice");
        when(tuple.get(1)).thenReturn(30);
        when(tuple.get(0, String.class)).thenReturn("Alice");
        when(tuple.get("name")).thenReturn("Alice");
        when(tuple.get("name", String.class)).thenReturn("Alice");
        when(tuple.toArray()).thenReturn(new Object[]{"Alice", 30});
        when(tuple.getElements()).thenReturn(List.of(mock(TupleElement.class), mock(TupleElement.class)));

        Expression<String> nameExpr = ConstantExpression.of("nameExpr");
        Expression<Integer> ageExpr = ConstantExpression.of(1);

        TupleResult result = new TupleResult(tuple, List.of(nameExpr, ageExpr));

        assertEquals("Alice", result.get(0));
        assertEquals("Alice", result.get(0, String.class));
        assertEquals("Alice", result.get("name"));
        assertEquals("Alice", result.get("name", String.class));
        assertEquals("Alice", result.get(nameExpr));
        assertEquals(30, result.get(ageExpr));
        assertArrayEquals(new Object[]{"Alice", 30}, result.toArray());
        assertEquals(2, result.size());
    }

    @Test
    void getByExpressionRequiresMapping() {
        Tuple tuple = mock(Tuple.class);
        TupleResult result = new TupleResult(tuple);

        assertThrows(IllegalStateException.class, () -> result.get(ConstantExpression.of("x")));
    }

    @Test
    void getByExpressionThrowsWhenExpressionNotPresent() {
        Tuple tuple = mock(Tuple.class);
        when(tuple.get(0)).thenReturn("Alice");

        Expression<String> present = ConstantExpression.of("present");
        Expression<String> missing = ConstantExpression.of("missing");

        TupleResult result = new TupleResult(tuple, List.of(present));

        assertThrows(IllegalArgumentException.class, () -> result.get(missing));
    }
}

