package com.soyesenna.helixquery;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.dsl.ComparableExpression;
import com.querydsl.core.types.dsl.DateExpression;
import com.querydsl.core.types.dsl.DateTimeExpression;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.core.types.dsl.TimeExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.core.BooleanBuilder;

/**
 * Fluent Querydsl wrapper that keeps a reference to the Q-root.
 */
public class QueryChain<T, Q extends EntityPathBase<T>> {

    private final Q root;
    private final JPAQuery<T> query;
    private final BooleanBuilder predicates = new BooleanBuilder();
    private boolean predicateAttached = false;

    QueryChain(Q root, JPAQuery<T> query) {
        this(root, query, null);
    }

    QueryChain(Q root, JPAQuery<T> query, Predicate initialPredicate) {
        this.root = root;
        this.query = query;
        if (initialPredicate != null) {
            predicates.and(initialPredicate);
            attachPredicateIfNeeded();
        }
    }

    /** Querydsl Q-type root. */
    public Q root() {
        return root;
    }

    /**
     * Exposes the underlying Querydsl query for advanced usage.
     */
    public JPAQuery<T> unwrap() {
        return query;
    }

    // --- where / 조건 조합 ---

    public QueryChain<T, Q> where(Predicate predicate) {
        return and(predicate);
    }

    public QueryChain<T, Q> and(Predicate predicate) {
        if (predicate != null) {
            predicates.and(predicate);
            attachPredicateIfNeeded();
        }
        return this;
    }

    public QueryChain<T, Q> or(Predicate predicate) {
        if (predicate != null) {
            predicates.or(predicate);
            attachPredicateIfNeeded();
        }
        return this;
    }

    // Field 기반 where 조건
    public <V> QueryChain<T, Q> whereEqual(Field<V, Q> field, V value) {
        if (value != null) {
            predicates.and(field.path(root).eq(value));
            attachPredicateIfNeeded();
        }
        return this;
    }

    public <V extends Comparable<?>> QueryChain<T, Q> whereGreaterThan(Field<V, Q> field, V value) {
        if (value != null) {
            Predicate predicate = buildGreaterThan(field, value);
            if (predicate != null) {
                predicates.and(predicate);
                attachPredicateIfNeeded();
            }
        }
        return this;
    }

    public <V extends Comparable<?>> QueryChain<T, Q> whereLessThan(Field<V, Q> field, V value) {
        if (value != null) {
            Predicate predicate = buildLessThan(field, value);
            if (predicate != null) {
                predicates.and(predicate);
                attachPredicateIfNeeded();
            }
        }
        return this;
    }

    public QueryChain<T, Q> whereLike(Field<String, Q> field, String pattern) {
        if (pattern != null) {
            var expression = field.path(root);
            if (expression instanceof StringExpression stringExpression) {
                predicates.and(stringExpression.like(pattern));
                attachPredicateIfNeeded();
            }
        }
        return this;
    }

    public <V> QueryChain<T, Q> whereIn(Field<V, Q> field, List<? extends V> values) {
        if (values != null && !values.isEmpty()) {
            predicates.and(field.path(root).in(values));
            attachPredicateIfNeeded();
        }
        return this;
    }

    // --- order by ---

    public QueryChain<T, Q> orderBy(OrderSpecifier<?>... orders) {
        query.orderBy(orders);
        return this;
    }

    public <V> QueryChain<T, Q> orderByAsc(Field<V, Q> field) {
        query.orderBy(field.asc(root));
        return this;
    }

    public <V> QueryChain<T, Q> orderByDesc(Field<V, Q> field) {
        query.orderBy(field.desc(root));
        return this;
    }

    // --- pagination ---

    public QueryChain<T, Q> limit(long limit) {
        query.limit(limit);
        return this;
    }

    public QueryChain<T, Q> offset(long offset) {
        query.offset(offset);
        return this;
    }

    /**
     * Apply Spring Data Pageable to the query. Sort resolution is delegated to a property->Field mapper.
     */
    public QueryChain<T, Q> pageable(
            org.springframework.data.domain.Pageable pageable,
            Function<String, Field<?, Q>> sortFieldResolver
    ) {
        query.offset(pageable.getOffset());
        query.limit(pageable.getPageSize());

        pageable.getSort().forEach(order -> {
            Field<?, Q> field = sortFieldResolver.apply(order.getProperty());
            if (field != null) {
                if (order.isAscending()) {
                    query.orderBy(field.asc(root));
                } else {
                    query.orderBy(field.desc(root));
                }
            }
        });

        return this;
    }

