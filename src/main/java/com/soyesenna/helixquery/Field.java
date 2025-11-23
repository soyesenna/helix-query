package com.soyesenna.helixquery;

import java.util.function.Function;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.core.types.dsl.SimpleExpression;

/**
 * Typed representation of a single-valued property.
 */
public record Field<T, Q extends EntityPathBase<?>>(
        String name,
        Class<T> type,
        Function<Q, SimpleExpression<T>> pathGetter
) {

    public SimpleExpression<T> path(Q root) {
        return pathGetter.apply(root);
    }

    public OrderSpecifier<? extends Comparable<?>> asc(Q root) {
        SimpleExpression<T> expression = path(root);
        if (expression instanceof ComparableExpressionBase<?> comparable) {
            @SuppressWarnings("unchecked")
            OrderSpecifier<? extends Comparable<?>> order = (OrderSpecifier<? extends Comparable<?>>) comparable.asc();
            return order;
        }
        throw new IllegalStateException("Field " + name + " is not comparable");
    }

    public OrderSpecifier<? extends Comparable<?>> desc(Q root) {
        SimpleExpression<T> expression = path(root);
        if (expression instanceof ComparableExpressionBase<?> comparable) {
            @SuppressWarnings("unchecked")
            OrderSpecifier<? extends Comparable<?>> order = (OrderSpecifier<? extends Comparable<?>>) comparable.desc();
            return order;
        }
        throw new IllegalStateException("Field " + name + " is not comparable");
    }
}
