package com.soyesenna.helixquery.expression;

/**
 * Visitor interface for traversing and compiling expression trees.
 * Implements the visitor pattern for type-safe expression processing.
 *
 * @param <R> the return type of visit methods
 * @param <C> the context type passed to visit methods
 */
public interface ExpressionVisitor<R, C> {

    /**
     * Visit a path expression (entity attribute reference).
     */
    R visitPath(PathExpression<?> path, C context);

    /**
     * Visit a constant expression (literal value).
     */
    R visitConstant(ConstantExpression<?> constant, C context);

    /**
     * Visit an operation expression (operator with arguments).
     */
    R visitOperation(OperationExpression<?> operation, C context);

    /**
     * Visit a predicate expression (boolean condition).
     */
    R visitPredicate(PredicateExpression predicate, C context);

    /**
     * Visit a constructor expression (DTO projection).
     */
    R visitConstructor(ConstructorExpression<?> constructor, C context);

    /**
     * Visit a tuple expression (multi-column projection).
     */
    R visitTuple(TupleExpression tuple, C context);
}
