package com.soyesenna.helixquery;

import com.soyesenna.helixquery.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class HelixQueryFactoryTest {

    @Test
    void fixedEntityManagerConstructorRejectsNull() {
        assertThrows(NullPointerException.class, () -> new HelixQueryFactory((EntityManager) null));
    }

    @Test
    void supplierConstructorRejectsNull() {
        assertThrows(NullPointerException.class, () -> new HelixQueryFactory((java.util.function.Supplier<EntityManager>) null));
    }

    @Test
    void supplierIsInvokedPerAccess() {
        EntityManager em = mock(EntityManager.class);
        AtomicInteger calls = new AtomicInteger();

        HelixQueryFactory factory = new HelixQueryFactory(() -> {
            calls.incrementAndGet();
            return em;
        });

        assertSame(em, factory.getEntityManager());
        assertNotNull(factory.query(User.class));
        assertNotNull(factory.selectFrom(User.class));
        assertEquals(3, calls.get());
    }
}
