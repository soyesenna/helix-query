package com.soyesenna.helixquery.field;

import com.soyesenna.helixquery.expression.OperationExpression;
import com.soyesenna.helixquery.expression.PathExpression;
import com.soyesenna.helixquery.expression.PredicateExpression;
import com.soyesenna.helixquery.order.OrderDirection;
import com.soyesenna.helixquery.order.OrderSpecifier;

import java.util.Collection;
import java.util.Objects;

/**
 * Field for numeric types with number-specific operations.
 *
 * @param <T> the numeric type (Integer, Long, Double, etc.)
 */
public record NumberField<T extends Number & Comparable<T>>(
        String name,
        Class<T> type,
        Class<?> entityType
) implements HelixField<T> {

    public NumberField {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(entityType, "entityType must not be null");
    }

    // ==================== Path Access ====================

    public PathExpression<T> path(PathExpression<?> root) {
        return new PathExpression<>(type, name, root);
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

    // ==================== Collection Predicates ====================

    public PredicateExpression in(PathExpression<?> root, Collection<? extends T> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return PredicateExpression.in(path(root), values);
    }

    @SafeVarargs
    public final PredicateExpression in(PathExpression<?> root, T... values) {
        if (values == null || values.length == 0) {
            return null;
        }
        return PredicateExpression.in(path(root), values);
    }

    // ==================== Null Predicates ====================

    public PredicateExpression isNull(PathExpression<?> root) {
        return PredicateExpression.isNull(path(root));
    }

    public PredicateExpression isNotNull(PathExpression<?> root) {
        return PredicateExpression.isNotNull(path(root));
    }

    // ==================== Aggregate Operations ====================

    /**
     * SUM(field)
     */
    public OperationExpression<T> sum(PathExpression<?> root) {
        return OperationExpression.sum(path(root));
    }

    /**
     * AVG(field)
     */
    public OperationExpression<Double> avg(PathExpression<?> root) {
        return OperationExpression.avg(path(root));
    }

    /**
     * MIN(field)
     */
    public OperationExpression<T> min(PathExpression<?> root) {
        return OperationExpression.min(path(root));
    }

    /**
     * MAX(field)
     */
    public OperationExpression<T> max(PathExpression<?> root) {
        return OperationExpression.max(path(root));
    }

    /**
     * COUNT(field)
     */
    public OperationExpression<Long> count(PathExpression<?> root) {
        return OperationExpression.count(path(root));
    }

    /**
     * ABS(field)
     */
    public OperationExpression<T> abs(PathExpression<?> root) {
        return OperationExpression.abs(path(root));
    }

    // ==================== Ordering ====================

    public OrderSpecifier asc(PathExpression<?> root) {
        return new OrderSpecifier(path(root), OrderDirection.ASC);
    }

    public OrderSpecifier desc(PathExpression<?> root) {
        return new OrderSpecifier(path(root), OrderDirection.DESC);
    }
}
