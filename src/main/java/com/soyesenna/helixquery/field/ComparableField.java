package com.soyesenna.helixquery.field;

import com.soyesenna.helixquery.expression.PathExpression;
import com.soyesenna.helixquery.expression.PredicateExpression;

import java.util.Objects;

/**
 * Field for Comparable types supporting comparison operations.
 *
 * @param <T> the comparable field type
 */
public record ComparableField<T extends Comparable<? super T>>(
        String name,
        Class<T> type,
        Class<?> entityType,
        String relationPath
) implements HelixField<T> {

    /**
     * Canonical constructor with all parameters.
     */
    public ComparableField {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(entityType, "entityType must not be null");
        // relationPath can be null
    }

    /**
     * Constructor without relationPath (for regular fields).
     */
    public ComparableField(String name, Class<T> type, Class<?> entityType) {
        this(name, type, entityType, null);
    }

    @Override
    public String relationPath() {
        return relationPath;
    }

    // ==================== Path Access ====================

    public PathExpression<T> path(PathExpression<?> root) {
        return new PathExpression<>(type, name, root, relationPath);
    }

    // ==================== Equality Predicates ====================

    public PredicateExpression eq(PathExpression<?> root, T value) {
        if (value == null) {
            return isNull(root);
        }
        return PredicateExpression.eq(path(root), value);
    }

    public PredicateExpression ne(PathExpression<?> root, T value) {
        if (value == null) {
            return isNotNull(root);
        }
        return PredicateExpression.ne(path(root), value);
    }

    // ==================== Comparison Predicates ====================

    /**
     * Greater than: field > value
     */
    public PredicateExpression gt(PathExpression<?> root, T value) {
        Objects.requireNonNull(value, "value must not be null for gt");
        return PredicateExpression.gt(path(root), value);
    }

    /**
     * Greater than or equal: field >= value
     */
    public PredicateExpression ge(PathExpression<?> root, T value) {
        Objects.requireNonNull(value, "value must not be null for ge");
        return PredicateExpression.ge(path(root), value);
    }

    /**
     * Less than: field < value
     */
    public PredicateExpression lt(PathExpression<?> root, T value) {
        Objects.requireNonNull(value, "value must not be null for lt");
        return PredicateExpression.lt(path(root), value);
    }

    /**
     * Less than or equal: field <= value
     */
    public PredicateExpression le(PathExpression<?> root, T value) {
        Objects.requireNonNull(value, "value must not be null for le");
        return PredicateExpression.le(path(root), value);
    }

    /**
     * Between (inclusive): from <= field <= to
     */
    public PredicateExpression between(PathExpression<?> root, T from, T to) {
        Objects.requireNonNull(from, "from must not be null for between");
        Objects.requireNonNull(to, "to must not be null for between");
        return PredicateExpression.between(path(root), from, to);
    }

    // ==================== Null Predicates ====================

    public PredicateExpression isNull(PathExpression<?> root) {
        return PredicateExpression.isNull(path(root));
    }

    public PredicateExpression isNotNull(PathExpression<?> root) {
        return PredicateExpression.isNotNull(path(root));
    }

    // ==================== Ordering ====================

    public com.soyesenna.helixquery.order.OrderSpecifier asc(PathExpression<?> root) {
        return new com.soyesenna.helixquery.order.OrderSpecifier(
                path(root), com.soyesenna.helixquery.order.OrderDirection.ASC);
    }

    public com.soyesenna.helixquery.order.OrderSpecifier desc(PathExpression<?> root) {
        return new com.soyesenna.helixquery.order.OrderSpecifier(
                path(root), com.soyesenna.helixquery.order.OrderDirection.DESC);
    }
}
