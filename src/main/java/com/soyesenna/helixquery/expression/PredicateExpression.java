package com.soyesenna.helixquery.expression;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Expression representing a boolean predicate (condition).
 * Predicates can be combined using AND, OR, and NOT operations.
 */
public class PredicateExpression implements Expression<Boolean> {

    private static final long serialVersionUID = 1L;

    private final Operator operator;
    private final List<Expression<?>> arguments;

    /**
     * Create a predicate expression.
     *
     * @param operator the operator
     * @param args     the arguments
     */
    public PredicateExpression(Operator operator, Expression<?>... args) {
        this.operator = Objects.requireNonNull(operator, "operator must not be null");
        this.arguments = List.of(args);
    }

    /**
     * Create a predicate expression with a list of arguments.
     *
     * @param operator the operator
     * @param args     the arguments list
     */
    public PredicateExpression(Operator operator, List<Expression<?>> args) {
        this.operator = Objects.requireNonNull(operator, "operator must not be null");
        this.arguments = List.copyOf(args);
    }

    @Override
    public Class<Boolean> getType() {
        return Boolean.class;
    }

    /**
     * Get the operator.
     */
    public Operator getOperator() {
        return operator;
    }

    /**
     * Get the arguments.
     */
    public List<Expression<?>> getArguments() {
        return arguments;
    }

    /**
     * Get an argument by index.
     */
    public Expression<?> getArg(int index) {
        return arguments.get(index);
    }

    // ==================== Logical Composition ====================

    /**
     * Combine this predicate with another using AND.
     *
     * @param other the other predicate
     * @return a new predicate representing (this AND other)
     */
    public PredicateExpression and(PredicateExpression other) {
        if (other == null) return this;
        return new PredicateExpression(Operator.AND, this, other);
    }

    /**
     * Combine this predicate with another using OR.
     *
     * @param other the other predicate
     * @return a new predicate representing (this OR other)
     */
    public PredicateExpression or(PredicateExpression other) {
        if (other == null) return this;
        return new PredicateExpression(Operator.OR, this, other);
    }

    /**
     * Negate this predicate.
     *
     * @return a new predicate representing NOT(this)
     */
    public PredicateExpression not() {
        return new PredicateExpression(Operator.NOT, this);
    }

    // ==================== Static Factory Methods ====================

    /**
     * Create a predicate that always evaluates to TRUE.
     * Useful for building dynamic queries where a condition might be optional.
     *
     * @return a predicate that always evaluates to TRUE
     */
    public static PredicateExpression alwaysTrue() {
        return new PredicateExpression(Operator.TRUE);
    }

    /**
     * Create a predicate that always evaluates to FALSE.
     * Useful for handling empty collection IN queries (empty IN should return no results).
     *
     * @return a predicate that always evaluates to FALSE
     */
    public static PredicateExpression alwaysFalse() {
        return new PredicateExpression(Operator.FALSE);
    }

    /**
     * Combine multiple predicates using AND.
     */
    public static PredicateExpression and(PredicateExpression... predicates) {
        PredicateExpression result = null;
        for (PredicateExpression p : predicates) {
            if (p != null) {
                result = (result == null) ? p : result.and(p);
            }
        }
        return result;
    }

    /**
     * Combine multiple predicates using OR.
     */
    public static PredicateExpression or(PredicateExpression... predicates) {
        PredicateExpression result = null;
        for (PredicateExpression p : predicates) {
            if (p != null) {
                result = (result == null) ? p : result.or(p);
            }
        }
        return result;
    }

    /**
     * Combine a collection of predicates using AND.
     */
    public static PredicateExpression allOf(Collection<PredicateExpression> predicates) {
        PredicateExpression result = null;
        for (PredicateExpression p : predicates) {
            if (p != null) {
                result = (result == null) ? p : result.and(p);
            }
        }
        return result;
    }

    /**
     * Combine a collection of predicates using OR.
     */
    public static PredicateExpression anyOf(Collection<PredicateExpression> predicates) {
        PredicateExpression result = null;
        for (PredicateExpression p : predicates) {
            if (p != null) {
                result = (result == null) ? p : result.or(p);
            }
        }
        return result;
    }

    // ==================== Comparison Predicates ====================

    public static <T> PredicateExpression eq(Expression<T> left, Expression<T> right) {
        return new PredicateExpression(Operator.EQ, left, right);
    }

    public static <T> PredicateExpression eq(Expression<T> left, T value) {
        return new PredicateExpression(Operator.EQ, left, ConstantExpression.of(value));
    }

    public static <T> PredicateExpression ne(Expression<T> left, Expression<T> right) {
        return new PredicateExpression(Operator.NE, left, right);
    }

    public static <T> PredicateExpression ne(Expression<T> left, T value) {
        return new PredicateExpression(Operator.NE, left, ConstantExpression.of(value));
    }

