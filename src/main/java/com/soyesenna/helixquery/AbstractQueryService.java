package com.soyesenna.helixquery;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.jpa.impl.JPAQueryFactory;

/**
 * Base service that wires Spring-managed EntityManager &amp; Querydsl factory while exposing query helpers.
 */
public abstract class AbstractQueryService<T, Q extends EntityPathBase<T>> {

    protected final JPAQueryFactory queryFactory;
    protected final Q q;

    /**
     * EntityManager managed by Spring's persistence context.
     */
    @PersistenceContext
    protected EntityManager em;

    protected AbstractQueryService(JPAQueryFactory queryFactory, Q q) {
        this.queryFactory = queryFactory;
        this.q = q;
    }

    // --- 기본 JPA CRUD 유틸 ---

    /**
     * Persist the given entity in the current persistence context.
     */
    public T persist(T entity) {
        em.persist(entity);
        return entity;
    }

    public T merge(T entity) {
        return em.merge(entity);
    }

    public void remove(T entity) {
        em.remove(entity);
    }

    public void flush() {
        em.flush();
    }

    // --- QueryChain 시작점 ---

    /**
     * Starts a QueryChain with selectFrom(q).
     */
    public QueryChain<T, Q> find() {
        return new QueryChain<>(q, queryFactory.selectFrom(q));
    }

    /**
     * Starts QueryChain with equality filter.
     */
    public <V> QueryChain<T, Q> findBy(Field<V, Q> field, V value) {
        return new QueryChain<>(q, queryFactory.selectFrom(q), field.path(q).eq(value));
    }

    /**
     * Starts QueryChain from custom predicate.
     */
    public QueryChain<T, Q> where(com.querydsl.core.types.dsl.BooleanExpression predicate) {
        return new QueryChain<>(q, queryFactory.selectFrom(q), predicate);
    }
}
