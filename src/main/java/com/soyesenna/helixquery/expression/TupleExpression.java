package com.soyesenna.helixquery.expression;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Expression representing a tuple (multi-column) projection.
 * Used to select multiple columns without mapping to a specific DTO.
 */
public class TupleExpression implements Expression<Object[]> {

    private static final long serialVersionUID = 1L;

    private final List<Expression<?>> elements;

    /**
     * Create a tuple expression.
     *
     * @param elements the elements to include in the tuple
     */
    public TupleExpression(Expression<?>... elements) {
        this.elements = Collections.unmodifiableList(Arrays.asList(elements));
    }

    /**
     * Create a tuple expression with a list of elements.
     *
     * @param elements the elements to include in the tuple
     */
    public TupleExpression(List<Expression<?>> elements) {
        this.elements = Collections.unmodifiableList(elements);
    }

    /**
     * Factory method to create a tuple expression.
     *
     * @param elements the elements
     * @return a new tuple expression
     */
    public static TupleExpression of(Expression<?>... elements) {
        return new TupleExpression(elements);
    }

    /**
     * Factory method to create a tuple expression from a list.
     *
     * @param elements the elements
     * @return a new tuple expression
     */
    public static TupleExpression of(List<Expression<?>> elements) {
        return new TupleExpression(elements);
    }

    @Override
    public Class<Object[]> getType() {
        return Object[].class;
    }

    /**
     * Get the tuple elements.
     */
    public List<Expression<?>> getElements() {
        return elements;
    }

    /**
     * Get an element by index.
     */
    public Expression<?> getElement(int index) {
        return elements.get(index);
    }

    /**
     * Get the number of elements.
     */
    public int getSize() {
        return elements.size();
    }

    @Override
    public <R, C> R accept(ExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitTuple(this, context);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TupleExpression that = (TupleExpression) o;
        return Objects.equals(elements, that.elements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elements);
    }

    @Override
    public String toString() {
        return "Tuple[" + elements.size() + " elements]";
    }
}
