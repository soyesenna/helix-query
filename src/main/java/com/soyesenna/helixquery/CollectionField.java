package com.soyesenna.helixquery;

import java.util.function.Function;

import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.CollectionExpressionBase;

/**
 * Represents collection-valued association or element collection fields.
 * Provides access to the underlying Querydsl collection path for advanced usage.
 */
public record CollectionField<E, Q extends EntityPathBase<?>, P extends CollectionExpressionBase<?, E>>(
        String name,
        Class<E> elementType,
        Function<Q, P> pathGetter
) {
    public P path(Q root) {
        return pathGetter.apply(root);
    }

    /**
     * Convenience method for checking collection contains on the path.
     */
    public com.querydsl.core.types.Predicate contains(Q root, E value) {
        return value == null ? null : path(root).contains(value);
    }

    public com.querydsl.core.types.Predicate isEmpty(Q root) {
        return path(root).isEmpty();
    }

    public com.querydsl.core.types.Predicate isNotEmpty(Q root) {
        return path(root).isNotEmpty();
    }
}
