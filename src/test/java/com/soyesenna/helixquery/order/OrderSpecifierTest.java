package com.soyesenna.helixquery.order;

import com.soyesenna.helixquery.expression.ConstantExpression;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderSpecifierTest {

    @Test
    void createsNullHandlingVariants() {
        OrderSpecifier asc = new OrderSpecifier(ConstantExpression.of(1), OrderDirection.ASC);
        assertTrue(asc.isAscending());
        assertFalse(asc.isDescending());
        assertEquals(OrderSpecifier.NullHandling.DEFAULT, asc.nullHandling());

        OrderSpecifier nullsFirst = asc.nullsFirst();
        assertEquals(OrderSpecifier.NullHandling.NULLS_FIRST, nullsFirst.nullHandling());

        OrderSpecifier nullsLast = asc.nullsLast();
        assertEquals(OrderSpecifier.NullHandling.NULLS_LAST, nullsLast.nullHandling());
    }

    @Test
    void validatesConstructorArguments() {
        assertThrows(NullPointerException.class, () -> new OrderSpecifier(null, OrderDirection.ASC));
        assertThrows(NullPointerException.class, () -> new OrderSpecifier(ConstantExpression.of(1), null));
        assertThrows(NullPointerException.class, () -> new OrderSpecifier(ConstantExpression.of(1), OrderDirection.ASC, null));
    }
}

