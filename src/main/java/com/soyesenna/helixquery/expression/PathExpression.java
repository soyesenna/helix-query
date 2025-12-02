package com.soyesenna.helixquery.expression;

import java.util.Objects;

/**
 * Expression representing a path to an entity attribute.
 * Paths can be nested (e.g., "user.department.name").
 *
 * @param <T> the type of the attribute
 */
public class PathExpression<T> implements Expression<T> {

    private static final long serialVersionUID = 1L;

    private final Class<T> type;
    private final String attributeName;
    private final PathExpression<?> parent;
    private final Class<?> rootType;
    private final String relationPath;

    /**
     * Create a root path expression for an entity.
     *
     * @param rootType the entity class
     */
    @SuppressWarnings("unchecked")
    public PathExpression(Class<T> rootType) {
        this.type = rootType;
        this.attributeName = null;
        this.parent = null;
        this.rootType = rootType;
        this.relationPath = null;
    }

    /**
     * Create an attribute path expression.
     *
     * @param type          the attribute type
     * @param attributeName the attribute name
     * @param parent        the parent path
     */
    public PathExpression(Class<T> type, String attributeName, PathExpression<?> parent) {
        this(type, attributeName, parent, null);
    }

    /**
     * Create an attribute path expression with a relation path for auto-join.
     *
     * @param type          the attribute type
     * @param attributeName the attribute name (can be a nested path like "department.name")
     * @param parent        the parent path
     * @param relationPath  the relation path to auto-join (e.g., "department")
     */
    public PathExpression(Class<T> type, String attributeName, PathExpression<?> parent, String relationPath) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.attributeName = Objects.requireNonNull(attributeName, "attributeName must not be null");
        this.parent = Objects.requireNonNull(parent, "parent must not be null");
        this.rootType = parent.getRootType();
        this.relationPath = relationPath;
    }

    /**
     * Get the relation path for auto-join.
     * When this path references a field through a relation (e.g., "department.name"),
     * this returns the relation path ("department") that should be automatically joined.
     *
     * @return the relation path to auto-join, or null if not applicable
     */
    public String getRelationPath() {
        return relationPath;
    }

    @Override
    public Class<? extends T> getType() {
        return type;
    }

    /**
     * Check if this is a root path (entity itself, not an attribute).
     */
    public boolean isRoot() {
        return parent == null;
    }

    /**
     * Get the attribute name (null for root paths).
     */
    public String getAttributeName() {
        return attributeName;
    }

    /**
     * Get the parent path (null for root paths).
     */
    public PathExpression<?> getParent() {
        return parent;
    }

    /**
     * Get the root entity type.
     */
    public Class<?> getRootType() {
        return rootType;
    }

    /**
     * Get the full dot-separated path (e.g., "department.name").
     * Returns empty string for root paths.
     */
    public String getFullPath() {
        if (isRoot()) {
            return "";
        }
        if (parent.isRoot()) {
            return attributeName;
        }
        return parent.getFullPath() + "." + attributeName;
    }

    /**
     * Navigate to a nested attribute.
     *
     * @param attribute     the attribute name
     * @param attributeType the attribute type
     * @param <U>           the attribute type
     * @return a new path expression for the nested attribute
     */
    public <U> PathExpression<U> get(String attribute, Class<U> attributeType) {
        return new PathExpression<>(attributeType, attribute, this);
    }

    @Override
    public <R, C> R accept(ExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitPath(this, context);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PathExpression<?> that = (PathExpression<?>) o;
        return Objects.equals(type, that.type) &&
                Objects.equals(attributeName, that.attributeName) &&
                Objects.equals(parent, that.parent) &&
                Objects.equals(rootType, that.rootType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, attributeName, parent, rootType);
    }

    @Override
    public String toString() {
        if (isRoot()) {
            return "PathExpression[root=" + type.getSimpleName() + "]";
        }
        return "PathExpression[" + getFullPath() + " : " + type.getSimpleName() + "]";
    }
}
