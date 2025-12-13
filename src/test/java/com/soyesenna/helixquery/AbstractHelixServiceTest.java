package com.soyesenna.helixquery;

import com.soyesenna.helixquery.entity.User;
import com.soyesenna.helixquery.entity.UserFields;
import com.soyesenna.helixquery.entity.UserStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class AbstractHelixServiceTest {

    @Configuration
    @EnableAutoConfiguration
    static class TestConfig {
        @Bean
        public HelixQueryFactory helixQueryFactory(EntityManager entityManager) {
            return new HelixQueryFactory(entityManager);
        }

        @Bean
        public UserService userService() {
            return new UserService();
        }
    }

    static class UserService extends AbstractHelixService<User> {
    }

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserService userService;

    @Test
    void resolvesEntityClassFromGenericAndCanQuery() {
        User user = new User("Alice", "alice@example.com", 30);
        user.setActive(true);
        user.setStatus(UserStatus.ACTIVE);
        entityManager.persist(user);
        entityManager.flush();
        entityManager.clear();

        long count = userService.find()
                .whereEqual(UserFields.NAME, "Alice")
                .queryCount();

        assertEquals(1, count);
    }

    @Test
    void saveUsesPersistWhenIdIsNull() {
        User user = new User("Bob", "bob@example.com", 25);
        user.setActive(true);
        user.setStatus(UserStatus.ACTIVE);

        User saved = userService.save(user);
        assertSame(user, saved);

        userService.flush();
        assertNotNull(user.getId());
    }

    @Test
    void saveUsesMergeWhenIdIsPresent() {
        User user = new User("Charlie", "charlie@example.com", 35);
        user.setActive(false);
        user.setStatus(UserStatus.INACTIVE);
        entityManager.persist(user);
        entityManager.flush();
        entityManager.detach(user);

        user.setName("Charlie Updated");

        User merged = userService.save(user);
        assertNotSame(user, merged);
        assertEquals(user.getId(), merged.getId());

        userService.flush();
        entityManager.clear();

        User reloaded = entityManager.find(User.class, merged.getId());
        assertEquals("Charlie Updated", reloaded.getName());
    }
}