    public static <T extends Comparable<? super T>> PredicateExpression gt(Expression<T> left, Expression<T> right) {
        return new PredicateExpression(Operator.GT, left, right);
    }

    public static <T extends Comparable<? super T>> PredicateExpression gt(Expression<T> left, T value) {
        return new PredicateExpression(Operator.GT, left, ConstantExpression.of(value));
    }

    public static <T extends Comparable<? super T>> PredicateExpression ge(Expression<T> left, Expression<T> right) {
        return new PredicateExpression(Operator.GE, left, right);
    }

    public static <T extends Comparable<? super T>> PredicateExpression ge(Expression<T> left, T value) {
        return new PredicateExpression(Operator.GE, left, ConstantExpression.of(value));
    }

    public static <T extends Comparable<? super T>> PredicateExpression lt(Expression<T> left, Expression<T> right) {
        return new PredicateExpression(Operator.LT, left, right);
    }

    public static <T extends Comparable<? super T>> PredicateExpression lt(Expression<T> left, T value) {
        return new PredicateExpression(Operator.LT, left, ConstantExpression.of(value));
    }

    public static <T extends Comparable<? super T>> PredicateExpression le(Expression<T> left, Expression<T> right) {
        return new PredicateExpression(Operator.LE, left, right);
    }

    public static <T extends Comparable<? super T>> PredicateExpression le(Expression<T> left, T value) {
        return new PredicateExpression(Operator.LE, left, ConstantExpression.of(value));
    }

    public static <T extends Comparable<? super T>> PredicateExpression between(Expression<T> value, Expression<T> from, Expression<T> to) {
        return new PredicateExpression(Operator.BETWEEN, value, from, to);
    }

    public static <T extends Comparable<? super T>> PredicateExpression between(Expression<T> value, T from, T to) {
        return new PredicateExpression(Operator.BETWEEN, value, ConstantExpression.of(from), ConstantExpression.of(to));
    }

    // ==================== Null Check Predicates ====================

    public static PredicateExpression isNull(Expression<?> expr) {
        return new PredicateExpression(Operator.IS_NULL, expr);
    }

    public static PredicateExpression isNotNull(Expression<?> expr) {
        return new PredicateExpression(Operator.IS_NOT_NULL, expr);
    }

    // ==================== Collection Predicates ====================

    public static <T> PredicateExpression in(Expression<T> expr, Collection<? extends T> values) {
        return new PredicateExpression(Operator.IN, expr, ConstantExpression.ofCollection((Collection<T>) values));
    }

    @SafeVarargs
    public static <T> PredicateExpression in(Expression<T> expr, T... values) {
        return in(expr, Arrays.asList(values));
    }

    public static <T> PredicateExpression notIn(Expression<T> expr, Collection<? extends T> values) {
        return new PredicateExpression(Operator.NOT_IN, expr, ConstantExpression.ofCollection((Collection<T>) values));
    }

    public static <E> PredicateExpression memberOf(Expression<E> element, Expression<? extends Collection<E>> collection) {
        return new PredicateExpression(Operator.MEMBER_OF, element, collection);
    }

    public static PredicateExpression isEmpty(Expression<?> collection) {
        return new PredicateExpression(Operator.IS_EMPTY, collection);
    }

    public static PredicateExpression isNotEmpty(Expression<?> collection) {
        return new PredicateExpression(Operator.IS_NOT_EMPTY, collection);
    }

    // ==================== String Predicates ====================

    public static PredicateExpression like(Expression<String> expr, String pattern) {
        return new PredicateExpression(Operator.LIKE, expr, ConstantExpression.of(pattern));
    }

    public static PredicateExpression like(Expression<String> expr, Expression<String> pattern) {
        return new PredicateExpression(Operator.LIKE, expr, pattern);
    }

    public static PredicateExpression contains(Expression<String> expr, String substring) {
        return like(expr, "%" + escapeWildcards(substring) + "%");
    }

    public static PredicateExpression startsWith(Expression<String> expr, String prefix) {
        return like(expr, escapeWildcards(prefix) + "%");
    }

    public static PredicateExpression endsWith(Expression<String> expr, String suffix) {
        return like(expr, "%" + escapeWildcards(suffix));
    }

    /**
     * Escape LIKE wildcards in a string.
     */
    private static String escapeWildcards(String str) {
        if (str == null) return null;
        return str.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    @Override
    public <R, C> R accept(ExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitPredicate(this, context);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PredicateExpression that = (PredicateExpression) o;
        return operator == that.operator && Objects.equals(arguments, that.arguments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operator, arguments);
    }

    @Override
    public String toString() {
        return "Predicate[" + operator + "(" + arguments + ")]";
    }
}
