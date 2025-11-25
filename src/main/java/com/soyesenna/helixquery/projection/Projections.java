package com.soyesenna.helixquery.projection;

import com.soyesenna.helixquery.expression.ConstructorExpression;
import com.soyesenna.helixquery.expression.Expression;
import com.soyesenna.helixquery.expression.TupleExpression;

import java.util.List;

/**
 * Factory class for creating projection expressions.
 * Provides convenient methods for building DTOs and tuple projections.
 */
public final class Projections {

    private Projections() {
        // Utility class
    }

    /**
     * Create a constructor projection for DTO instantiation.
     *
     * @param type the DTO class
     * @param args the constructor arguments
     * @param <T>  the DTO type
     * @return a constructor expression
     */
    public static <T> ConstructorExpression<T> constructor(Class<T> type, Expression<?>... args) {
        return ConstructorExpression.create(type, args);
    }

    /**
     * Create a constructor projection with a list of arguments.
     *
     * @param type the DTO class
     * @param args the constructor arguments
     * @param <T>  the DTO type
     * @return a constructor expression
     */
    public static <T> ConstructorExpression<T> constructor(Class<T> type, List<Expression<?>> args) {
        return ConstructorExpression.create(type, args);
    }

    /**
     * Create a tuple projection for multi-column results.
     *
     * @param expressions the expressions to include
     * @return a tuple expression
     */
    public static TupleExpression tuple(Expression<?>... expressions) {
        return TupleExpression.of(expressions);
    }

    /**
     * Create a tuple projection from a list.
     *
     * @param expressions the expressions to include
     * @return a tuple expression
     */
    public static TupleExpression tuple(List<Expression<?>> expressions) {
        return TupleExpression.of(expressions);
    }

    /**
     * Create a simple bean projection.
     * The target class should have a no-arg constructor
     * and setter methods matching the selected properties.
     *
     * @param type   the bean class
     * @param fields the fields to project
     * @param <T>    the bean type
     * @return a constructor expression (using JPA's construct)
     */
    public static <T> ConstructorExpression<T> bean(Class<T> type, Expression<?>... fields) {
        // Note: This creates a constructor projection.
        // For actual bean projection (setter-based), a custom implementation would be needed.
        return constructor(type, fields);
    }
}
