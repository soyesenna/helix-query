package com.soyesenna.helixquery;

import com.soyesenna.helixquery.expression.PredicateExpression;
import com.soyesenna.helixquery.field.ComparableField;
import com.soyesenna.helixquery.field.Field;
import com.soyesenna.helixquery.field.HelixField;
import com.soyesenna.helixquery.field.NumberField;
import com.soyesenna.helixquery.field.StringField;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;

/**
 * Abstract base class for services using HelixQuery.
 * Provides common CRUD operations and query entry points.
 * Replaces AbstractQueryService from the QueryDSL-based implementation.
 *
 * <p>This class supports two usage patterns:</p>
 *
 * <h3>1. Zero-boilerplate (recommended)</h3>
 * <pre>{@code
 * @Service
 * public class UserService extends AbstractHelixService<User> {
 *     // No constructor needed! Spring auto-wires everything.
 * }
 * }</pre>
 *
 * <h3>2. Explicit constructor (for testing or special cases)</h3>
 * <pre>{@code
 * @Service
 * public class UserService extends AbstractHelixService<User> {
 *     public UserService(HelixQueryFactory queryFactory) {
 *         super(queryFactory, User.class);
 *     }
 * }
 * }</pre>
 *
 * @param <T> the entity type
 */
public abstract class AbstractHelixService<T> {

    @Autowired
    protected HelixQueryFactory queryFactory;

    protected Class<T> entityClass;

    @PersistenceContext
    protected EntityManager em;

    /**
     * Default constructor for Spring dependency injection.
     * The entityClass is automatically resolved from the generic type parameter.
     *
     * <pre>{@code
     * @Service
     * public class UserService extends AbstractHelixService<User> {
     *     // No constructor needed!
     * }
     * }</pre>
     */
    protected AbstractHelixService() {
        // entityClass will be resolved in @PostConstruct
    }

    /**
     * Create a new service with explicit parameters.
     * Useful for testing or when automatic type resolution is not possible.
     *
     * @param queryFactory the query factory
     * @param entityClass  the entity class
     */
    protected AbstractHelixService(HelixQueryFactory queryFactory, Class<T> entityClass) {
        this.queryFactory = queryFactory;
        this.entityClass = entityClass;
    }

    /**
     * Initialize the entity class from the generic type parameter.
     * Called automatically by Spring after dependency injection.
     */
    @PostConstruct
    private void initEntityClass() {
        if (this.entityClass == null) {
            this.entityClass = resolveEntityClass();
        }
    }

    /**
     * Resolve the entity class from the generic type parameter using reflection.
     * Handles direct inheritance and intermediate class hierarchies.
     *
     * @return the resolved entity class
     * @throws IllegalStateException if the entity class cannot be resolved
     */
    @SuppressWarnings("unchecked")
    private Class<T> resolveEntityClass() {
        Class<?> clazz = getClass();

        while (clazz != null) {
            Type genericSuperclass = clazz.getGenericSuperclass();

            if (genericSuperclass instanceof ParameterizedType) {
                ParameterizedType paramType = (ParameterizedType) genericSuperclass;

                // Check if this is AbstractHelixService
                if (paramType.getRawType() == AbstractHelixService.class) {
                    Type typeArg = paramType.getActualTypeArguments()[0];

                    if (typeArg instanceof Class) {
                        return (Class<T>) typeArg;
                    } else if (typeArg instanceof ParameterizedType) {
                        // Handle cases like AbstractHelixService<List<User>>
                        return (Class<T>) ((ParameterizedType) typeArg).getRawType();
                    }

                    throw new IllegalStateException(
                            "Cannot resolve entity class: type argument '" + typeArg +
                                    "' is not a concrete class. " +
                                    "Please use the constructor with explicit entityClass parameter: " +
                                    "super(queryFactory, YourEntity.class)");
                }
            }

            clazz = clazz.getSuperclass();
        }

        throw new IllegalStateException(
                "Cannot resolve entity class from generic type for " + getClass().getName() + ". " +
                        "Please use the constructor with explicit entityClass parameter: " +
                        "super(queryFactory, YourEntity.class)");
    }

