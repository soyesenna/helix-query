package com.soyesenna.helixquery;

import com.soyesenna.helixquery.expression.PredicateExpression;
import com.soyesenna.helixquery.field.Field;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Abstract base class for services using HelixQuery.
 * Provides common CRUD operations and query entry points.
 * Replaces AbstractQueryService from the QueryDSL-based implementation.
 *
 * @param <T> the entity type
 */
public abstract class AbstractHelixService<T> {

    protected final HelixQueryFactory queryFactory;
    protected final Class<T> entityClass;

    @PersistenceContext
    protected EntityManager em;

    /**
     * Create a new service.
     *
     * @param queryFactory the query factory
     * @param entityClass  the entity class
     */
    protected AbstractHelixService(HelixQueryFactory queryFactory, Class<T> entityClass) {
        this.queryFactory = queryFactory;
        this.entityClass = entityClass;
    }

    // ==================== CRUD Operations ====================

    /**
     * Persist a new entity.
     *
     * @param entity the entity to persist
     */
    protected void persist(T entity) {
        em.persist(entity);
    }

    /**
     * Merge (update) an entity.
     *
     * @param entity the entity to merge
     * @return the merged entity
     */
    protected T merge(T entity) {
        return em.merge(entity);
    }

    /**
     * Remove an entity.
     *
     * @param entity the entity to remove
     */
    protected void remove(T entity) {
        em.remove(entity);
    }

    /**
     * Flush pending changes to the database.
     */
    protected void flush() {
        em.flush();
    }

    /**
     * Find an entity by ID.
     *
     * @param id the entity ID
     * @return the entity or null if not found
     */
    protected T findById(Object id) {
        return em.find(entityClass, id);
    }

    /**
     * Refresh an entity from the database.
     *
     * @param entity the entity to refresh
     */
    protected void refresh(T entity) {
        em.refresh(entity);
    }

    /**
     * Detach an entity from the persistence context.
     *
     * @param entity the entity to detach
     */
    protected void detach(T entity) {
        em.detach(entity);
    }

    // ==================== Query Entry Points ====================

    /**
     * Start a new query for this entity type.
     *
     * @return a new HelixQuery instance
     */
    protected HelixQuery<T> find() {
        return queryFactory.query(entityClass);
    }

    /**
     * Start a query with an equality filter.
     *
     * @param field the field to filter on
     * @param value the value to match
     * @param <V>   the field value type
     * @return a new HelixQuery instance with the filter applied
     */
    protected <V> HelixQuery<T> findBy(Field<V> field, V value) {
        return find().whereEqual(field, value);
    }

    /**
     * Start a query with a custom predicate.
     *
     * @param predicate the predicate to apply
     * @return a new HelixQuery instance with the predicate applied
     */
    protected HelixQuery<T> where(PredicateExpression predicate) {
        return find().where(predicate);
    }

    /**
     * Get the underlying EntityManager.
     * Useful for advanced operations not covered by HelixQuery.
     *
     * @return the entity manager
     */
    protected EntityManager getEntityManager() {
        return em;
    }

    /**
     * Get the query factory.
     *
     * @return the query factory
     */
    protected HelixQueryFactory getQueryFactory() {
        return queryFactory;
    }

    /**
     * Get the entity class.
     *
     * @return the entity class
     */
    protected Class<T> getEntityClass() {
        return entityClass;
    }
}
