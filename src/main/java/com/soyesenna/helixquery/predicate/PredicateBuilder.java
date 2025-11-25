package com.soyesenna.helixquery.predicate;

import com.soyesenna.helixquery.expression.PredicateExpression;

import java.util.function.Supplier;

/**
 * Mutable builder for constructing predicate expressions.
 * Replaces QueryDSL's BooleanBuilder with similar functionality.
 * Useful for building dynamic queries where conditions are added conditionally.
 */
public class PredicateBuilder {

    private PredicateExpression predicate;

    /**
     * Create an empty predicate builder.
     */
    public PredicateBuilder() {
        this.predicate = null;
    }

    /**
     * Create a predicate builder with an initial predicate.
     *
     * @param initial the initial predicate
     */
    public PredicateBuilder(PredicateExpression initial) {
        this.predicate = initial;
    }

    // ==================== AND Operations ====================

    /**
     * Add a predicate with AND.
     * If current predicate is null, sets the predicate.
     * If the given predicate is null, does nothing.
     *
     * @param expr the predicate to AND with
     * @return this builder for chaining
     */
    public PredicateBuilder and(PredicateExpression expr) {
        if (expr != null) {
            predicate = (predicate == null) ? expr : predicate.and(expr);
        }
        return this;
    }

    /**
     * Conditionally add a predicate with AND.
     *
     * @param condition if true, add the predicate
     * @param expr      the predicate to AND with
     * @return this builder for chaining
     */
    public PredicateBuilder andIf(boolean condition, PredicateExpression expr) {
        if (condition) {
            and(expr);
        }
        return this;
    }

    /**
     * Conditionally add a predicate with AND (lazy evaluation).
     *
     * @param condition if true, evaluate and add the predicate
     * @param supplier  supplier of the predicate
     * @return this builder for chaining
     */
    public PredicateBuilder andIf(boolean condition, Supplier<PredicateExpression> supplier) {
        if (condition) {
            and(supplier.get());
        }
        return this;
    }

    /**
     * Add a predicate with AND if the value is not null.
     *
     * @param value    the value to check
     * @param supplier supplier of the predicate
     * @return this builder for chaining
     */
    public <T> PredicateBuilder andIfNotNull(T value, Supplier<PredicateExpression> supplier) {
        if (value != null) {
            and(supplier.get());
        }
        return this;
    }

    /**
     * Add a predicate with AND if the string is not null or empty.
     *
     * @param value    the string to check
     * @param supplier supplier of the predicate
     * @return this builder for chaining
     */
    public PredicateBuilder andIfNotEmpty(String value, Supplier<PredicateExpression> supplier) {
        if (value != null && !value.isEmpty()) {
            and(supplier.get());
        }
        return this;
    }

    // ==================== OR Operations ====================

    /**
     * Add a predicate with OR.
     * If current predicate is null, sets the predicate.
     * If the given predicate is null, does nothing.
     *
     * @param expr the predicate to OR with
     * @return this builder for chaining
     */
    public PredicateBuilder or(PredicateExpression expr) {
        if (expr != null) {
            predicate = (predicate == null) ? expr : predicate.or(expr);
        }
        return this;
    }

    /**
     * Conditionally add a predicate with OR.
     *
     * @param condition if true, add the predicate
     * @param expr      the predicate to OR with
     * @return this builder for chaining
     */
    public PredicateBuilder orIf(boolean condition, PredicateExpression expr) {
        if (condition) {
            or(expr);
        }
        return this;
    }

    /**
     * Conditionally add a predicate with OR (lazy evaluation).
     *
     * @param condition if true, evaluate and add the predicate
     * @param supplier  supplier of the predicate
     * @return this builder for chaining
     */
    public PredicateBuilder orIf(boolean condition, Supplier<PredicateExpression> supplier) {
        if (condition) {
            or(supplier.get());
        }
        return this;
    }

    // ==================== NOT Operation ====================

    /**
     * Negate the current predicate.
     *
     * @return this builder for chaining
     */
    public PredicateBuilder not() {
        if (predicate != null) {
            predicate = predicate.not();
        }
        return this;
    }

    // ==================== Nested Builders ====================

    /**
     * Start a nested AND group.
     * Example: builder.and(nested -> nested.or(a).or(b))
     * Results in: currentPredicate AND (a OR b)
     *
     * @param builderConsumer consumer that builds the nested predicate
     * @return this builder for chaining
     */
    public PredicateBuilder andGroup(java.util.function.Consumer<PredicateBuilder> builderConsumer) {
        PredicateBuilder nested = new PredicateBuilder();
        builderConsumer.accept(nested);
        if (nested.hasValue()) {
            and(nested.build());
        }
        return this;
    }

    /**
     * Start a nested OR group.
     * Example: builder.or(nested -> nested.and(a).and(b))
     * Results in: currentPredicate OR (a AND b)
     *
     * @param builderConsumer consumer that builds the nested predicate
     * @return this builder for chaining
     */
    public PredicateBuilder orGroup(java.util.function.Consumer<PredicateBuilder> builderConsumer) {
        PredicateBuilder nested = new PredicateBuilder();
        builderConsumer.accept(nested);
        if (nested.hasValue()) {
            or(nested.build());
        }
        return this;
    }

    // ==================== State & Build ====================

    /**
     * Check if this builder has a predicate.
     *
     * @return true if a predicate has been set
     */
    public boolean hasValue() {
        return predicate != null;
    }

    /**
     * Get the built predicate.
     *
     * @return the predicate, or null if no conditions were added
     */
    public PredicateExpression build() {
        return predicate;
    }

    /**
     * Get the built predicate, throwing if null.
     *
     * @return the predicate
     * @throws IllegalStateException if no predicate was built
     */
    public PredicateExpression buildRequired() {
        if (predicate == null) {
            throw new IllegalStateException("No predicate conditions were added");
        }
        return predicate;
    }

    /**
     * Reset this builder to empty state.
     *
     * @return this builder for chaining
     */
    public PredicateBuilder reset() {
        predicate = null;
        return this;
    }

    // ==================== Static Factory Methods ====================

    /**
     * Create a builder starting with an AND of multiple predicates.
     */
    public static PredicateBuilder allOf(PredicateExpression... predicates) {
        PredicateBuilder builder = new PredicateBuilder();
        for (PredicateExpression p : predicates) {
            builder.and(p);
        }
        return builder;
    }

    /**
     * Create a builder starting with an OR of multiple predicates.
     */
    public static PredicateBuilder anyOf(PredicateExpression... predicates) {
        PredicateBuilder builder = new PredicateBuilder();
        for (PredicateExpression p : predicates) {
            builder.or(p);
        }
        return builder;
    }

    @Override
    public String toString() {
        return "PredicateBuilder[" + (predicate != null ? predicate : "empty") + "]";
    }
}
