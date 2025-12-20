package com.soyesenna.helixquery.expression;

import jakarta.persistence.criteria.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Visitor that compiles HelixQuery expressions to JPA Criteria API expressions.
 * This is the core translation layer between the custom DSL and JPA.
 */
public class CriteriaExpressionVisitor implements ExpressionVisitor<jakarta.persistence.criteria.Expression<?>, CriteriaContext> {

    @Override
    public jakarta.persistence.criteria.Expression<?> visitPath(PathExpression<?> path, CriteriaContext ctx) {
        return ctx.resolvePath(path);
    }

    @Override
    public jakarta.persistence.criteria.Expression<?> visitConstant(ConstantExpression<?> constant, CriteriaContext ctx) {
        if (constant.isNull()) {
            return ctx.getCriteriaBuilder().nullLiteral(constant.getType());
        }
        return ctx.getCriteriaBuilder().literal(constant.getValue());
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public jakarta.persistence.criteria.Expression<?> visitOperation(OperationExpression<?> operation, CriteriaContext ctx) {
        CriteriaBuilder cb = ctx.getCriteriaBuilder();
        Operator op = operation.getOperator();
        List<Expression<?>> args = operation.getArguments();

        return switch (op) {
            // String operations
            case UPPER -> cb.upper((jakarta.persistence.criteria.Expression<String>) compile(args.get(0), ctx));
            case LOWER -> cb.lower((jakarta.persistence.criteria.Expression<String>) compile(args.get(0), ctx));
            case TRIM -> cb.trim((jakarta.persistence.criteria.Expression<String>) compile(args.get(0), ctx));
            case LENGTH -> cb.length((jakarta.persistence.criteria.Expression<String>) compile(args.get(0), ctx));
            case CONCAT -> cb.concat(
                    (jakarta.persistence.criteria.Expression<String>) compile(args.get(0), ctx),
                    (jakarta.persistence.criteria.Expression<String>) compile(args.get(1), ctx)
            );
            case SUBSTRING -> {
                if (args.size() == 2) {
                    yield cb.substring(
                            (jakarta.persistence.criteria.Expression<String>) compile(args.get(0), ctx),
                            (jakarta.persistence.criteria.Expression<Integer>) compile(args.get(1), ctx)
                    );
                } else {
                    yield cb.substring(
                            (jakarta.persistence.criteria.Expression<String>) compile(args.get(0), ctx),
                            (jakarta.persistence.criteria.Expression<Integer>) compile(args.get(1), ctx),
                            (jakarta.persistence.criteria.Expression<Integer>) compile(args.get(2), ctx)
                    );
                }
            }
            case LOCATE -> cb.locate(
                    (jakarta.persistence.criteria.Expression<String>) compile(args.get(0), ctx),
                    (jakarta.persistence.criteria.Expression<String>) compile(args.get(1), ctx)
            );

            // Numeric operations
            case ADD -> cb.sum(
                    (jakarta.persistence.criteria.Expression<Number>) compile(args.get(0), ctx),
                    (jakarta.persistence.criteria.Expression<Number>) compile(args.get(1), ctx)
            );
            case SUBTRACT -> cb.diff(
                    (jakarta.persistence.criteria.Expression<Number>) compile(args.get(0), ctx),
                    (jakarta.persistence.criteria.Expression<Number>) compile(args.get(1), ctx)
            );
            case MULTIPLY -> cb.prod(
                    (jakarta.persistence.criteria.Expression<Number>) compile(args.get(0), ctx),
                    (jakarta.persistence.criteria.Expression<Number>) compile(args.get(1), ctx)
            );
            case DIVIDE -> cb.quot(
                    (jakarta.persistence.criteria.Expression<Number>) compile(args.get(0), ctx),
                    (jakarta.persistence.criteria.Expression<Number>) compile(args.get(1), ctx)
            );
            case MOD -> cb.mod(
                    (jakarta.persistence.criteria.Expression<Integer>) compile(args.get(0), ctx),
                    (jakarta.persistence.criteria.Expression<Integer>) compile(args.get(1), ctx)
            );
            case ABS -> cb.abs((jakarta.persistence.criteria.Expression<Number>) compile(args.get(0), ctx));
            case NEGATE -> cb.neg((jakarta.persistence.criteria.Expression<Number>) compile(args.get(0), ctx));
            case SQRT -> cb.sqrt((jakarta.persistence.criteria.Expression<Number>) compile(args.get(0), ctx));

            // Aggregate operations
            case COUNT -> cb.count(compile(args.get(0), ctx));
            case COUNT_DISTINCT -> cb.countDistinct(compile(args.get(0), ctx));
            case SUM -> cb.sum((jakarta.persistence.criteria.Expression<Number>) compile(args.get(0), ctx));
            case AVG -> cb.avg((jakarta.persistence.criteria.Expression<Number>) compile(args.get(0), ctx));
            case MIN -> cb.min((jakarta.persistence.criteria.Expression<Number>) compile(args.get(0), ctx));
            case MAX -> cb.max((jakarta.persistence.criteria.Expression<Number>) compile(args.get(0), ctx));

            // Collection operations
            case SIZE -> cb.size((jakarta.persistence.criteria.Expression<Collection>) compile(args.get(0), ctx));

            // Date/Time operations
            case CURRENT_DATE -> cb.currentDate();
            case CURRENT_TIME -> cb.currentTime();
            case CURRENT_TIMESTAMP -> cb.currentTimestamp();

            // Type operations
            case COALESCE -> {
                CriteriaBuilder.Coalesce coalesce = cb.coalesce();
                for (Expression<?> arg : args) {
                    coalesce.value(compile(arg, ctx));
                }
                yield coalesce;
            }
            case NULLIF -> cb.nullif(compile(args.get(0), ctx), compile(args.get(1), ctx));

            default -> throw new UnsupportedOperationException("Operation not supported: " + op);
        };
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public jakarta.persistence.criteria.Expression<?> visitPredicate(PredicateExpression predicate, CriteriaContext ctx) {
        return compilePredicate(predicate, ctx);
    }

    @Override
    public jakarta.persistence.criteria.Expression<?> visitConstructor(ConstructorExpression<?> constructor, CriteriaContext ctx) {
        // Constructor expressions are handled specially in HelixQuery.queryAs()
        // This visitor method returns null as CompoundSelection cannot be cast to Expression
        throw new UnsupportedOperationException(
                "Constructor expressions should be handled directly in HelixQuery.queryAs(), not through visitor");
    }

    @Override
    public jakarta.persistence.criteria.Expression<?> visitTuple(TupleExpression tuple, CriteriaContext ctx) {
        // Tuple expressions are handled specially in HelixQuery.queryTuple()
        // This visitor method returns null as CompoundSelection cannot be cast to Expression
        throw new UnsupportedOperationException(
                "Tuple expressions should be handled directly in HelixQuery.queryTuple(), not through visitor");
    }

    // ==================== Public Compilation Methods ====================

    /**
     * Compile a HelixQuery expression to a JPA Criteria expression.
     *
     * @param expr the HelixQuery expression
     * @param ctx  the criteria context
     * @return the JPA Criteria expression
     */
    public jakarta.persistence.criteria.Expression<?> compile(Expression<?> expr, CriteriaContext ctx) {
        return expr.accept(this, ctx);
    }

    /**
     * Compile a predicate expression to a JPA Predicate.
     *
     * @param predicate the predicate expression
     * @param ctx       the criteria context
     * @return the JPA Predicate
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Predicate compilePredicate(PredicateExpression predicate, CriteriaContext ctx) {
        CriteriaBuilder cb = ctx.getCriteriaBuilder();
        Operator op = predicate.getOperator();
        List<Expression<?>> args = predicate.getArguments();

        return switch (op) {
            // Comparison operations
            case EQ -> cb.equal(compile(args.get(0), ctx), compile(args.get(1), ctx));
            case NE -> cb.notEqual(compile(args.get(0), ctx), compile(args.get(1), ctx));
            case GT -> cb.greaterThan(
                    (jakarta.persistence.criteria.Expression<Comparable>) compile(args.get(0), ctx),
                    (jakarta.persistence.criteria.Expression<Comparable>) compile(args.get(1), ctx)
            );
            case GE -> cb.greaterThanOrEqualTo(
                    (jakarta.persistence.criteria.Expression<Comparable>) compile(args.get(0), ctx),
                    (jakarta.persistence.criteria.Expression<Comparable>) compile(args.get(1), ctx)
            );
            case LT -> cb.lessThan(
                    (jakarta.persistence.criteria.Expression<Comparable>) compile(args.get(0), ctx),
                    (jakarta.persistence.criteria.Expression<Comparable>) compile(args.get(1), ctx)
            );
            case LE -> cb.lessThanOrEqualTo(
                    (jakarta.persistence.criteria.Expression<Comparable>) compile(args.get(0), ctx),
                    (jakarta.persistence.criteria.Expression<Comparable>) compile(args.get(1), ctx)
            );
            case BETWEEN -> cb.between(
                    (jakarta.persistence.criteria.Expression<Comparable>) compile(args.get(0), ctx),
                    (jakarta.persistence.criteria.Expression<Comparable>) compile(args.get(1), ctx),
                    (jakarta.persistence.criteria.Expression<Comparable>) compile(args.get(2), ctx)
            );

            // Null checks
            case IS_NULL -> cb.isNull(compile(args.get(0), ctx));
            case IS_NOT_NULL -> cb.isNotNull(compile(args.get(0), ctx));

            // Boolean operations
            case AND -> {
                Predicate left = compilePredicate((PredicateExpression) args.get(0), ctx);
                Predicate right = compilePredicate((PredicateExpression) args.get(1), ctx);
                yield cb.and(left, right);
            }
            case OR -> {
                Predicate left = compilePredicate((PredicateExpression) args.get(0), ctx);
                Predicate right = compilePredicate((PredicateExpression) args.get(1), ctx);
                yield cb.or(left, right);
            }
            case NOT -> cb.not(compilePredicate((PredicateExpression) args.get(0), ctx));
            case TRUE -> cb.conjunction();  // Always true predicate (1=1)
            case FALSE -> cb.disjunction(); // Always false predicate (1=0)

            // Collection operations
            case IN -> {
                jakarta.persistence.criteria.Expression<?> expr = compile(args.get(0), ctx);
                ConstantExpression<?> valuesExpr = (ConstantExpression<?>) args.get(1);
                Collection<?> values = (Collection<?>) valuesExpr.getValue();
                CriteriaBuilder.In inClause = cb.in(expr);
                for (Object value : values) {
                    inClause.value(value);
                }
                yield inClause;
            }
            case NOT_IN -> {
                jakarta.persistence.criteria.Expression<?> expr = compile(args.get(0), ctx);
                ConstantExpression<?> valuesExpr = (ConstantExpression<?>) args.get(1);
                Collection<?> values = (Collection<?>) valuesExpr.getValue();
                CriteriaBuilder.In inClause = cb.in(expr);
                for (Object value : values) {
                    inClause.value(value);
                }
                yield cb.not(inClause);
            }
            case MEMBER_OF -> {
                Object elementValue = ((ConstantExpression<?>) args.get(0)).getValue();
                yield cb.isMember(
                        elementValue,
                        (jakarta.persistence.criteria.Expression<Collection<Object>>) compile(args.get(1), ctx)
                );
            }
            case IS_EMPTY -> cb.isEmpty((jakarta.persistence.criteria.Expression<Collection>) compile(args.get(0), ctx));
            case IS_NOT_EMPTY -> cb.isNotEmpty((jakarta.persistence.criteria.Expression<Collection>) compile(args.get(0), ctx));

            // String operations
            case LIKE -> cb.like(
                    (jakarta.persistence.criteria.Expression<String>) compile(args.get(0), ctx),
                    (jakarta.persistence.criteria.Expression<String>) compile(args.get(1), ctx)
            );
            case LIKE_ESCAPE -> cb.like(
                    (jakarta.persistence.criteria.Expression<String>) compile(args.get(0), ctx),
                    (jakarta.persistence.criteria.Expression<String>) compile(args.get(1), ctx),
                    (char) ((ConstantExpression<?>) args.get(2)).getValue()
            );

            default -> throw new UnsupportedOperationException("Predicate operator not supported: " + op);
        };
    }

    /**
     * Compile multiple predicate expressions to a JPA Predicate array.
     *
     * @param predicates the predicate expressions
     * @param ctx        the criteria context
     * @return the JPA Predicates
     */
    public Predicate[] compilePredicates(List<PredicateExpression> predicates, CriteriaContext ctx) {
        Predicate[] result = new Predicate[predicates.size()];
        for (int i = 0; i < predicates.size(); i++) {
            result[i] = compilePredicate(predicates.get(i), ctx);
        }
        return result;
    }
}
