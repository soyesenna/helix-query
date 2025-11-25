package com.soyesenna.helixquery.field;

import com.soyesenna.helixquery.expression.PathExpression;
import com.soyesenna.helixquery.expression.PredicateExpression;
import com.soyesenna.helixquery.order.OrderSpecifier;

/**
 * Common interface for all field types in HelixQuery.
 * Enables unified handling of different field types without method overloading.
 *
 * @param <T> the field value type
 */
public interface HelixField<T> {

    /**
     * Get the field name.
     *
     * @return the field name
     */
    String name();

    /**
     * Get the entity type that owns this field.
     *
     * @return the entity class
     */
    Class<?> entityType();

    /**
     * Get a path expression for this field.
     *
     * @param root the root path expression
     * @return a path expression for this field
     */
    PathExpression<?> path(PathExpression<?> root);

    /**
     * Create an equality predicate: field = value
     *
     * @param root  the root path expression
     * @param value the value to compare
     * @return the predicate expression
     */
    PredicateExpression eq(PathExpression<?> root, T value);

    /**
     * Create an inequality predicate: field != value
     *
     * @param root  the root path expression
     * @param value the value to compare
     * @return the predicate expression
     */
    PredicateExpression ne(PathExpression<?> root, T value);

    /**
     * Create a null check predicate: field IS NULL
     *
     * @param root the root path expression
     * @return the predicate expression
     */
    PredicateExpression isNull(PathExpression<?> root);

    /**
     * Create a not-null check predicate: field IS NOT NULL
     *
     * @param root the root path expression
     * @return the predicate expression
     */
    PredicateExpression isNotNull(PathExpression<?> root);

    /**
     * Create an ascending order specifier.
     *
     * @param root the root path expression
     * @return the order specifier
     */
    OrderSpecifier asc(PathExpression<?> root);

    /**
     * Create a descending order specifier.
     *
     * @param root the root path expression
     * @return the order specifier
     */
    OrderSpecifier desc(PathExpression<?> root);
}
