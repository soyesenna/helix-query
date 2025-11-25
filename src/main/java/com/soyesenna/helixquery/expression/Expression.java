package com.soyesenna.helixquery.expression;

import java.io.Serializable;

/**
 * Base interface for all expressions in the HelixQuery DSL.
 * Expressions form a tree structure that can be compiled to JPA Criteria API.
 *
 * @param <T> the Java type this expression evaluates to
 */
public interface Expression<T> extends Serializable {

    /**
     * Returns the Java type of this expression.
     *
     * @return the type class
     */
    Class<? extends T> getType();

    /**
     * Accept a visitor for expression traversal and compilation.
     *
     * @param visitor the expression visitor
     * @param context the compilation context
     * @param <R>     the return type of the visitor
     * @param <C>     the context type
     * @return the result of visiting this expression
     */
    <R, C> R accept(ExpressionVisitor<R, C> visitor, C context);
}
