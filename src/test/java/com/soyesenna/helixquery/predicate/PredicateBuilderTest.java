package com.soyesenna.helixquery.predicate;

import com.soyesenna.helixquery.expression.ConstantExpression;
import com.soyesenna.helixquery.expression.Operator;
import com.soyesenna.helixquery.expression.PredicateExpression;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class PredicateBuilderTest {

    @Test
    void buildRequiredThrowsWhenEmpty() {
        PredicateBuilder builder = new PredicateBuilder();
        assertThrows(IllegalStateException.class, builder::buildRequired);
    }

    @Test
    void andOrNotOperationsComposePredicates() {
        PredicateExpression p1 = PredicateExpression.eq(ConstantExpression.of(1), 1);
        PredicateExpression p2 = PredicateExpression.eq(ConstantExpression.of(1), 2);

        PredicateBuilder builder = new PredicateBuilder()
                .and(p1)
                .and(null)
                .or(p2);

        PredicateExpression built = builder.build();

        assertNotNull(built);
        assertEquals(Operator.OR, built.getOperator());
        assertEquals(p1, built.getArg(0));
        assertEquals(p2, built.getArg(1));

        builder.not();
        assertEquals(Operator.NOT, builder.build().getOperator());
    }

    @Test
    void conditionalAddsOnlyWhenConditionMatches() {
        PredicateExpression p = PredicateExpression.eq(ConstantExpression.of(1), 1);
        AtomicInteger calls = new AtomicInteger();

        PredicateBuilder builder = new PredicateBuilder()
                .andIf(false, p)
                .andIf(true, () -> {
                    calls.incrementAndGet();
                    return p;
                })
                .andIfNotNull(null, () -> {
                    calls.incrementAndGet();
                    return p;
                })
                .andIfNotNull("x", () -> {
                    calls.incrementAndGet();
                    return p;
                })
                .andIfNotEmpty("", () -> {
                    calls.incrementAndGet();
                    return p;
                })
                .andIfNotEmpty("x", () -> {
                    calls.incrementAndGet();
                    return p;
                });

        assertTrue(builder.hasValue());
        assertEquals(3, calls.get());
    }

    @Test
    void nestedGroupsOnlyApplyWhenNestedHasValue() {
        PredicateExpression p1 = PredicateExpression.eq(ConstantExpression.of(1), 1);
        PredicateExpression p2 = PredicateExpression.eq(ConstantExpression.of(1), 2);

        PredicateBuilder builder = new PredicateBuilder()
                .and(p1)
                .andGroup(group -> group.or(p2))
                .orGroup(group -> group.and(null));

        PredicateExpression built = builder.build();

        assertNotNull(built);
        assertEquals(Operator.AND, built.getOperator());
    }

    @Test
    void resetClearsState() {
        PredicateExpression p = PredicateExpression.eq(ConstantExpression.of(1), 1);
        PredicateBuilder builder = new PredicateBuilder(p);

        assertTrue(builder.hasValue());
        builder.reset();
        assertFalse(builder.hasValue());
        assertNull(builder.build());
    }
}

