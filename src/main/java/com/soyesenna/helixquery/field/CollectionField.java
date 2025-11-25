package com.soyesenna.helixquery.field;

import com.soyesenna.helixquery.expression.OperationExpression;
import com.soyesenna.helixquery.expression.PathExpression;
import com.soyesenna.helixquery.expression.PredicateExpression;

import java.util.Objects;

/**
 * Field representing a collection relationship (OneToMany, ManyToMany, ElementCollection).
 * Provides collection-specific predicates like isEmpty, contains, size.
 *
 * @param <E> the element type of the collection
 */
public record CollectionField<E>(
        String name,
        Class<E> elementType,
        Class<?> entityType
) {

    public CollectionField {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(elementType, "elementType must not be null");
        Objects.requireNonNull(entityType, "entityType must not be null");
    }

    // ==================== Path Access ====================

    /**
     * Get a path expression for this collection.
     * Note: The path type represents the collection itself.
     *
     * @param root the root path expression
     * @return a path expression for the collection
     */
    @SuppressWarnings("unchecked")
    public PathExpression<java.util.Collection<E>> path(PathExpression<?> root) {
        return new PathExpression<>((Class<java.util.Collection<E>>) (Class<?>) java.util.Collection.class, name, root);
    }

    // ==================== Collection Predicates ====================

    /**
     * Check if the collection is empty: collection IS EMPTY
     */
    public PredicateExpression isEmpty(PathExpression<?> root) {
        return PredicateExpression.isEmpty(path(root));
    }

    /**
     * Check if the collection is not empty: collection IS NOT EMPTY
     */
    public PredicateExpression isNotEmpty(PathExpression<?> root) {
        return PredicateExpression.isNotEmpty(path(root));
    }

    /**
     * Check if an element is a member of the collection: element MEMBER OF collection
     */
    public PredicateExpression contains(PathExpression<?> root, E element) {
        Objects.requireNonNull(element, "element must not be null for contains");
        return PredicateExpression.memberOf(
                new com.soyesenna.helixquery.expression.ConstantExpression<>(element, elementType),
                path(root)
        );
    }

    // ==================== Collection Operations ====================

    /**
     * Get the size of the collection: SIZE(collection)
     */
    public OperationExpression<Integer> size(PathExpression<?> root) {
        return OperationExpression.size(path(root));
    }

    /**
     * Check if the collection has at least N elements: SIZE(collection) >= n
     */
    public PredicateExpression hasSizeAtLeast(PathExpression<?> root, int n) {
        return PredicateExpression.ge(size(root), n);
    }

    /**
     * Check if the collection has at most N elements: SIZE(collection) <= n
     */
    public PredicateExpression hasSizeAtMost(PathExpression<?> root, int n) {
        return PredicateExpression.le(size(root), n);
    }

    /**
     * Check if the collection has exactly N elements: SIZE(collection) = n
     */
    public PredicateExpression hasSize(PathExpression<?> root, int n) {
        return PredicateExpression.eq(size(root), n);
    }
}
