package com.soyesenna.helixquery;

import com.soyesenna.helixquery.entity.User;
import com.soyesenna.helixquery.entity.UserFields;
import com.soyesenna.helixquery.entity.UserStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TransactionRequiredException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class HelixQueryTransactionErrorTest {

    @Configuration
    @EnableAutoConfiguration
    static class TestConfig {
        @Bean
        public HelixQueryFactory helixQueryFactory(EntityManager entityManager) {
            return new HelixQueryFactory(entityManager);
        }
    }

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private HelixQueryFactory queryFactory;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate tx;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactionManager);
        tx.execute(status -> {
            entityManager.createQuery("DELETE FROM User").executeUpdate();
            entityManager.flush();
            entityManager.clear();

            User alice = new User("Alice", "alice@example.com", 30);
            alice.setActive(true);
            alice.setStatus(UserStatus.ACTIVE);
            entityManager.persist(alice);
            return null;
        });
    }

    @AfterEach
    void tearDown() {
        tx.execute(status -> {
            entityManager.createQuery("DELETE FROM User").executeUpdate();
            entityManager.flush();
            entityManager.clear();
            return null;
        });
    }

    @Test
    @DisplayName("delete() - should throw TransactionRequiredException when no transaction is active")
    void deleteShouldThrowWhenNoTransaction() {
        assertThrows(TransactionRequiredException.class, () ->
                queryFactory.query(User.class)
                        .whereEqual(UserFields.NAME, "Alice")
                        .delete()
        );
    }

    @Test
    @DisplayName("deleteBulk() - should throw TransactionRequiredException when no transaction is active")
    void deleteBulkShouldThrowWhenNoTransaction() {
        assertThrows(TransactionRequiredException.class, () ->
                queryFactory.query(User.class)
                        .whereEqual(UserFields.NAME, "Alice")
                        .deleteBulk()
        );
    }
}