    // --- join ---

    public <J> QueryChain<T, Q> join(EntityPath<J> joinTarget, Predicate on) {
        query.join(joinTarget).on(on);
        return this;
    }

    public <J> QueryChain<T, Q> leftJoin(EntityPath<J> joinTarget, Predicate on) {
        query.leftJoin(joinTarget).on(on);
        return this;
    }

    public <J> QueryChain<T, Q> fetchJoin(EntityPath<J> joinTarget, Predicate on) {
        query.join(joinTarget).fetchJoin().on(on);
        return this;
    }

    // RelationField 기반 join
    public <J, JQ extends EntityPathBase<J>> QueryChain<T, Q> joinRelation(
            RelationField<J, Q, JQ> relation,
            JQ joinRoot,
            Predicate on
    ) {
        query.join(relation.joinPathGetter().apply(root), joinRoot).on(on);
        return this;
    }

    // --- group by / having ---

    public QueryChain<T, Q> groupBy(Expression<?>... expressions) {
        query.groupBy(expressions);
        return this;
    }

    public QueryChain<T, Q> having(Predicate predicate) {
        if (predicate != null) {
            query.having(predicate);
        }
        return this;
    }

    // --- 동적 적용 유틸 ---

    public QueryChain<T, Q> when(boolean condition, Consumer<QueryChain<T, Q>> customizer) {
        if (condition) {
            customizer.accept(this);
        }
        return this;
    }

    // --- 결과 실행부---

    /** Fetches all results. */
    public List<T> query() {
        return query.fetch();
    }

    /** Fetches a single result or null. */
    public T queryOneOrNull() {
        return query.fetchOne();
    }

    /** Fetches a single result wrapped in Optional. */
    public Optional<T> queryOne() {
        return Optional.ofNullable(query.fetchOne());
    }

    /** Fetches the first result or null. */
    public T queryFirstOrNull() {
        return query.fetchFirst();
    }

    /** Executes count query. */
    public long queryCount() {
        return query.fetchCount();
    }

    /** Existence check using fetchFirst. */
    public boolean exists() {
        return query.fetchFirst() != null;
    }

    /** Projects the query using a custom projection expression builder. */
    public <R> List<R> queryAs(Function<Q, Expression<R>> projectionBuilder) {
        return query.select(projectionBuilder.apply(root)).fetch();
    }

    private void attachPredicateIfNeeded() {
        if (!predicateAttached && predicates.hasValue()) {
            query.where(predicates);
            predicateAttached = true;
        }
    }

    @SuppressWarnings("unchecked")
    private <V extends Comparable<?>> Predicate buildGreaterThan(Field<V, Q> field, V value) {
        var expression = field.path(root);
        if (expression instanceof ComparableExpression<?> comparable) {
            return ((ComparableExpression<V>) comparable).gt(value);
        }
        if (expression instanceof NumberExpression<?> numberExpression && value instanceof Number numberValue) {
            return ((NumberExpression<?>) numberExpression).gt((Number & Comparable<?>) numberValue);
        }
        if (expression instanceof DateExpression<?> dateExpression) {
            return ((DateExpression<V>) dateExpression).after(value);
        }
        if (expression instanceof DateTimeExpression<?> dateTimeExpression) {
            return ((DateTimeExpression<V>) dateTimeExpression).after(value);
        }
        if (expression instanceof TimeExpression<?> timeExpression) {
            return ((TimeExpression<V>) timeExpression).after(value);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <V extends Comparable<?>> Predicate buildLessThan(Field<V, Q> field, V value) {
        var expression = field.path(root);
        if (expression instanceof ComparableExpression<?> comparable) {
            return ((ComparableExpression<V>) comparable).lt(value);
        }
        if (expression instanceof NumberExpression<?> numberExpression && value instanceof Number numberValue) {
            return ((NumberExpression<?>) numberExpression).lt((Number & Comparable<?>) numberValue);
        }
        if (expression instanceof DateExpression<?> dateExpression) {
            return ((DateExpression<V>) dateExpression).before(value);
        }
        if (expression instanceof DateTimeExpression<?> dateTimeExpression) {
            return ((DateTimeExpression<V>) dateTimeExpression).before(value);
        }
        if (expression instanceof TimeExpression<?> timeExpression) {
            return ((TimeExpression<V>) timeExpression).before(value);
        }
        return null;
    }
}