    // ==================== CRUD Operations ====================

    /**
     * Save an entity - persist if new (no ID), merge if existing (has ID).
     * This method automatically determines whether to use persist or merge
     * based on the entity's identifier value.
     *
     * <pre>{@code
     * // New entity (ID is null) - calls persist
     * User newUser = new User("John", "john@example.com");
     * User saved = userService.save(newUser);
     *
     * // Existing entity (ID is set) - calls merge
     * User existing = userService.findById(1L);
     * existing.setName("Updated Name");
     * User updated = userService.save(existing);
     * }</pre>
     *
     * @param entity the entity to save
     * @return the saved entity (same instance for persist, managed copy for merge)
     */
    public T save(T entity) {
        Object id = em.getEntityManagerFactory()
                .getPersistenceUnitUtil()
                .getIdentifier(entity);
        if (id == null) {
            em.persist(entity);
            return entity;
        } else {
            return em.merge(entity);
        }
    }

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
     * Start a query with an IN filter using the unified HelixField interface.
     * Filters entities where the field value is in the given collection.
     *
     * <pre>{@code
     * // Example usage:
     * List<Long> ticketIds = List.of(1L, 2L, 3L);
     * List<Ticket> tickets = ticketService.findBy(TicketFields.ID, ticketIds).query();
     *
     * // With additional conditions:
     * List<User> users = userService.findBy(UserFields.ID, userIds)
     *     .whereEqual(UserFields.STATUS, UserStatus.ACTIVE)
     *     .query();
     *
     * // With enum values:
     * List<UserStatus> statuses = List.of(UserStatus.ACTIVE, UserStatus.PENDING);
     * List<User> users = userService.findBy(UserFields.STATUS, statuses).query();
     * }</pre>
     *
     * @param field  the field to filter on (any HelixField type)
     * @param values the collection of values to match (IN clause)
     * @param <V>    the field value type
     * @return a new HelixQuery instance with the IN filter applied
     */
    public <V> HelixQuery<T> findBy(HelixField<V> field, Collection<? extends V> values) {
        if (values == null || values.isEmpty()) {
            return find().where(PredicateExpression.alwaysFalse());
        }
        return find().where(field.in(find().root(), values));
    }

    /**
     * Start a query with an IN filter for Field type.
     *
     * @param field  the field to filter on
     * @param values the collection of values to match
     * @param <V>    the field value type
     * @return a new HelixQuery instance with the IN filter applied
     */
    public <V> HelixQuery<T> findBy(Field<V> field, Collection<? extends V> values) {
        if (values == null || values.isEmpty()) {
            return find().where(PredicateExpression.alwaysFalse());
        }
        return find().whereIn(field, values);
    }

    /**
     * Start a query with an IN filter for StringField type.
     *
     * @param field  the string field to filter on
     * @param values the collection of string values to match
     * @return a new HelixQuery instance with the IN filter applied
     */
    public HelixQuery<T> findBy(StringField field, Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return find().where(PredicateExpression.alwaysFalse());
        }
        return find().whereIn(field, values);
    }

    /**
     * Start a query with an IN filter for NumberField type.
     *
     * @param field  the number field to filter on
     * @param values the collection of number values to match
     * @param <V>    the number type
     * @return a new HelixQuery instance with the IN filter applied
     */
    public <V extends Number & Comparable<V>> HelixQuery<T> findBy(NumberField<V> field, Collection<? extends V> values) {
        if (values == null || values.isEmpty()) {
            return find().where(PredicateExpression.alwaysFalse());
        }
        return find().whereIn(field, values);
    }

    /**
     * Start a query with an IN filter for ComparableField type.
     *
     * @param field  the comparable field to filter on
     * @param values the collection of comparable values to match
     * @param <V>    the comparable type
     * @return a new HelixQuery instance with the IN filter applied
     */
    public <V extends Comparable<? super V>> HelixQuery<T> findBy(ComparableField<V> field, Collection<? extends V> values) {
        if (values == null || values.isEmpty()) {
            return find().where(PredicateExpression.alwaysFalse());
        }
        return find().whereIn(field, values);
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
