package com.soyesenna.helixquery.field;

import com.soyesenna.helixquery.expression.OperationExpression;
import com.soyesenna.helixquery.expression.PathExpression;
import com.soyesenna.helixquery.expression.PredicateExpression;
import com.soyesenna.helixquery.order.OrderDirection;
import com.soyesenna.helixquery.order.OrderSpecifier;

import java.util.Collection;
import java.util.Objects;

/**
 * Field for String type with string-specific operations.
 */
public record StringField(
        String name,
        Class<?> entityType,
        String relationPath
) implements HelixField<String> {

    /**
     * Canonical constructor with all parameters.
     */
    public StringField {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(entityType, "entityType must not be null");
        // relationPath can be null
    }

    /**
     * Constructor without relationPath (for regular fields).
     */
    public StringField(String name, Class<?> entityType) {
        this(name, entityType, null);
    }

    @Override
    public String relationPath() {
        return relationPath;
    }

    // ==================== Path Access ====================

    public PathExpression<String> path(PathExpression<?> root) {
        return new PathExpression<>(String.class, name, root, relationPath);
    }

    // ==================== Equality Predicates ====================

    public PredicateExpression eq(PathExpression<?> root, String value) {
        if (value == null) {
            return isNull(root);
        }
        return PredicateExpression.eq(path(root), value);
    }

    public PredicateExpression ne(PathExpression<?> root, String value) {
        if (value == null) {
            return isNotNull(root);
        }
        return PredicateExpression.ne(path(root), value);
    }

    /**
     * Case-insensitive equality using UPPER()
     */
    public PredicateExpression eqIgnoreCase(PathExpression<?> root, String value) {
        if (value == null) {
            return isNull(root);
        }
        return PredicateExpression.eq(upper(root), OperationExpression.upper(
                new com.soyesenna.helixquery.expression.ConstantExpression<>(value.toUpperCase())));
    }

    // ==================== String Pattern Predicates ====================

    /**
     * LIKE pattern matching: field LIKE pattern
     */
    public PredicateExpression like(PathExpression<?> root, String pattern) {
        Objects.requireNonNull(pattern, "pattern must not be null");
        return PredicateExpression.like(path(root), pattern);
    }

    /**
     * Contains substring: field LIKE '%value%'
     */
    public PredicateExpression contains(PathExpression<?> root, String substring) {
        Objects.requireNonNull(substring, "substring must not be null");
        return PredicateExpression.contains(path(root), substring);
    }

    /**
     * Starts with prefix: field LIKE 'value%'
     */
    public PredicateExpression startsWith(PathExpression<?> root, String prefix) {
        Objects.requireNonNull(prefix, "prefix must not be null");
        return PredicateExpression.startsWith(path(root), prefix);
    }

    /**
     * Ends with suffix: field LIKE '%value'
     */
    public PredicateExpression endsWith(PathExpression<?> root, String suffix) {
        Objects.requireNonNull(suffix, "suffix must not be null");
        return PredicateExpression.endsWith(path(root), suffix);
    }

    // ==================== Collection Predicates ====================

    public PredicateExpression in(PathExpression<?> root, Collection<? extends String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return PredicateExpression.in(path(root), values);
    }

    public PredicateExpression in(PathExpression<?> root, String... values) {
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

    /**
     * Check for empty string: field = '' OR field IS NULL
     */
    public PredicateExpression isEmpty(PathExpression<?> root) {
        return PredicateExpression.or(
                PredicateExpression.eq(path(root), ""),
                PredicateExpression.isNull(path(root))
        );
    }

    /**
     * Check for non-empty string: field != '' AND field IS NOT NULL
     */
    public PredicateExpression isNotEmpty(PathExpression<?> root) {
        return PredicateExpression.and(
                PredicateExpression.ne(path(root), ""),
                PredicateExpression.isNotNull(path(root))
        );
    }

    // ==================== String Operations ====================

    /**
     * UPPER(field)
     */
    public OperationExpression<String> upper(PathExpression<?> root) {
        return OperationExpression.upper(path(root));
    }

    /**
     * LOWER(field)
     */
    public OperationExpression<String> lower(PathExpression<?> root) {
        return OperationExpression.lower(path(root));
    }

    /**
     * TRIM(field)
     */
    public OperationExpression<String> trim(PathExpression<?> root) {
        return OperationExpression.trim(path(root));
    }

    /**
     * LENGTH(field)
     */
    public OperationExpression<Integer> length(PathExpression<?> root) {
        return OperationExpression.length(path(root));
    }

    // ==================== Ordering ====================

    public OrderSpecifier asc(PathExpression<?> root) {
        return new OrderSpecifier(path(root), OrderDirection.ASC);
    }

    public OrderSpecifier desc(PathExpression<?> root) {
        return new OrderSpecifier(path(root), OrderDirection.DESC);
    }
}
