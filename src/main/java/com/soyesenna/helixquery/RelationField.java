package com.soyesenna.helixquery;

import java.util.function.Function;

import com.querydsl.core.types.dsl.EntityPathBase;

/**
 * Represents a relationship property that can be used for joins.
 */
public record RelationField<T, RQ extends EntityPathBase<?>, JQ extends EntityPathBase<T>>(
        String name,
        Class<T> type,
        Function<RQ, JQ> joinPathGetter
) {
    public JQ path(RQ root) {
        return joinPathGetter.apply(root);
    }
}
