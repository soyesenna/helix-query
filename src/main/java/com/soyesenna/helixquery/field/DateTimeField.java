package com.soyesenna.helixquery.field;

import com.soyesenna.helixquery.expression.PathExpression;
import com.soyesenna.helixquery.expression.PredicateExpression;
import com.soyesenna.helixquery.order.OrderDirection;
import com.soyesenna.helixquery.order.OrderSpecifier;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.Temporal;
import java.util.Objects;

/**
 * Field for date/time types with temporal-specific operations.
 *
 * @param <T> the temporal type (LocalDate, LocalDateTime, LocalTime, etc.)
 */
public record DateTimeField<T extends Temporal & Comparable<? super T>>(
        String name,
        Class<T> type,
        Class<?> entityType
) {

    public DateTimeField {
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

    // ==================== Temporal Comparison Predicates ====================

    /**
     * Before (less than): field < value
     */
    public PredicateExpression before(PathExpression<?> root, T value) {
        Objects.requireNonNull(value, "value must not be null for before");
        return PredicateExpression.lt(path(root), value);
    }

    /**
     * After (greater than): field > value
     */
    public PredicateExpression after(PathExpression<?> root, T value) {
        Objects.requireNonNull(value, "value must not be null for after");
        return PredicateExpression.gt(path(root), value);
    }

    /**
     * On or before: field <= value
     */
    public PredicateExpression onOrBefore(PathExpression<?> root, T value) {
        Objects.requireNonNull(value, "value must not be null for onOrBefore");
        return PredicateExpression.le(path(root), value);
    }

    /**
     * On or after: field >= value
     */
    public PredicateExpression onOrAfter(PathExpression<?> root, T value) {
        Objects.requireNonNull(value, "value must not be null for onOrAfter");
        return PredicateExpression.ge(path(root), value);
    }

    /**
     * Between (inclusive): from <= field <= to
     */
    public PredicateExpression between(PathExpression<?> root, T from, T to) {
        Objects.requireNonNull(from, "from must not be null for between");
        Objects.requireNonNull(to, "to must not be null for between");
        return PredicateExpression.between(path(root), from, to);
    }

    // ==================== "Now" Convenience Methods ====================

    /**
     * Before current time: field < now()
     * Only supported for LocalDateTime.
     */
    @SuppressWarnings("unchecked")
    public PredicateExpression beforeNow(PathExpression<?> root) {
        if (type.equals(LocalDateTime.class)) {
            return before(root, (T) LocalDateTime.now());
        } else if (type.equals(LocalDate.class)) {
            return before(root, (T) LocalDate.now());
        } else if (type.equals(LocalTime.class)) {
            return before(root, (T) LocalTime.now());
        }
        throw new UnsupportedOperationException(
                "beforeNow is only supported for LocalDateTime, LocalDate, or LocalTime, not " + type.getSimpleName());
    }

    /**
     * After current time: field > now()
     * Only supported for LocalDateTime.
     */
    @SuppressWarnings("unchecked")
    public PredicateExpression afterNow(PathExpression<?> root) {
        if (type.equals(LocalDateTime.class)) {
            return after(root, (T) LocalDateTime.now());
        } else if (type.equals(LocalDate.class)) {
            return after(root, (T) LocalDate.now());
        } else if (type.equals(LocalTime.class)) {
            return after(root, (T) LocalTime.now());
        }
        throw new UnsupportedOperationException(
                "afterNow is only supported for LocalDateTime, LocalDate, or LocalTime, not " + type.getSimpleName());
    }

    /**
     * On or before current time: field <= now()
     */
    @SuppressWarnings("unchecked")
    public PredicateExpression onOrBeforeNow(PathExpression<?> root) {
        if (type.equals(LocalDateTime.class)) {
            return onOrBefore(root, (T) LocalDateTime.now());
        } else if (type.equals(LocalDate.class)) {
            return onOrBefore(root, (T) LocalDate.now());
        } else if (type.equals(LocalTime.class)) {
            return onOrBefore(root, (T) LocalTime.now());
        }
        throw new UnsupportedOperationException(
                "onOrBeforeNow is only supported for LocalDateTime, LocalDate, or LocalTime");
    }

    /**
     * On or after current time: field >= now()
     */
    @SuppressWarnings("unchecked")
    public PredicateExpression onOrAfterNow(PathExpression<?> root) {
        if (type.equals(LocalDateTime.class)) {
            return onOrAfter(root, (T) LocalDateTime.now());
        } else if (type.equals(LocalDate.class)) {
            return onOrAfter(root, (T) LocalDate.now());
        } else if (type.equals(LocalTime.class)) {
            return onOrAfter(root, (T) LocalTime.now());
        }
        throw new UnsupportedOperationException(
                "onOrAfterNow is only supported for LocalDateTime, LocalDate, or LocalTime");
    }

    // ==================== Null Predicates ====================

    public PredicateExpression isNull(PathExpression<?> root) {
        return PredicateExpression.isNull(path(root));
    }

    public PredicateExpression isNotNull(PathExpression<?> root) {
        return PredicateExpression.isNotNull(path(root));
    }

    // ==================== Ordering ====================

    public OrderSpecifier asc(PathExpression<?> root) {
        return new OrderSpecifier(path(root), OrderDirection.ASC);
    }

    public OrderSpecifier desc(PathExpression<?> root) {
        return new OrderSpecifier(path(root), OrderDirection.DESC);
    }
}
