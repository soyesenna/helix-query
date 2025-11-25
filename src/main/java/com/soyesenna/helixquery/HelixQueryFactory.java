package com.soyesenna.helixquery;

import jakarta.persistence.EntityManager;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Factory for creating HelixQuery instances.
 * Thread-safe and designed for Spring dependency injection.
 */
public class HelixQueryFactory {

    private final Supplier<EntityManager> entityManagerSupplier;

    /**
     * Create a factory with a fixed EntityManager.
     * Note: In a Spring environment, prefer the Supplier variant
     * to properly handle transaction-scoped EntityManagers.
     *
     * @param entityManager the JPA entity manager
     */
    public HelixQueryFactory(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager must not be null");
        this.entityManagerSupplier = () -> entityManager;
    }

    /**
     * Create a factory with an EntityManager supplier.
     * Use this in Spring environments to get the current
     * transaction-bound EntityManager.
     *
     * @param entityManagerSupplier supplier of the entity manager
     */
    public HelixQueryFactory(Supplier<EntityManager> entityManagerSupplier) {
        this.entityManagerSupplier = Objects.requireNonNull(entityManagerSupplier,
                "entityManagerSupplier must not be null");
    }

    /**
     * Create a new query for the given entity class.
     *
     * @param entityClass the entity class to query
     * @param <T>         the entity type
     * @return a new HelixQuery instance
     */
    public <T> HelixQuery<T> query(Class<T> entityClass) {
        return new HelixQuery<>(entityManagerSupplier.get(), entityClass);
    }

    /**
     * Alias for query() for compatibility with QueryDSL-style API.
     *
     * @param entityClass the entity class to query
     * @param <T>         the entity type
     * @return a new HelixQuery instance
     */
    public <T> HelixQuery<T> selectFrom(Class<T> entityClass) {
        return query(entityClass);
    }

    /**
     * Get the EntityManager.
     * Useful for direct JPA operations when needed.
     *
     * @return the entity manager
     */
    public EntityManager getEntityManager() {
        return entityManagerSupplier.get();
    }
}
