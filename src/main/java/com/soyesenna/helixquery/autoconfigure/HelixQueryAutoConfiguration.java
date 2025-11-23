package com.soyesenna.helixquery.autoconfigure;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configures {@link JPAQueryFactory} using the Spring-managed {@link EntityManager}.
 * This keeps HelixQuery's Querydsl usage aligned with the active persistence context.
 */
@AutoConfiguration
@ConditionalOnClass({JPAQueryFactory.class, EntityManager.class})
public class HelixQueryAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
        return new JPAQueryFactory(entityManager);
    }
}
