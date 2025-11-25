package com.soyesenna.helixquery.expression;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Expression representing a constructor invocation for DTO projections.
 * Used to project query results into custom DTO classes.
 *
 * @param <T> the DTO type to construct
 */
public class ConstructorExpression<T> implements Expression<T> {

    private static final long serialVersionUID = 1L;

    private final Class<T> type;
    private final List<Expression<?>> arguments;

    /**
     * Create a constructor expression.
     *
     * @param type the DTO class to construct
     * @param args the constructor arguments
     */
    public ConstructorExpression(Class<T> type, Expression<?>... args) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.arguments = Collections.unmodifiableList(Arrays.asList(args));
    }

    /**
     * Create a constructor expression with a list of arguments.
     *
     * @param type the DTO class to construct
     * @param args the constructor arguments
     */
    public ConstructorExpression(Class<T> type, List<Expression<?>> args) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.arguments = Collections.unmodifiableList(args);
    }

    /**
     * Factory method to create a constructor expression.
     *
     * @param type the DTO class
     * @param args the constructor arguments
     * @param <T>  the DTO type
     * @return a new constructor expression
     */
    public static <T> ConstructorExpression<T> create(Class<T> type, Expression<?>... args) {
        return new ConstructorExpression<>(type, args);
    }

    /**
     * Factory method to create a constructor expression with a list of arguments.
     *
     * @param type the DTO class
     * @param args the constructor arguments
     * @param <T>  the DTO type
     * @return a new constructor expression
     */
    public static <T> ConstructorExpression<T> create(Class<T> type, List<Expression<?>> args) {
        return new ConstructorExpression<>(type, args);
    }

    @Override
    public Class<? extends T> getType() {
        return type;
    }

    /**
     * Get the constructor arguments.
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

    /**
     * Get the number of arguments.
     */
    public int getArgCount() {
        return arguments.size();
    }

    @Override
    public <R, C> R accept(ExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitConstructor(this, context);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConstructorExpression<?> that = (ConstructorExpression<?>) o;
        return Objects.equals(type, that.type) && Objects.equals(arguments, that.arguments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, arguments);
    }

    @Override
    public String toString() {
        return "Constructor[" + type.getSimpleName() + "(" + arguments.size() + " args)]";
    }
}
