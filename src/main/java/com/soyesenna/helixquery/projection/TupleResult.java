package com.soyesenna.helixquery.projection;

import com.soyesenna.helixquery.expression.Expression;
import jakarta.persistence.Tuple;

import java.util.List;
import java.util.Objects;

/**
 * Wrapper for JPA Tuple results providing convenient access methods.
 * Simplifies extraction of values from multi-column projections.
 */
public class TupleResult {

    private final Tuple tuple;
    private final List<Expression<?>> expressions;

    /**
     * Create a TupleResult wrapping a JPA Tuple.
     *
     * @param tuple       the JPA Tuple
     * @param expressions the expressions used in the projection
     */
    public TupleResult(Tuple tuple, List<Expression<?>> expressions) {
        this.tuple = Objects.requireNonNull(tuple, "tuple must not be null");
        this.expressions = expressions;
    }

    /**
     * Create a TupleResult wrapping a JPA Tuple without expression mapping.
     *
     * @param tuple the JPA Tuple
     */
    public TupleResult(Tuple tuple) {
        this(tuple, null);
    }

    /**
     * Get the underlying JPA Tuple.
     *
     * @return the tuple
     */
    public Tuple getTuple() {
        return tuple;
    }

    /**
     * Get a value by index.
     *
     * @param index the zero-based index
     * @return the value at the index
     */
    public Object get(int index) {
        return tuple.get(index);
    }

    /**
     * Get a typed value by index.
     *
     * @param index the zero-based index
     * @param type  the expected type
     * @param <T>   the type
     * @return the value cast to the specified type
     */
    public <T> T get(int index, Class<T> type) {
        return tuple.get(index, type);
    }

    /**
     * Get a value by alias.
     *
     * @param alias the alias name
     * @return the value
     */
    public Object get(String alias) {
        return tuple.get(alias);
    }

    /**
     * Get a typed value by alias.
     *
     * @param alias the alias name
     * @param type  the expected type
     * @param <T>   the type
     * @return the value cast to the specified type
     */
    public <T> T get(String alias, Class<T> type) {
        return tuple.get(alias, type);
    }

    /**
     * Get a value by the expression used to select it.
     * Only works if expressions were provided during construction.
     *
     * @param expr the expression
     * @param <T>  the type
     * @return the value
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Expression<T> expr) {
        if (expressions == null) {
            throw new IllegalStateException("Expression mapping not available");
        }
        int index = expressions.indexOf(expr);
        if (index < 0) {
            throw new IllegalArgumentException("Expression not found in result: " + expr);
        }
        return (T) tuple.get(index);
    }

    /**
     * Get all elements as an array.
     *
     * @return the values as an array
     */
    public Object[] toArray() {
        return tuple.toArray();
    }

    /**
     * Get the number of elements in this tuple.
     *
     * @return the size
     */
    public int size() {
        return tuple.getElements().size();
    }

    @Override
    public String toString() {
        return "TupleResult" + java.util.Arrays.toString(toArray());
    }
}
