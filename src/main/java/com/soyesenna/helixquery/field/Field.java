package com.soyesenna.helixquery.field;

import com.soyesenna.helixquery.expression.ConstantExpression;
import com.soyesenna.helixquery.expression.PathExpression;
import com.soyesenna.helixquery.expression.PredicateExpression;
import com.soyesenna.helixquery.order.OrderDirection;
import com.soyesenna.helixquery.order.OrderSpecifier;

import java.util.Collection;
import java.util.Objects;

/**
 * Represents a single-valued field in an entity.
 * Provides type-safe access to entity attributes without Q-type dependencies.
 *
 * @param <T> the field value type
 */
public record Field<T>(
        String name,
        Class<T> type,
        Class<?> entityType
) implements HelixField<T> {

    public Field {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(entityType, "entityType must not be null");
    }

    // ==================== Path Access ====================

    /**
     * Get a path expression for this field.
     *
     * @param root the root path expression
     * @return a path expression for this field
     */
    public PathExpression<T> path(PathExpression<?> root) {
        return new PathExpression<>(type, name, root);
    }

    // ==================== Equality Predicates ====================

    /**
     * Create an equality predicate: field = value
     */
    public PredicateExpression eq(PathExpression<?> root, T value) {
        if (value == null) {
            return isNull(root);
        }
        return PredicateExpression.eq(path(root), value);
    }

    /**
     * Create an inequality predicate: field != value
     */
    public PredicateExpression ne(PathExpression<?> root, T value) {
        if (value == null) {
            return isNotNull(root);
        }
        return PredicateExpression.ne(path(root), value);
    }

    // ==================== Null Predicates ====================

    /**
     * Create a null check predicate: field IS NULL
     */
    public PredicateExpression isNull(PathExpression<?> root) {
        return PredicateExpression.isNull(path(root));
    }

    /**
     * Create a not-null check predicate: field IS NOT NULL
     */
    public PredicateExpression isNotNull(PathExpression<?> root) {
        return PredicateExpression.isNotNull(path(root));
    }

    // ==================== Collection Predicates ====================

    /**
     * Create an IN predicate: field IN (values)
     */
    public PredicateExpression in(PathExpression<?> root, Collection<? extends T> values) {
        if (values == null || values.isEmpty()) {
            return null; // No-op for empty collection
        }
        return PredicateExpression.in(path(root), values);
    }

    /**
     * Create an IN predicate: field IN (values)
     */
    @SafeVarargs
    public final PredicateExpression in(PathExpression<?> root, T... values) {
        if (values == null || values.length == 0) {
            return null;
        }
        return PredicateExpression.in(path(root), values);
    }

    /**
     * Create a NOT IN predicate: field NOT IN (values)
     */
    public PredicateExpression notIn(PathExpression<?> root, Collection<? extends T> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return PredicateExpression.notIn(path(root), values);
    }

    // ==================== Ordering ====================

    /**
     * Create an ascending order specifier.
     */
    public OrderSpecifier asc(PathExpression<?> root) {
        return new OrderSpecifier(path(root), OrderDirection.ASC);
    }

    /**
     * Create a descending order specifier.
     */
    public OrderSpecifier desc(PathExpression<?> root) {
        return new OrderSpecifier(path(root), OrderDirection.DESC);
    }
}
