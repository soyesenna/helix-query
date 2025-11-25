package com.soyesenna.helixquery;

import com.soyesenna.helixquery.expression.PredicateExpression;
import com.soyesenna.helixquery.field.ComparableField;
import com.soyesenna.helixquery.field.Field;
import com.soyesenna.helixquery.field.HelixField;
import com.soyesenna.helixquery.field.NumberField;
import com.soyesenna.helixquery.field.StringField;
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
    public void persist(T entity) {
        em.persist(entity);
    }

    /**
     * Merge (update) an entity.
     *
     * @param entity the entity to merge
     * @return the merged entity
     */
    public T merge(T entity) {
        return em.merge(entity);
    }

    /**
     * Remove an entity.
     *
     * @param entity the entity to remove
     */
    public void remove(T entity) {
        em.remove(entity);
    }

    /**
     * Flush pending changes to the database.
     */
    public void flush() {
        em.flush();
    }

    /**
     * Find an entity by ID.
     *
     * @param id the entity ID
     * @return the entity or null if not found
     */
    public T findById(Object id) {
        return em.find(entityClass, id);
    }

    /**
     * Refresh an entity from the database.
     *
     * @param entity the entity to refresh
     */
    public void refresh(T entity) {
        em.refresh(entity);
    }

    /**
     * Detach an entity from the persistence context.
     *
     * @param entity the entity to detach
     */
    public void detach(T entity) {
        em.detach(entity);
    }

    // ==================== Query Entry Points ====================

    /**
     * Start a new query for this entity type.
     *
     * @return a new HelixQuery instance
     */
    public HelixQuery<T> find() {
        return queryFactory.query(entityClass);
    }

    /**
     * Start a query with an equality filter using the unified HelixField interface.
     * This method works with all field types: Field, StringField, NumberField,
     * ComparableField, DateTimeField, RelationField.
     *
     * <pre>{@code
     * // Example usage with different field types:
     * findBy(UserFields.EMAIL, "test@example.com")     // StringField
     * findBy(UserFields.AGE, 25)                       // NumberField
     * findBy(UserFields.STATUS, UserStatus.ACTIVE)    // ComparableField (enum)
     * findBy(UserFields.ID, 1L)                        // NumberField
     * }</pre>
     *
     * @param field the field to filter on (any HelixField type)
     * @param value the value to match
     * @param <V>   the field value type
     * @return a new HelixQuery instance with the filter applied
     */
    public <V> HelixQuery<T> findBy(HelixField<V> field, V value) {
        return find().whereEqual(field, value);
    }

    /**
     * Start a query with a custom predicate.
     *
     * @param predicate the predicate to apply
     * @return a new HelixQuery instance with the predicate applied
     */
    public HelixQuery<T> where(PredicateExpression predicate) {
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
