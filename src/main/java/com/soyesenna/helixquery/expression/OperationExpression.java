package com.soyesenna.helixquery.expression;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Expression representing an operation with an operator and arguments.
 * Operations include comparisons, arithmetic, string functions, aggregates, etc.
 *
 * @param <T> the result type of the operation
 */
public class OperationExpression<T> implements Expression<T> {

    private static final long serialVersionUID = 1L;

    private final Operator operator;
    private final Class<T> type;
    private final List<Expression<?>> arguments;

    /**
     * Create an operation expression.
     *
     * @param operator the operator
     * @param type     the result type
     * @param args     the arguments
     */
    public OperationExpression(Operator operator, Class<T> type, Expression<?>... args) {
        this.operator = Objects.requireNonNull(operator, "operator must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.arguments = Collections.unmodifiableList(Arrays.asList(args));
    }

    /**
     * Create an operation expression with a list of arguments.
     *
     * @param operator the operator
     * @param type     the result type
     * @param args     the arguments list
     */
    public OperationExpression(Operator operator, Class<T> type, List<Expression<?>> args) {
        this.operator = Objects.requireNonNull(operator, "operator must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.arguments = Collections.unmodifiableList(args);
    }

    @Override
    public Class<? extends T> getType() {
        return type;
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
     *
     * @param index the argument index
     * @return the argument expression
     */
    public Expression<?> getArg(int index) {
        return arguments.get(index);
    }

    /**
     * Get the number of arguments.
     */
    public int getArgCount() {
        return arguments.size();
    }

    @Override
    public <R, C> R accept(ExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitOperation(this, context);
    }

    // ==================== Static Factory Methods ====================

    /**
     * Create a unary operation.
     */
    public static <T> OperationExpression<T> unary(Operator op, Class<T> type, Expression<?> arg) {
        return new OperationExpression<>(op, type, arg);
    }

    /**
     * Create a binary operation.
     */
    public static <T> OperationExpression<T> binary(Operator op, Class<T> type, Expression<?> left, Expression<?> right) {
        return new OperationExpression<>(op, type, left, right);
    }

    /**
     * Create a ternary operation (e.g., BETWEEN).
     */
    public static <T> OperationExpression<T> ternary(Operator op, Class<T> type, Expression<?> arg1, Expression<?> arg2, Expression<?> arg3) {
        return new OperationExpression<>(op, type, arg1, arg2, arg3);
    }

    // ==================== String Operations ====================

    public static OperationExpression<String> upper(Expression<String> expr) {
        return unary(Operator.UPPER, String.class, expr);
    }

    public static OperationExpression<String> lower(Expression<String> expr) {
        return unary(Operator.LOWER, String.class, expr);
    }

    public static OperationExpression<String> trim(Expression<String> expr) {
        return unary(Operator.TRIM, String.class, expr);
    }

    public static OperationExpression<Integer> length(Expression<String> expr) {
        return unary(Operator.LENGTH, Integer.class, expr);
    }

    public static OperationExpression<String> concat(Expression<String> left, Expression<String> right) {
        return binary(Operator.CONCAT, String.class, left, right);
    }

    // ==================== Numeric Operations ====================

    @SuppressWarnings("unchecked")
    public static <N extends Number> OperationExpression<N> add(Expression<N> left, Expression<N> right) {
        return binary(Operator.ADD, (Class<N>) left.getType(), left, right);
    }

    @SuppressWarnings("unchecked")
    public static <N extends Number> OperationExpression<N> subtract(Expression<N> left, Expression<N> right) {
        return binary(Operator.SUBTRACT, (Class<N>) left.getType(), left, right);
    }

    @SuppressWarnings("unchecked")
    public static <N extends Number> OperationExpression<N> multiply(Expression<N> left, Expression<N> right) {
        return binary(Operator.MULTIPLY, (Class<N>) left.getType(), left, right);
    }

    @SuppressWarnings("unchecked")
    public static <N extends Number> OperationExpression<N> divide(Expression<N> left, Expression<N> right) {
        return binary(Operator.DIVIDE, (Class<N>) left.getType(), left, right);
    }

    public static OperationExpression<Integer> mod(Expression<Integer> left, Expression<Integer> right) {
        return binary(Operator.MOD, Integer.class, left, right);
    }

    @SuppressWarnings("unchecked")
    public static <N extends Number> OperationExpression<N> abs(Expression<N> expr) {
        return unary(Operator.ABS, (Class<N>) expr.getType(), expr);
    }

    // ==================== Aggregate Operations ====================

    public static OperationExpression<Long> count(Expression<?> expr) {
        return unary(Operator.COUNT, Long.class, expr);
    }

    public static OperationExpression<Long> countDistinct(Expression<?> expr) {
        return unary(Operator.COUNT_DISTINCT, Long.class, expr);
    }

    @SuppressWarnings("unchecked")
    public static <N extends Number> OperationExpression<N> sum(Expression<N> expr) {
        return unary(Operator.SUM, (Class<N>) expr.getType(), expr);
    }

    public static OperationExpression<Double> avg(Expression<? extends Number> expr) {
        return unary(Operator.AVG, Double.class, expr);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Comparable<? super T>> OperationExpression<T> min(Expression<T> expr) {
        return unary(Operator.MIN, (Class<T>) expr.getType(), expr);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Comparable<? super T>> OperationExpression<T> max(Expression<T> expr) {
        return unary(Operator.MAX, (Class<T>) expr.getType(), expr);
    }

    // ==================== Collection Operations ====================

    public static OperationExpression<Integer> size(Expression<?> collection) {
        return unary(Operator.SIZE, Integer.class, collection);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OperationExpression<?> that = (OperationExpression<?>) o;
        return operator == that.operator &&
                Objects.equals(type, that.type) &&
                Objects.equals(arguments, that.arguments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operator, type, arguments);
    }

    @Override
    public String toString() {
        return "Operation[" + operator + "(" + arguments + ") : " + type.getSimpleName() + "]";
    }
}
