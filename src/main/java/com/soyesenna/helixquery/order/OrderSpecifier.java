package com.soyesenna.helixquery.order;

import com.soyesenna.helixquery.expression.Expression;

import java.io.Serializable;
import java.util.Objects;

/**
 * Specifies ordering criteria for query results.
 * Supports direction (ASC/DESC) and null handling.
 */
public record OrderSpecifier(
        Expression<?> target,
        OrderDirection direction,
        NullHandling nullHandling
) implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Null handling strategy for ordering.
     */
    public enum NullHandling {
        /**
         * Use database default null handling.
         */
        DEFAULT,
        /**
         * Sort null values first.
         */
        NULLS_FIRST,
        /**
         * Sort null values last.
         */
        NULLS_LAST
    }

    /**
     * Create an order specifier with default null handling.
     *
     * @param target    the expression to order by
     * @param direction the order direction
     */
    public OrderSpecifier(Expression<?> target, OrderDirection direction) {
        this(target, direction, NullHandling.DEFAULT);
    }

    public OrderSpecifier {
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(direction, "direction must not be null");
        Objects.requireNonNull(nullHandling, "nullHandling must not be null");
    }

    /**
     * Create a new order specifier with NULLS FIRST handling.
     */
    public OrderSpecifier nullsFirst() {
        return new OrderSpecifier(target, direction, NullHandling.NULLS_FIRST);
    }

    /**
     * Create a new order specifier with NULLS LAST handling.
     */
    public OrderSpecifier nullsLast() {
        return new OrderSpecifier(target, direction, NullHandling.NULLS_LAST);
    }

    /**
     * Check if this is ascending order.
     */
    public boolean isAscending() {
        return direction == OrderDirection.ASC;
    }

    /**
     * Check if this is descending order.
     */
    public boolean isDescending() {
        return direction == OrderDirection.DESC;
    }

    // ==================== Static Factory Methods ====================

    /**
     * Create an ascending order specifier.
     *
     * @param target the expression to order by
     * @return an ascending order specifier
     */
    public static OrderSpecifier asc(Expression<?> target) {
        return new OrderSpecifier(target, OrderDirection.ASC);
    }

    /**
     * Create a descending order specifier.
     *
     * @param target the expression to order by
     * @return a descending order specifier
     */
    public static OrderSpecifier desc(Expression<?> target) {
        return new OrderSpecifier(target, OrderDirection.DESC);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("OrderSpecifier[");
        sb.append(target);
        sb.append(" ").append(direction);
        if (nullHandling != NullHandling.DEFAULT) {
            sb.append(" ").append(nullHandling);
        }
        sb.append("]");
        return sb.toString();
    }
}
