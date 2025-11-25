package com.soyesenna.helixquery.expression;

import java.util.Collection;
import java.util.Objects;

/**
 * Expression representing a constant (literal) value.
 *
 * @param <T> the type of the constant
 */
public class ConstantExpression<T> implements Expression<T> {

    private static final long serialVersionUID = 1L;

    private final T value;
    private final Class<T> type;

    /**
     * Create a constant expression with explicit type.
     *
     * @param value the constant value
     * @param type  the type class
     */
    public ConstantExpression(T value, Class<T> type) {
        this.value = value;
        this.type = Objects.requireNonNull(type, "type must not be null");
    }

    /**
     * Create a constant expression, inferring type from value.
     *
     * @param value the constant value (must not be null)
     */
    @SuppressWarnings("unchecked")
    public ConstantExpression(T value) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null; use ConstantExpression(value, type) for nullable values");
        }
        this.value = value;
        this.type = (Class<T>) value.getClass();
    }

    /**
     * Factory method to create a constant expression.
     *
     * @param value the constant value
     * @param <T>   the type
     * @return a new constant expression
     */
    public static <T> ConstantExpression<T> of(T value) {
        return new ConstantExpression<>(value);
    }

    /**
     * Factory method to create a constant expression with explicit type.
     *
     * @param value the constant value
     * @param type  the type class
     * @param <T>   the type
     * @return a new constant expression
     */
    public static <T> ConstantExpression<T> of(T value, Class<T> type) {
        return new ConstantExpression<>(value, type);
    }

    /**
     * Factory method for collection constants.
     *
     * @param values the collection values
     * @param <T>    the element type
     * @return a new constant expression wrapping the collection
     */
    @SuppressWarnings("unchecked")
    public static <T> ConstantExpression<Collection<T>> ofCollection(Collection<T> values) {
        return new ConstantExpression<>(values, (Class<Collection<T>>) (Class<?>) Collection.class);
    }

    /**
     * Factory method for null constant with explicit type.
     *
     * @param type the type class
     * @param <T>  the type
     * @return a new constant expression with null value
     */
    public static <T> ConstantExpression<T> ofNull(Class<T> type) {
        return new ConstantExpression<>(null, type);
    }

    @Override
    public Class<? extends T> getType() {
        return type;
    }

    /**
     * Get the constant value.
     */
    public T getValue() {
        return value;
    }

    /**
     * Check if this constant is null.
     */
    public boolean isNull() {
        return value == null;
    }

    @Override
    public <R, C> R accept(ExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitConstant(this, context);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConstantExpression<?> that = (ConstantExpression<?>) o;
        return Objects.equals(value, that.value) && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, type);
    }

    @Override
    public String toString() {
        return "Constant[" + value + " : " + type.getSimpleName() + "]";
    }
}
