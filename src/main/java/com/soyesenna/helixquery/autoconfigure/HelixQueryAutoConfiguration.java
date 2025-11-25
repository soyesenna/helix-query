package com.soyesenna.helixquery.autoconfigure;

import com.soyesenna.helixquery.HelixQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configures {@link HelixQueryFactory} using the Spring-managed {@link EntityManager}.
 * This keeps HelixQuery aligned with the active persistence context and transaction boundaries.
 */
@AutoConfiguration
@ConditionalOnClass({EntityManager.class})
public class HelixQueryAutoConfiguration {

    /**
     * Creates a HelixQueryFactory bean that uses the current EntityManager.
     * The factory creates HelixQuery instances for type-safe querying.
     *
     * @param entityManager the JPA entity manager
     * @return a configured HelixQueryFactory
     */
    @Bean
    @ConditionalOnMissingBean
    public HelixQueryFactory helixQueryFactory(EntityManager entityManager) {
        return new HelixQueryFactory(entityManager);
    }
}
