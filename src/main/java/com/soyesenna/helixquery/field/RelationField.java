package com.soyesenna.helixquery.field;

import com.soyesenna.helixquery.expression.PathExpression;
import com.soyesenna.helixquery.expression.PredicateExpression;
import com.soyesenna.helixquery.order.OrderDirection;
import com.soyesenna.helixquery.order.OrderSpecifier;

import java.util.Objects;

/**
 * Field representing a relationship (ManyToOne, OneToOne) to another entity.
 * Provides navigation to related entity fields.
 *
 * @param <T> the related entity type
 */
public record RelationField<T>(
        String name,
        Class<T> targetType,
        Class<?> sourceEntityType
) implements HelixField<T> {

    public RelationField {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(targetType, "targetType must not be null");
        Objects.requireNonNull(sourceEntityType, "sourceEntityType must not be null");
    }

    // ==================== Path Access ====================

    /**
     * Get a path expression for this relation.
     *
     * @param root the root path expression
     * @return a path expression for the related entity
     */
    public PathExpression<T> path(PathExpression<?> root) {
        return new PathExpression<>(targetType, name, root);
    }

    /**
     * Navigate to an attribute of the related entity.
     *
     * @param root          the root path expression
     * @param attribute     the attribute name in the related entity
     * @param attributeType the attribute type
     * @param <U>           the attribute type
     * @return a path expression for the nested attribute
     */
    public <U> PathExpression<U> get(PathExpression<?> root, String attribute, Class<U> attributeType) {
        return path(root).get(attribute, attributeType);
    }

    // ==================== Equality Predicates ====================

    /**
     * Create an equality predicate: relation = entity
     */
    public PredicateExpression eq(PathExpression<?> root, T entity) {
        if (entity == null) {
            return isNull(root);
        }
        return PredicateExpression.eq(path(root), entity);
    }

    /**
     * Create an inequality predicate: relation != entity
     */
    public PredicateExpression ne(PathExpression<?> root, T entity) {
        if (entity == null) {
            return isNotNull(root);
        }
        return PredicateExpression.ne(path(root), entity);
    }

    // ==================== Null Predicates ====================

    /**
     * Check if the relation is null: relation IS NULL
     */
    public PredicateExpression isNull(PathExpression<?> root) {
        return PredicateExpression.isNull(path(root));
    }

    /**
     * Check if the relation is not null: relation IS NOT NULL
     */
    public PredicateExpression isNotNull(PathExpression<?> root) {
        return PredicateExpression.isNotNull(path(root));
    }

    // ==================== HelixField Interface ====================

    /**
     * Get the entity type that owns this relation field.
     * Alias for sourceEntityType() for HelixField compatibility.
     *
     * @return the source entity class
     */
    @Override
    public Class<?> entityType() {
        return sourceEntityType;
    }

    // ==================== Ordering ====================

    /**
     * Create an ascending order specifier.
     *
     * @param root the root path expression
     * @return the order specifier
     */
    @Override
    public OrderSpecifier asc(PathExpression<?> root) {
        return new OrderSpecifier(path(root), OrderDirection.ASC);
    }

    /**
     * Create a descending order specifier.
     *
     * @param root the root path expression
     * @return the order specifier
     */
    @Override
    public OrderSpecifier desc(PathExpression<?> root) {
        return new OrderSpecifier(path(root), OrderDirection.DESC);
    }
}
