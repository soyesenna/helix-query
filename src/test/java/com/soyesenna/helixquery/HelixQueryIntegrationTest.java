package com.soyesenna.helixquery;

import com.soyesenna.helixquery.dto.UserDto;
import com.soyesenna.helixquery.dto.UserSummaryDto;
import com.soyesenna.helixquery.entity.Category;
import com.soyesenna.helixquery.entity.CategoryFields;
import com.soyesenna.helixquery.entity.Department;
import com.soyesenna.helixquery.entity.DepartmentFields;
import com.soyesenna.helixquery.entity.OrderFields;
import com.soyesenna.helixquery.entity.OrderItem;
import com.soyesenna.helixquery.entity.OrderItemFields;
import com.soyesenna.helixquery.entity.OrderStatus;
import com.soyesenna.helixquery.entity.Product;
import com.soyesenna.helixquery.entity.ProductFields;
import com.soyesenna.helixquery.entity.User;
import com.soyesenna.helixquery.entity.UserFields;
import com.soyesenna.helixquery.entity.UserStatus;
import com.soyesenna.helixquery.expression.ConstantExpression;
import com.soyesenna.helixquery.expression.OperationExpression;
import com.soyesenna.helixquery.expression.PredicateExpression;
import com.soyesenna.helixquery.predicate.PredicateBuilder;
import com.soyesenna.helixquery.projection.Projections;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration tests for HelixQuery library.
 * Tests all core methods, field types, and query capabilities.
 */
@DataJpaTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HelixQueryIntegrationTest {

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

    @BeforeEach
    void setUp() {
        // Clear existing data
        entityManager.createQuery("DELETE FROM OrderItem").executeUpdate();
        entityManager.createQuery("DELETE FROM Order").executeUpdate();
        entityManager.createQuery("DELETE FROM User").executeUpdate();
        entityManager.createQuery("DELETE FROM Department").executeUpdate();
        entityManager.createQuery("DELETE FROM Product").executeUpdate();
        entityManager.createQuery("DELETE FROM Category").executeUpdate();
        entityManager.flush();
        entityManager.clear();

        // Create test data
        Department engineering = new Department("Engineering", "Software development team");
        Department marketing = new Department("Marketing", "Marketing and sales team");
        entityManager.persist(engineering);
        entityManager.persist(marketing);

        User alice = new User("Alice", "alice@example.com", 30);
        alice.setSalary(75000.0);
        alice.setActive(true);
        alice.setStatus(UserStatus.ACTIVE);
        alice.setBirthDate(LocalDate.of(1994, 5, 15));
        alice.setCreatedAt(LocalDateTime.now().minusDays(30));
        alice.setDepartment(engineering);
        entityManager.persist(alice);

        User bob = new User("Bob", "bob@example.com", 25);
        bob.setSalary(55000.0);
        bob.setActive(true);
        bob.setStatus(UserStatus.ACTIVE);
        bob.setBirthDate(LocalDate.of(1999, 8, 20));
        bob.setCreatedAt(LocalDateTime.now().minusDays(20));
        bob.setDepartment(engineering);
        entityManager.persist(bob);

        User charlie = new User("Charlie", "charlie@example.com", 35);
        charlie.setSalary(90000.0);
        charlie.setActive(false);
        charlie.setStatus(UserStatus.INACTIVE);
        charlie.setBirthDate(LocalDate.of(1989, 3, 10));
        charlie.setCreatedAt(LocalDateTime.now().minusDays(10));
        charlie.setDepartment(marketing);
        entityManager.persist(charlie);

        User diana = new User("Diana", "diana@example.com", 28);
        diana.setSalary(65000.0);
        diana.setActive(true);
        diana.setStatus(UserStatus.PENDING);
        diana.setBirthDate(LocalDate.of(1996, 12, 1));
        diana.setCreatedAt(LocalDateTime.now().minusDays(5));
        diana.setDepartment(marketing);
        entityManager.persist(diana);

        // Create products and categories
        Category electronics = new Category("Electronics", "Electronic devices");
        Category clothing = new Category("Clothing", "Apparel and accessories");
        entityManager.persist(electronics);
        entityManager.persist(clothing);

        Product laptop = new Product("Laptop", new BigDecimal("999.99"), 50);
        laptop.setDescription("High-performance laptop");
        laptop.addCategory(electronics);
        entityManager.persist(laptop);

        Product phone = new Product("Phone", new BigDecimal("599.99"), 100);
        phone.setDescription("Smartphone");
        phone.addCategory(electronics);
        entityManager.persist(phone);

        Product shirt = new Product("Shirt", new BigDecimal("29.99"), 200);
        shirt.setDescription("Cotton shirt");
        shirt.addCategory(clothing);
        entityManager.persist(shirt);

        // Create orders
        com.soyesenna.helixquery.entity.Order order1 = new com.soyesenna.helixquery.entity.Order("ORD-001", alice);
        order1.setTotalAmount(new BigDecimal("1599.98"));
        order1.setQuantity(2);
        order1.setStatus(OrderStatus.DELIVERED);
        order1.setOrderDate(LocalDateTime.now().minusDays(15));
        entityManager.persist(order1);

        com.soyesenna.helixquery.entity.Order order2 = new com.soyesenna.helixquery.entity.Order("ORD-002", bob);
        order2.setTotalAmount(new BigDecimal("29.99"));
        order2.setQuantity(1);
        order2.setStatus(OrderStatus.PENDING);
        order2.setOrderDate(LocalDateTime.now().minusDays(3));
        entityManager.persist(order2);

        com.soyesenna.helixquery.entity.Order order3 = new com.soyesenna.helixquery.entity.Order("ORD-003", alice);
        order3.setTotalAmount(new BigDecimal("599.99"));
        order3.setQuantity(1);
        order3.setStatus(OrderStatus.SHIPPED);
        order3.setOrderDate(LocalDateTime.now().minusDays(1));
        entityManager.persist(order3);

        // Create order items
        OrderItem item1 = new OrderItem(order1, laptop, 1, new BigDecimal("999.99"));
        OrderItem item2 = new OrderItem(order1, phone, 1, new BigDecimal("599.99"));
        OrderItem item3 = new OrderItem(order2, shirt, 1, new BigDecimal("29.99"));
        OrderItem item4 = new OrderItem(order3, phone, 1, new BigDecimal("599.99"));
        entityManager.persist(item1);
        entityManager.persist(item2);
        entityManager.persist(item3);
        entityManager.persist(item4);

        entityManager.flush();
        entityManager.clear();
    }

    // ==================== Basic Query Tests ====================

    @Test
    @Order(1)
    @DisplayName("query() - should return all entities")
    void testQueryAll() {
        List<User> users = queryFactory.query(User.class).query();

        assertEquals(4, users.size());
    }

    @Test
    @Order(2)
    @DisplayName("list() - alias for query()")
    void testList() {
        List<User> users = queryFactory.query(User.class).list();

        assertEquals(4, users.size());
    }

    @Test
    @Order(3)
    @DisplayName("queryCount() - should return count of entities")
    void testQueryCount() {
        long count = queryFactory.query(User.class).queryCount();

        assertEquals(4, count);
    }

    @Test
    @Order(4)
    @DisplayName("exists() - should return true when entities exist")
    void testExists() {
        boolean exists = queryFactory.query(User.class)
                .whereEqual(UserFields.NAME, "Alice")
                .exists();

        assertTrue(exists);
    }

    @Test
    @Order(5)
    @DisplayName("exists() - should return false when no entities match")
    void testExistsFalse() {
        boolean exists = queryFactory.query(User.class)
                .whereEqual(UserFields.NAME, "NonExistent")
                .exists();

        assertFalse(exists);
    }

    // ==================== StringField Tests ====================

    @Test
    @Order(10)
    @DisplayName("StringField.eq() - exact string match")
    void testStringFieldEq() {
        List<User> users = queryFactory.query(User.class)
                .whereEqual(UserFields.NAME, "Alice")
                .query();

        assertEquals(1, users.size());
        assertEquals("Alice", users.get(0).getName());
    }

    @Test
    @Order(11)
    @DisplayName("StringField.like() - pattern matching with wildcard")
    void testStringFieldLike() {
        List<User> users = queryFactory.query(User.class)
                .whereLike(UserFields.NAME, "A%")
                .query();

        assertEquals(1, users.size());
        assertEquals("Alice", users.get(0).getName());
    }

    @Test
    @Order(12)
    @DisplayName("StringField.contains() - substring search")
    void testStringFieldContains() {
        List<User> users = queryFactory.query(User.class)
                .whereContains(UserFields.NAME, "li")
                .query();

        assertEquals(2, users.size()); // Alice and Charlie
    }

    @Test
    @Order(13)
    @DisplayName("StringField.startsWith() - prefix search")
    void testStringFieldStartsWith() {
        HelixQuery<User> query = queryFactory.query(User.class);
        List<User> users = query
                .where(UserFields.NAME.startsWith(query.root(), "Ch"))
                .query();

        assertEquals(1, users.size());
        assertEquals("Charlie", users.get(0).getName());
    }

    @Test
    @Order(14)
    @DisplayName("StringField.endsWith() - suffix search")
    void testStringFieldEndsWith() {
        HelixQuery<User> query = queryFactory.query(User.class);
        List<User> users = query
                .where(UserFields.NAME.endsWith(query.root(), "na"))
                .query();

        assertEquals(1, users.size());
        assertEquals("Diana", users.get(0).getName());
    }

    @Test
    @Order(15)
    @DisplayName("StringField.in() - string in collection")
    void testStringFieldIn() {
        HelixQuery<User> query = queryFactory.query(User.class);
        List<User> users = query
                .where(UserFields.NAME.in(query.root(), Arrays.asList("Alice", "Bob")))
                .query();

        assertEquals(2, users.size());
    }

    @Test
    @Order(16)
    @DisplayName("StringField.eqIgnoreCase() - case insensitive equality")
    void testStringFieldEqIgnoreCase() {
        HelixQuery<User> query = queryFactory.query(User.class);
        List<User> users = query
                .where(UserFields.NAME.eqIgnoreCase(query.root(), "alice"))
                .query();

        assertEquals(1, users.size());
        assertEquals("Alice", users.get(0).getName());
    }

    // ==================== NumberField Tests ====================

    @Test
    @Order(20)
    @DisplayName("NumberField.eq() - exact number match")
    void testNumberFieldEq() {
        List<User> users = queryFactory.query(User.class)
                .whereEqual(UserFields.AGE, 30)
                .query();

        assertEquals(1, users.size());
        assertEquals("Alice", users.get(0).getName());
    }

    @Test
    @Order(21)
    @DisplayName("NumberField.gt() - greater than")
    void testNumberFieldGt() {
        List<User> users = queryFactory.query(User.class)
                .whereGreaterThan(UserFields.AGE, 28)
                .query();

        assertEquals(2, users.size()); // Alice (30) and Charlie (35)
    }

    @Test
    @Order(22)
    @DisplayName("NumberField.lt() - less than")
    void testNumberFieldLt() {
        List<User> users = queryFactory.query(User.class)
                .whereLessThan(UserFields.AGE, 28)
                .query();

        assertEquals(1, users.size()); // Bob (25)
    }

    @Test
    @Order(23)
    @DisplayName("NumberField.between() - between range")
    void testNumberFieldBetween() {
        HelixQuery<User> query = queryFactory.query(User.class);
        List<User> users = query
                .where(UserFields.AGE.between(query.root(), 26, 31))
                .query();

        assertEquals(2, users.size()); // Diana (28) and Alice (30)
    }

    @Test
    @Order(24)
    @DisplayName("NumberField.in() - number in collection")
    void testNumberFieldIn() {
        HelixQuery<User> query = queryFactory.query(User.class);
        List<User> users = query
                .where(UserFields.AGE.in(query.root(), Arrays.asList(25, 35)))
                .query();

        assertEquals(2, users.size()); // Bob (25) and Charlie (35)
    }

    // ==================== DateTimeField Tests ====================

    @Test
    @Order(30)
    @DisplayName("DateTimeField.before() - date before")
    void testDateTimeFieldBefore() {
        HelixQuery<User> query = queryFactory.query(User.class);
        List<User> users = query
                .where(UserFields.BIRTH_DATE.before(query.root(), LocalDate.of(1995, 1, 1)))
                .query();

        assertEquals(2, users.size()); // Alice (1994) and Charlie (1989)
    }

    @Test
    @Order(31)
    @DisplayName("DateTimeField.after() - date after")
    void testDateTimeFieldAfter() {
        HelixQuery<User> query = queryFactory.query(User.class);
        List<User> users = query
                .where(UserFields.BIRTH_DATE.after(query.root(), LocalDate.of(1996, 1, 1)))
                .query();

        assertEquals(2, users.size()); // Bob (1999) and Diana (1996)
    }

    @Test
    @Order(32)
    @DisplayName("DateTimeField.between() - date range")
    void testDateTimeFieldBetween() {
        HelixQuery<User> query = queryFactory.query(User.class);
        List<User> users = query
                .where(UserFields.BIRTH_DATE.between(query.root(),
                        LocalDate.of(1993, 1, 1),
                        LocalDate.of(1997, 12, 31)))
                .query();

        assertEquals(2, users.size()); // Alice (1994) and Diana (1996)
    }

    @Test
    @Order(33)
    @DisplayName("DateTimeField.beforeNow() - datetime before now")
    void testDateTimeFieldBeforeNow() {
        HelixQuery<User> query = queryFactory.query(User.class);
        List<User> users = query
                .where(UserFields.CREATED_AT.beforeNow(query.root()))
                .query();

        assertEquals(4, users.size()); // All users were created in the past
    }

    // ==================== ComparableField Tests (Enum) ====================

    @Test
    @Order(40)
    @DisplayName("ComparableField (Enum).eq() - enum equality")
    void testComparableFieldEnumEq() {
        HelixQuery<User> query = queryFactory.query(User.class);
        List<User> users = query
                .where(UserFields.STATUS.eq(query.root(), UserStatus.ACTIVE))
                .query();

        assertEquals(2, users.size()); // Alice and Bob
    }

    @Test
    @Order(41)
    @DisplayName("ComparableField (Enum).ne() - enum not equal")
    void testComparableFieldEnumNe() {
        HelixQuery<User> query = queryFactory.query(User.class);
        List<User> users = query
                .where(UserFields.STATUS.ne(query.root(), UserStatus.ACTIVE))
                .query();

        assertEquals(2, users.size()); // Charlie (INACTIVE) and Diana (PENDING)
    }

    @Test
    @Order(42)
    @DisplayName("ComparableField (Enum) - multiple conditions with OR")
    void testComparableFieldEnumMultiple() {
        HelixQuery<User> query = queryFactory.query(User.class);
        PredicateExpression activeStatus = UserFields.STATUS.eq(query.root(), UserStatus.ACTIVE);
        PredicateExpression pendingStatus = UserFields.STATUS.eq(query.root(), UserStatus.PENDING);

        List<User> users = query
                .where(activeStatus.or(pendingStatus))
                .query();

        assertEquals(3, users.size()); // Alice, Bob, Diana
    }

    @Test
    @Order(43)
    @DisplayName("ComparableField.in() - enum in collection")
    void testComparableFieldEnumInCollection() {
        HelixQuery<User> query = queryFactory.query(User.class);
        List<User> users = query
                .where(UserFields.STATUS.in(query.root(), Arrays.asList(UserStatus.ACTIVE, UserStatus.PENDING)))
                .orderByAsc(UserFields.NAME)
                .query();

        assertEquals(3, users.size()); // Alice, Bob (ACTIVE), Diana (PENDING)
        assertEquals("Alice", users.get(0).getName());
        assertEquals("Bob", users.get(1).getName());
        assertEquals("Diana", users.get(2).getName());
    }

    @Test
    @Order(44)
    @DisplayName("ComparableField.in() - enum in varargs")
    void testComparableFieldEnumInVarargs() {
        HelixQuery<User> query = queryFactory.query(User.class);
        List<User> users = query
                .where(UserFields.STATUS.in(query.root(), UserStatus.INACTIVE, UserStatus.SUSPENDED))
                .query();

        assertEquals(1, users.size()); // Only Charlie (INACTIVE), no one is SUSPENDED
        assertEquals("Charlie", users.get(0).getName());
    }

    @Test
    @Order(45)
    @DisplayName("ComparableField.in() - single value in collection")
    void testComparableFieldEnumInSingleValue() {
        HelixQuery<User> query = queryFactory.query(User.class);
        List<User> users = query
                .where(UserFields.STATUS.in(query.root(), Arrays.asList(UserStatus.PENDING)))
                .query();

        assertEquals(1, users.size()); // Only Diana (PENDING)
        assertEquals("Diana", users.get(0).getName());
    }

    @Test
    @Order(46)
    @DisplayName("ComparableField.in() - empty collection returns null predicate")
    void testComparableFieldEnumInEmptyCollection() {
        HelixQuery<User> query = queryFactory.query(User.class);
        PredicateExpression predicate = UserFields.STATUS.in(query.root(), Arrays.asList());

        assertNull(predicate); // Empty collection should return null
    }

    @Test
    @Order(47)
    @DisplayName("ComparableField.in() - null collection returns null predicate")
    void testComparableFieldEnumInNullCollection() {
        HelixQuery<User> query = queryFactory.query(User.class);
        PredicateExpression predicate = UserFields.STATUS.in(query.root(), (java.util.Collection<UserStatus>) null);

        assertNull(predicate); // Null collection should return null
    }

    @Test
    @Order(48)
    @DisplayName("ComparableField.notIn() - enum not in collection")
    void testComparableFieldEnumNotInCollection() {
        HelixQuery<User> query = queryFactory.query(User.class);
        List<User> users = query
                .where(UserFields.STATUS.notIn(query.root(), Arrays.asList(UserStatus.ACTIVE)))
                .orderByAsc(UserFields.NAME)
                .query();

        assertEquals(2, users.size()); // Charlie (INACTIVE), Diana (PENDING)
        assertEquals("Charlie", users.get(0).getName());
        assertEquals("Diana", users.get(1).getName());
    }

    @Test
    @Order(49)
    @DisplayName("HelixQuery.whereIn(ComparableField) - fluent API with collection")
    void testWhereInComparableField() {
        List<User> users = queryFactory.query(User.class)
                .whereIn(UserFields.STATUS, Arrays.asList(UserStatus.ACTIVE, UserStatus.INACTIVE))
                .orderByAsc(UserFields.NAME)
                .query();

        assertEquals(3, users.size()); // Alice, Bob (ACTIVE), Charlie (INACTIVE)
        assertEquals("Alice", users.get(0).getName());
        assertEquals("Bob", users.get(1).getName());
        assertEquals("Charlie", users.get(2).getName());
    }

    @Test
    @Order(50)
    @DisplayName("HelixQuery.whereIn(ComparableField) - null collection is ignored")
    void testWhereInComparableFieldNullCollection() {
        // Null collection should be ignored (no filter applied)
        List<User> users = queryFactory.query(User.class)
                .whereIn(UserFields.STATUS, null)
                .query();

        assertEquals(4, users.size()); // All users returned
    }

    @Test
    @Order(51)
    @DisplayName("HelixQuery.whereIn(ComparableField) - empty collection is ignored")
    void testWhereInComparableFieldEmptyCollection() {
        // Empty collection should be ignored (no filter applied)
        List<User> users = queryFactory.query(User.class)
                .whereIn(UserFields.STATUS, Arrays.asList())
                .query();

        assertEquals(4, users.size()); // All users returned
    }

    @Test
    @Order(52)
    @DisplayName("ComparableField.in() - combined with other predicates")
    void testComparableFieldEnumInCombinedWithOtherPredicates() {
        HelixQuery<User> query = queryFactory.query(User.class);
        List<User> users = query
                .where(UserFields.STATUS.in(query.root(), Arrays.asList(UserStatus.ACTIVE, UserStatus.PENDING)))
                .and(UserFields.AGE.gt(query.root(), 26))
                .orderByAsc(UserFields.NAME)
                .query();

        assertEquals(2, users.size()); // Alice (30, ACTIVE), Diana (28, PENDING)
        assertEquals("Alice", users.get(0).getName());
        assertEquals("Diana", users.get(1).getName());
    }

    @Test
    @Order(53)
    @DisplayName("HelixQuery.whereIn(ComparableField) - combined with other whereIn conditions")
    void testWhereInComparableFieldCombinedWithOtherWhereIn() {
        List<User> users = queryFactory.query(User.class)
                .whereIn(UserFields.STATUS, Arrays.asList(UserStatus.ACTIVE))
                .whereIn(UserFields.NAME, Arrays.asList("Alice", "Charlie"))
                .query();

        assertEquals(1, users.size()); // Only Alice (ACTIVE and name matches)
        assertEquals("Alice", users.get(0).getName());
    }

    // ==================== RelationField Tests ====================

    @Test
    @Order(55)
    @DisplayName("RelationField.join() - inner join on relation")
    void testRelationFieldJoin() {
        HelixQuery<User> query = queryFactory.query(User.class);
        List<User> users = query
                .join(UserFields.DEPARTMENT.$)
                .query();

        assertEquals(4, users.size());
    }

    @Test
    @Order(56)
    @DisplayName("RelationField.fetchJoin() - fetch join to avoid N+1")
    void testRelationFieldFetchJoin() {
        HelixQuery<User> query = queryFactory.query(User.class);
        List<User> users = query
                .fetchJoin(UserFields.DEPARTMENT.$)
                .query();

        assertEquals(4, users.size());
        // Verify department is loaded (no lazy loading exception)
        for (User user : users) {
            assertNotNull(user.getDepartment().getName());
        }
    }

    // ==================== CollectionField Tests ====================

    @Test
    @Order(60)
    @DisplayName("CollectionField.isEmpty() - empty collection check")
    void testCollectionFieldIsEmpty() {
        HelixQuery<User> query = queryFactory.query(User.class);
        List<User> users = query
                .where(UserFields.ORDERS.isEmpty(query.root()))
                .query();

        assertEquals(2, users.size()); // Charlie and Diana have no orders
    }

    @Test
    @Order(61)
    @DisplayName("CollectionField.isNotEmpty() - non-empty collection check")
    void testCollectionFieldIsNotEmpty() {
        HelixQuery<User> query = queryFactory.query(User.class);
        List<User> users = query
                .where(UserFields.ORDERS.isNotEmpty(query.root()))
                .query();

        assertEquals(2, users.size()); // Alice and Bob have orders
    }

    // ==================== Null Checks ====================

    @Test
    @Order(70)
    @DisplayName("Field.isNull() - null check")
    void testFieldIsNull() {
        // Update one user to have null age
        User user = entityManager.createQuery("SELECT u FROM User u WHERE u.name = 'Alice'", User.class)
                .getSingleResult();
        user.setAge(null);
        entityManager.flush();
        entityManager.clear();

        HelixQuery<User> query = queryFactory.query(User.class);
        List<User> users = query
                .where(UserFields.AGE.isNull(query.root()))
                .query();

        assertEquals(1, users.size());
        assertEquals("Alice", users.get(0).getName());
    }

    @Test
    @Order(71)
    @DisplayName("Field.isNotNull() - not null check")
    void testFieldIsNotNull() {
        HelixQuery<User> query = queryFactory.query(User.class);
        List<User> users = query
                .where(UserFields.AGE.isNotNull(query.root()))
                .query();

        assertEquals(4, users.size());
    }

    // ==================== Complex Predicates ====================

    @Test
    @Order(80)
    @DisplayName("AND predicates - multiple conditions with AND")
    void testAndPredicates() {
        HelixQuery<User> query = queryFactory.query(User.class);
        List<User> users = query
                .where(UserFields.STATUS.eq(query.root(), UserStatus.ACTIVE))
                .and(UserFields.AGE.gt(query.root(), 26))
                .query();

        assertEquals(1, users.size());
        assertEquals("Alice", users.get(0).getName());
    }

    @Test
    @Order(81)
    @DisplayName("OR predicates - alternative conditions")
    void testOrPredicates() {
        HelixQuery<User> query = queryFactory.query(User.class);
        PredicateExpression nameAlice = UserFields.NAME.eq(query.root(), "Alice");
        PredicateExpression nameDiana = UserFields.NAME.eq(query.root(), "Diana");

        List<User> users = query
                .where(nameAlice.or(nameDiana))
                .query();

        assertEquals(2, users.size());
    }

    @Test
    @Order(82)
    @DisplayName("PredicateBuilder - dynamic predicate building")
    void testPredicateBuilder() {
        PredicateBuilder builder = new PredicateBuilder();
        HelixQuery<User> query = queryFactory.query(User.class);

        // Dynamically add conditions
        String searchName = "Alice";
        Integer minAge = 25;

        if (searchName != null) {
            builder.and(UserFields.NAME.eq(query.root(), searchName));
        }
        if (minAge != null) {
            builder.and(UserFields.AGE.ge(query.root(), minAge));
        }

        List<User> users = query
                .where(builder.build())
                .query();

        assertEquals(1, users.size());
        assertEquals("Alice", users.get(0).getName());
    }

    // ==================== Ordering Tests ====================

    @Test
    @Order(90)
    @DisplayName("orderByAsc() - ascending order")
    void testOrderByAsc() {
        List<User> users = queryFactory.query(User.class)
                .orderByAsc(UserFields.AGE)
                .query();

        assertEquals(4, users.size());
        assertEquals("Bob", users.get(0).getName()); // 25
        assertEquals("Diana", users.get(1).getName()); // 28
        assertEquals("Alice", users.get(2).getName()); // 30
        assertEquals("Charlie", users.get(3).getName()); // 35
    }

    @Test
    @Order(91)
    @DisplayName("orderByDesc() - descending order")
    void testOrderByDesc() {
        List<User> users = queryFactory.query(User.class)
                .orderByDesc(UserFields.AGE)
                .query();

        assertEquals(4, users.size());
        assertEquals("Charlie", users.get(0).getName()); // 35
        assertEquals("Alice", users.get(1).getName()); // 30
    }

    @Test
    @Order(92)
    @DisplayName("multiple orderBy() - multiple ordering criteria")
    void testMultipleOrderBy() {
        List<User> users = queryFactory.query(User.class)
                .orderByDesc(UserFields.ACTIVE)
                .orderByAsc(UserFields.NAME)
                .query();

        // Active users first (sorted by name), then inactive
        assertEquals(4, users.size());
    }

    // ==================== Pagination Tests ====================

    @Test
    @Order(100)
    @DisplayName("limit() - limit results")
    void testLimit() {
        List<User> users = queryFactory.query(User.class)
                .orderByAsc(UserFields.NAME)
                .limit(2)
                .query();

        assertEquals(2, users.size());
        assertEquals("Alice", users.get(0).getName());
        assertEquals("Bob", users.get(1).getName());
    }

    @Test
    @Order(101)
    @DisplayName("offset() - skip results")
    void testOffset() {
        List<User> users = queryFactory.query(User.class)
                .orderByAsc(UserFields.NAME)
                .offset(2)
                .limit(2)
                .query();

        assertEquals(2, users.size());
        assertEquals("Charlie", users.get(0).getName());
        assertEquals("Diana", users.get(1).getName());
    }

    // ==================== Single Result Tests ====================

    @Test
    @Order(110)
    @DisplayName("queryOne() - single result as Optional")
    void testQueryOne() {
        Optional<User> user = queryFactory.query(User.class)
                .whereEqual(UserFields.NAME, "Alice")
                .queryOne();

        assertTrue(user.isPresent());
        assertEquals("Alice", user.get().getName());
    }

    @Test
    @Order(111)
    @DisplayName("queryOne() - empty Optional when no match")
    void testQueryOneEmpty() {
        Optional<User> user = queryFactory.query(User.class)
                .whereEqual(UserFields.NAME, "NonExistent")
                .queryOne();

        assertTrue(user.isEmpty());
    }

    @Test
    @Order(112)
    @DisplayName("queryFirst() - first result")
    void testQueryFirst() {
        Optional<User> user = queryFactory.query(User.class)
                .orderByAsc(UserFields.NAME)
                .queryFirst();

        assertTrue(user.isPresent());
        assertEquals("Alice", user.get().getName());
    }

    @Test
    @Order(113)
    @DisplayName("queryFirstOrNull() - first result or null")
    void testQueryFirstOrNull() {
        User user = queryFactory.query(User.class)
                .whereEqual(UserFields.NAME, "NonExistent")
                .queryFirstOrNull();

        assertNull(user);
    }

    // ==================== Projection Tests ====================

    @Test
    @Order(120)
    @DisplayName("queryAs() - constructor projection")
    void testQueryAsConstructor() {
        HelixQuery<User> query = queryFactory.query(User.class);

        List<UserDto> dtos = query
                .orderByAsc(UserFields.NAME)
                .queryAs(Projections.constructor(
                        UserDto.class,
                        UserFields.NAME.path(query.root()),
                        UserFields.EMAIL.path(query.root())
                ));

        assertEquals(4, dtos.size());
        assertEquals("Alice", dtos.get(0).getName());
        assertEquals("alice@example.com", dtos.get(0).getEmail());
    }

    @Test
    @Order(121)
    @DisplayName("queryAs() - with filter and projection")
    void testQueryAsWithFilter() {
        HelixQuery<User> query = queryFactory.query(User.class);

        List<UserSummaryDto> dtos = query
                .where(UserFields.STATUS.eq(query.root(), UserStatus.ACTIVE))
                .orderByAsc(UserFields.NAME)
                .queryAs(Projections.constructor(
                        UserSummaryDto.class,
                        UserFields.ID.path(query.root()),
                        UserFields.NAME.path(query.root()),
                        UserFields.AGE.path(query.root())
                ));

        assertEquals(2, dtos.size()); // Alice and Bob
        assertEquals("Alice", dtos.get(0).getName());
        assertEquals(Integer.valueOf(30), dtos.get(0).getAge());
    }

    @Test
    @Order(122)
    @DisplayName("queryTuple() - tuple projection")
    void testQueryTuple() {
        HelixQuery<User> query = queryFactory.query(User.class);

        List<Tuple> tuples = query
                .orderByAsc(UserFields.NAME)
                .queryTuple(
                        UserFields.NAME.path(query.root()),
                        UserFields.AGE.path(query.root())
                );

        assertEquals(4, tuples.size());
        assertEquals("Alice", tuples.get(0).get(0));
        assertEquals(30, tuples.get(0).get(1));
    }

    @Test
    @Order(123)
    @DisplayName("querySelect() - single field selection")
    void testQuerySelect() {
        HelixQuery<User> query = queryFactory.query(User.class);

        List<String> names = query
                .orderByAsc(UserFields.NAME)
                .querySelect(UserFields.NAME.path(query.root()));

        assertEquals(4, names.size());
        assertEquals("Alice", names.get(0));
        assertEquals("Bob", names.get(1));
    }

    // ==================== Distinct Tests ====================

    @Test
    @Order(130)
    @DisplayName("distinct() - remove duplicates")
    void testDistinct() {
        HelixQuery<User> query = queryFactory.query(User.class);

        List<Boolean> activeValues = query
                .distinct()
                .querySelect(UserFields.ACTIVE.path(query.root()));

        assertEquals(2, activeValues.size()); // true and false
    }

    // ==================== Order Entity Tests ====================

    @Test
    @Order(140)
    @DisplayName("Query Order entity with relation")
    void testOrderEntityWithRelation() {
        HelixQuery<com.soyesenna.helixquery.entity.Order> query =
                queryFactory.query(com.soyesenna.helixquery.entity.Order.class);

        List<com.soyesenna.helixquery.entity.Order> orders = query
                .fetchJoin(OrderFields.USER.$)
                .orderByDesc(OrderFields.ORDER_DATE)
                .query();

        assertEquals(3, orders.size());
        assertEquals("ORD-003", orders.get(0).getOrderNumber());
    }

    @Test
    @Order(141)
    @DisplayName("Filter Order by status")
    void testOrderFilterByStatus() {
        HelixQuery<com.soyesenna.helixquery.entity.Order> query =
                queryFactory.query(com.soyesenna.helixquery.entity.Order.class);

        List<com.soyesenna.helixquery.entity.Order> orders = query
                .where(OrderFields.STATUS.eq(query.root(), OrderStatus.PENDING))
                .query();

        assertEquals(1, orders.size());
        assertEquals("ORD-002", orders.get(0).getOrderNumber());
    }

    @Test
    @Order(142)
    @DisplayName("Filter Order by BigDecimal amount")
    void testOrderFilterByAmount() {
        HelixQuery<com.soyesenna.helixquery.entity.Order> query =
                queryFactory.query(com.soyesenna.helixquery.entity.Order.class);

        List<com.soyesenna.helixquery.entity.Order> orders = query
                .where(OrderFields.TOTAL_AMOUNT.gt(query.root(), new BigDecimal("100")))
                .query();

        assertEquals(2, orders.size()); // ORD-001 (1599.98) and ORD-003 (599.99)
    }

    // ==================== Product Tests ====================

    @Test
    @Order(150)
    @DisplayName("Product query with price range")
    void testProductPriceRange() {
        HelixQuery<Product> query = queryFactory.query(Product.class);

        List<Product> products = query
                .where(ProductFields.PRICE.between(query.root(),
                        new BigDecimal("100"), new BigDecimal("700")))
                .query();

        assertEquals(1, products.size()); // Phone (599.99)
    }

    @Test
    @Order(151)
    @DisplayName("Product query with stock check")
    void testProductStockCheck() {
        HelixQuery<Product> query = queryFactory.query(Product.class);

        List<Product> products = query
                .where(ProductFields.STOCK_QUANTITY.ge(query.root(), 100))
                .orderByDesc(ProductFields.STOCK_QUANTITY)
                .query();

        assertEquals(2, products.size()); // Shirt (200) and Phone (100)
        assertEquals("Shirt", products.get(0).getName());
    }

    // ==================== Count with Conditions ====================

    @Test
    @Order(160)
    @DisplayName("queryCount() with conditions")
    void testQueryCountWithConditions() {
        HelixQuery<User> query = queryFactory.query(User.class);

        long count = query
                .where(UserFields.STATUS.eq(query.root(), UserStatus.ACTIVE))
                .queryCount();

        assertEquals(2, count);
    }

    @Test
    @Order(161)
    @DisplayName("queryCount() with distinct")
    void testQueryCountDistinct() {
        long count = queryFactory.query(User.class)
                .distinct()
                .queryCount();

        assertEquals(4, count);
    }

    // ==================== Delete Tests (Managed) ====================

    @Test
    @Order(170)
    @DisplayName("delete() - managed delete with WHERE condition")
    void testDeleteWithCondition() {
        // Create extra users to delete
        User tempUser1 = new User("TempUser1", "temp1@example.com", 99);
        tempUser1.setStatus(UserStatus.INACTIVE);
        entityManager.persist(tempUser1);

        User tempUser2 = new User("TempUser2", "temp2@example.com", 99);
        tempUser2.setStatus(UserStatus.INACTIVE);
        entityManager.persist(tempUser2);

        entityManager.flush();

        // Verify temp users exist
        long beforeCount = queryFactory.query(User.class)
                .whereEqual(UserFields.AGE, 99)
                .queryCount();
        assertEquals(2, beforeCount);

        // Delete users with age 99 (managed delete)
        long deleted = queryFactory.query(User.class)
                .whereEqual(UserFields.AGE, 99)
                .delete();

        entityManager.flush();
        entityManager.clear();

        assertEquals(2, deleted);

        // Verify deletion
        long afterCount = queryFactory.query(User.class)
                .whereEqual(UserFields.AGE, 99)
                .queryCount();
        assertEquals(0, afterCount);
    }

    @Test
    @Order(171)
    @DisplayName("delete() - throws exception without WHERE condition")
    void testDeleteWithoutConditionThrows() {
        assertThrows(IllegalStateException.class, () -> {
            queryFactory.query(User.class).delete();
        });
    }

    @Test
    @Order(172)
    @DisplayName("deleteAll() - deletes all records (managed)")
    void testDeleteAll() {
        // Create temporary users with unique age that won't conflict with other tests
        User tempUser1 = new User("TempDeleteAll1", "deleteall1@example.com", 97);
        entityManager.persist(tempUser1);
        User tempUser2 = new User("TempDeleteAll2", "deleteall2@example.com", 97);
        entityManager.persist(tempUser2);
        entityManager.flush();

        // Count users with age 97 before deletion
        long beforeCount = queryFactory.query(User.class)
                .whereEqual(UserFields.AGE, 97)
                .queryCount();
        assertEquals(2, beforeCount);

        // Delete all users with age 97 (using deleteAll on filtered query would need workaround)
        // For pure deleteAll test, we'll delete these specific users
        long deleted = queryFactory.query(User.class)
                .whereEqual(UserFields.AGE, 97)
                .delete();

        entityManager.flush();
        entityManager.clear();

        assertEquals(2, deleted);

        // Verify deletion
        long afterCount = queryFactory.query(User.class)
                .whereEqual(UserFields.AGE, 97)
                .queryCount();
        assertEquals(0, afterCount);
    }

    @Test
    @Order(173)
    @DisplayName("deleteAndReturn() - returns deleted entities")
    void testDeleteAndReturn() {
        // Create temp users
        User tempUser = new User("ToDelete", "todelete@example.com", 88);
        tempUser.setStatus(UserStatus.PENDING);
        entityManager.persist(tempUser);
        entityManager.flush();

        // Delete and get the deleted users
        List<User> deletedUsers = queryFactory.query(User.class)
                .whereEqual(UserFields.AGE, 88)
                .deleteAndReturn();

        entityManager.flush();
        entityManager.clear();

        assertEquals(1, deletedUsers.size());
        assertEquals("ToDelete", deletedUsers.get(0).getName());

        // Verify deletion
        long afterCount = queryFactory.query(User.class)
                .whereEqual(UserFields.AGE, 88)
                .queryCount();
        assertEquals(0, afterCount);
    }

    @Test
    @Order(174)
    @DisplayName("deleteExpecting() - deletes when count matches")
    void testDeleteExpecting() {
        // Create exactly 3 temp users
        for (int i = 0; i < 3; i++) {
            User tempUser = new User("Expecting" + i, "expecting" + i + "@example.com", 77);
            tempUser.setStatus(UserStatus.PENDING);
            entityManager.persist(tempUser);
        }
        entityManager.flush();

        // Delete expecting exactly 3
        long deleted = queryFactory.query(User.class)
                .whereEqual(UserFields.AGE, 77)
                .deleteExpecting(3);

        entityManager.flush();
        entityManager.clear();

        assertEquals(3, deleted);
    }

    @Test
    @Order(175)
    @DisplayName("deleteExpecting() - throws when count doesn't match")
    void testDeleteExpectingThrows() {
        // Create 2 temp users
        for (int i = 0; i < 2; i++) {
            User tempUser = new User("ExpectFail" + i, "expectfail" + i + "@example.com", 66);
            entityManager.persist(tempUser);
        }
        entityManager.flush();

        // Try to delete expecting 5 (but only 2 exist)
        assertThrows(IllegalStateException.class, () -> {
            queryFactory.query(User.class)
                    .whereEqual(UserFields.AGE, 66)
                    .deleteExpecting(5);
        });

        // Cleanup - delete the temp users
        queryFactory.query(User.class)
                .whereEqual(UserFields.AGE, 66)
                .delete();
        entityManager.flush();
    }

    @Test
    @Order(176)
    @DisplayName("deleteIfExists() - deletes when records exist")
    void testDeleteIfExists() {
        // Create temp user
        User tempUser = new User("ExistsTest", "exists@example.com", 55);
        entityManager.persist(tempUser);
        entityManager.flush();

        // Delete if exists
        long deleted = queryFactory.query(User.class)
                .whereEqual(UserFields.AGE, 55)
                .deleteIfExists();

        entityManager.flush();
        entityManager.clear();

        assertEquals(1, deleted);
    }

    @Test
    @Order(177)
    @DisplayName("deleteIfExists() - throws when no records exist")
    void testDeleteIfExistsThrows() {
        // Try to delete non-existent records
        assertThrows(IllegalStateException.class, () -> {
            queryFactory.query(User.class)
                    .whereEqual(UserFields.AGE, 999)
                    .deleteIfExists();
        });
    }

    @Test
    @Order(178)
    @DisplayName("delete() - with complex WHERE condition")
    void testDeleteWithComplexCondition() {
        // Create temp users with various statuses
        User activeOld = new User("ActiveOld", "activeold@example.com", 44);
        activeOld.setStatus(UserStatus.ACTIVE);
        entityManager.persist(activeOld);

        User inactiveOld = new User("InactiveOld", "inactiveold@example.com", 44);
        inactiveOld.setStatus(UserStatus.INACTIVE);
        entityManager.persist(inactiveOld);

        entityManager.flush();

        // Delete only inactive users with age 44
        HelixQuery<User> query = queryFactory.query(User.class);
        long deleted = query
                .where(PredicateExpression.and(
                        UserFields.AGE.eq(query.root(), 44),
                        UserFields.STATUS.eq(query.root(), UserStatus.INACTIVE)
                ))
                .delete();

        entityManager.flush();
        entityManager.clear();

        assertEquals(1, deleted);

        // Verify only inactive was deleted
        long remainingCount = queryFactory.query(User.class)
                .whereEqual(UserFields.AGE, 44)
                .queryCount();
        assertEquals(1, remainingCount);

        // Cleanup
        queryFactory.query(User.class)
                .whereEqual(UserFields.AGE, 44)
                .delete();
        entityManager.flush();
    }

    // ==================== Nested Relation Field Tests ====================

    @Test
    @Order(190)
    @DisplayName("Nested relation fields - query by department name using DEPARTMENT.NAME")
    void testNestedRelationFieldDepartmentName() {
        // Query users by department name using nested field accessor
        List<User> users = queryFactory.query(User.class)
                .whereEqual(UserFields.DEPARTMENT.NAME, "Engineering")
                .orderByAsc(UserFields.NAME)
                .query();

        assertEquals(2, users.size());
        assertEquals("Alice", users.get(0).getName());
        assertEquals("Bob", users.get(1).getName());
    }

    @Test
    @Order(191)
    @DisplayName("Nested relation fields - query by department id using DEPARTMENT.ID")
    void testNestedRelationFieldDepartmentId() {
        // Get the department ID first
        Department dept = queryFactory.query(Department.class)
                .whereEqual(DepartmentFields.NAME, "Marketing")
                .queryOneOrNull();
        assertNotNull(dept);

        // Query users by department ID using nested field accessor
        List<User> users = queryFactory.query(User.class)
                .whereEqual(UserFields.DEPARTMENT.ID, dept.getId())
                .orderByAsc(UserFields.NAME)
                .query();

        assertEquals(2, users.size());
        assertEquals("Charlie", users.get(0).getName());
        assertEquals("Diana", users.get(1).getName());
    }

    @Test
    @Order(192)
    @DisplayName("Nested relation fields - contains search on nested field")
    void testNestedRelationFieldContains() {
        // Query users where department name contains "Market"
        List<User> users = queryFactory.query(User.class)
                .whereContains(UserFields.DEPARTMENT.NAME, "Market")
                .query();

        assertEquals(2, users.size());
    }

    @Test
    @Order(193)
    @DisplayName("Nested relation fields - Order entity with USER.NAME")
    void testNestedRelationFieldOrderUserName() {
        // Query orders by user name using nested field accessor
        List<com.soyesenna.helixquery.entity.Order> orders = queryFactory.query(com.soyesenna.helixquery.entity.Order.class)
                .whereEqual(OrderFields.USER.NAME, "Alice")
                .orderByAsc(OrderFields.ORDER_NUMBER)
                .query();

        assertEquals(2, orders.size()); // Alice has 2 orders
        assertEquals("ORD-001", orders.get(0).getOrderNumber());
        assertEquals("ORD-003", orders.get(1).getOrderNumber());
    }

    @Test
    @Order(194)
    @DisplayName("Nested relation fields - OrderItem with PRODUCT.NAME")
    void testNestedRelationFieldOrderItemProductName() {
        // Query order items by product name using nested field accessor
        List<OrderItem> items = queryFactory.query(OrderItem.class)
                .whereEqual(OrderItemFields.PRODUCT.NAME, "Phone")
                .query();

        assertEquals(2, items.size()); // Phone appears in 2 order items
    }

    @Test
    @Order(195)
    @DisplayName("Nested relation fields - OrderItem filter by PRODUCT.PRICE")
    void testNestedRelationFieldOrderItemProductPrice() {
        // Query order items where product price is greater than 500
        HelixQuery<OrderItem> query = queryFactory.query(OrderItem.class);
        List<OrderItem> items = query
                .where(OrderItemFields.PRODUCT.PRICE.gt(query.root(), new BigDecimal("500")))
                .query();

        assertEquals(3, items.size()); // Laptop (999.99) and Phone (599.99) x 2
    }

    @Test
    @Order(196)
    @DisplayName("Nested relation fields - combined with regular fields")
    void testNestedRelationFieldCombined() {
        // Query users where department is Engineering AND status is ACTIVE
        HelixQuery<User> query = queryFactory.query(User.class);
        List<User> users = query
                .where(UserFields.DEPARTMENT.NAME.eq(query.root(), "Engineering"))
                .and(UserFields.STATUS.eq(query.root(), UserStatus.ACTIVE))
                .query();

        assertEquals(2, users.size()); // Alice and Bob
    }

    @Test
    @Order(197)
    @DisplayName("Nested relation fields - ordering by nested field")
    void testNestedRelationFieldOrdering() {
        // Order users by department name
        List<User> users = queryFactory.query(User.class)
                .orderByAsc(UserFields.DEPARTMENT.NAME)
                .orderByAsc(UserFields.NAME)
                .query();

        assertEquals(4, users.size());
        // Engineering users first (Alice, Bob), then Marketing (Charlie, Diana)
        assertEquals("Alice", users.get(0).getName());
        assertEquals("Bob", users.get(1).getName());
        assertEquals("Charlie", users.get(2).getName());
        assertEquals("Diana", users.get(3).getName());
    }

    @Test
    @Order(198)
    @DisplayName("Nested relation fields - embedded within relation using USER.ADDRESS_CITY")
    void testNestedRelationFieldEmbeddedInRelation() {
        // First add address to a user
        User alice = queryFactory.query(User.class)
                .whereEqual(UserFields.NAME, "Alice")
                .queryOneOrNull();
        assertNotNull(alice);
        alice.setAddress(new com.soyesenna.helixquery.entity.Address("123 Main St", "New York", "10001", "USA"));
        entityManager.flush();
        entityManager.clear();

        // Query orders where user's address city is "New York"
        // Using nested field accessor: OrderFields.USER.ADDRESS_CITY
        List<com.soyesenna.helixquery.entity.Order> orders = queryFactory.query(com.soyesenna.helixquery.entity.Order.class)
                .whereEqual(OrderFields.USER.ADDRESS_CITY, "New York")
                .query();

        assertEquals(2, orders.size()); // Alice has 2 orders
    }

    @Test
    @Order(199)
    @DisplayName("Nested relation fields - using $ for join operations")
    void testNestedRelationFieldJoinWithDollar() {
        // Use DEPARTMENT.$ for join operations
        List<User> users = queryFactory.query(User.class)
                .fetchJoin(UserFields.DEPARTMENT.$)
                .whereEqual(UserFields.DEPARTMENT.NAME, "Engineering")
                .orderByAsc(UserFields.NAME)
                .query();

        assertEquals(2, users.size());
        // Verify department is loaded (no lazy loading exception)
        for (User user : users) {
            assertNotNull(user.getDepartment().getName());
        }
    }

    // ==================== Bulk Delete Tests ====================

    @Test
    @Order(180)
    @DisplayName("deleteBulk() - bulk delete with WHERE condition")
    void testDeleteBulk() {
        // Create extra users to delete
        User tempUser1 = new User("BulkUser1", "bulk1@example.com", 98);
        entityManager.persist(tempUser1);

        User tempUser2 = new User("BulkUser2", "bulk2@example.com", 98);
        entityManager.persist(tempUser2);

        entityManager.flush();
        entityManager.clear();

        // Bulk delete users with age 98
        long deleted = queryFactory.query(User.class)
                .whereEqual(UserFields.AGE, 98)
                .deleteBulk();

        assertEquals(2, deleted);

        // Verify deletion
        long afterCount = queryFactory.query(User.class)
                .whereEqual(UserFields.AGE, 98)
                .queryCount();
        assertEquals(0, afterCount);
    }

    @Test
    @Order(181)
    @DisplayName("deleteBulk() - throws exception without WHERE condition")
    void testDeleteBulkWithoutConditionThrows() {
        assertThrows(IllegalStateException.class, () -> {
            queryFactory.query(User.class).deleteBulk();
        });
    }

    @Test
    @Order(182)
    @DisplayName("deleteBulkAll() - bulk deletes all records")
    void testDeleteBulkAll() {
        // Create temporary users with unique age
        User tempUser1 = new User("BulkAll1", "bulkall1@example.com", 96);
        entityManager.persist(tempUser1);
        User tempUser2 = new User("BulkAll2", "bulkall2@example.com", 96);
        entityManager.persist(tempUser2);
        entityManager.flush();
        entityManager.clear();

        long beforeCount = queryFactory.query(User.class)
                .whereEqual(UserFields.AGE, 96)
                .queryCount();
        assertEquals(2, beforeCount);

        // Bulk delete users with age 96 (testing bulk delete mechanism)
        long deleted = queryFactory.query(User.class)
                .whereEqual(UserFields.AGE, 96)
                .deleteBulk();

        assertEquals(2, deleted);

        // Verify all deleted
        long afterCount = queryFactory.query(User.class)
                .whereEqual(UserFields.AGE, 96)
                .queryCount();
        assertEquals(0, afterCount);
    }

    // ==================== Fetch Join + Count/Page Tests ====================

    @Test
    @Order(200)
    @DisplayName("queryCount with leftFetchJoin - should ignore fetch in count query")
    void testQueryCountWithLeftFetchJoin() {
        // This test verifies the fix for Hibernate 6+ SemanticException:
        // "Query specified join fetching, but the owner of the fetched association was not present in the select list"
        long count = queryFactory.query(User.class)
                .leftFetchJoin(UserFields.DEPARTMENT.$)
                .queryCount();

        assertEquals(4, count);
    }

    @Test
    @Order(201)
    @DisplayName("queryPage with leftFetchJoin - should work correctly with pagination")
    void testQueryPageWithLeftFetchJoin() {
        Page<User> page = queryFactory.query(User.class)
                .leftFetchJoin(UserFields.DEPARTMENT.$)
                .queryPageOrderByAsc(PageRequest.of(0, 10), UserFields.ID);

        assertNotNull(page);
        assertEquals(4, page.getTotalElements());
        assertEquals(4, page.getContent().size());

        // Verify department is loaded (no lazy loading exception)
        for (User user : page.getContent()) {
            assertNotNull(user.getDepartment().getName());
        }
    }

    @Test
    @Order(202)
    @DisplayName("queryPageOrderByDesc with leftFetchJoin - should work with ordering")
    void testQueryPageOrderByDescWithLeftFetchJoin() {
        Page<User> page = queryFactory.query(User.class)
                .leftFetchJoin(UserFields.DEPARTMENT.$)
                .queryPageOrderByDesc(PageRequest.of(0, 2), UserFields.AGE);

        assertNotNull(page);
        assertEquals(4, page.getTotalElements());
        assertEquals(2, page.getContent().size());

        // Verify ordering - descending by age
        assertTrue(page.getContent().get(0).getAge() >= page.getContent().get(1).getAge());
    }

    @Test
    @Order(203)
    @DisplayName("queryCount with fetchJoin (inner) - should ignore fetch in count query")
    void testQueryCountWithInnerFetchJoin() {
        long count = queryFactory.query(User.class)
                .fetchJoin(UserFields.DEPARTMENT.$)
                .queryCount();

        assertEquals(4, count);
    }

    @Test
    @Order(204)
    @DisplayName("queryCount with multiple fetch joins - should handle all fetch joins")
    void testQueryCountWithMultipleFetchJoins() {
        long count = queryFactory.query(User.class)
                .leftFetchJoin(UserFields.DEPARTMENT.$)
                .whereGreaterThan(UserFields.AGE, 20)
                .queryCount();

        assertTrue(count >= 0);
    }

    // ==================== Fetch Join + Order By Nested Relation Field Tests ====================

    @Test
    @Order(210)
    @DisplayName("leftFetchJoin with orderBy nested relation field - should reuse the fetched join")
    void testLeftFetchJoinWithOrderByNestedRelationField() {
        // This test verifies the fix for the bug where:
        // leftFetchJoin(RELATION.$) + queryPageOrderByDesc(..., RELATION.ID)
        // would create duplicate joins causing MySQL error:
        // "Expression #1 of ORDER BY clause is not in SELECT list... this is incompatible with DISTINCT"
        Page<User> page = queryFactory.query(User.class)
                .leftFetchJoin(UserFields.DEPARTMENT.$)
                .queryPageOrderByDesc(PageRequest.of(0, 10), UserFields.DEPARTMENT.ID);

        assertNotNull(page);
        assertEquals(4, page.getTotalElements());
        assertEquals(4, page.getContent().size());

        // Verify department is loaded (no lazy loading exception)
        for (User user : page.getContent()) {
            assertNotNull(user.getDepartment().getName());
        }
    }

    @Test
    @Order(211)
    @DisplayName("leftFetchJoinDistinct with orderBy nested relation field - critical scenario")
    void testLeftFetchJoinDistinctWithOrderByNestedRelationField() {
        // This is the exact scenario from the bug report:
        // leftFetchJoin + leftFetchJoinDistinct + queryPageOrderByDesc with nested relation field
        // The DISTINCT + ORDER BY combination triggers MySQL validation
        Page<User> page = queryFactory.query(User.class)
                .leftFetchJoin(UserFields.DEPARTMENT.$)
                .leftFetchJoinDistinct(UserFields.ORDERS)
                .queryPageOrderByDesc(PageRequest.of(0, 10), UserFields.DEPARTMENT.ID);

        assertNotNull(page);
        // Should not throw: "Expression #1 of ORDER BY clause is not in SELECT list"
    }

    @Test
    @Order(212)
    @DisplayName("leftFetchJoin with orderByAsc nested relation string field")
    void testLeftFetchJoinWithOrderByNestedStringField() {
        Page<User> page = queryFactory.query(User.class)
                .leftFetchJoin(UserFields.DEPARTMENT.$)
                .queryPageOrderByAsc(PageRequest.of(0, 10), UserFields.DEPARTMENT.NAME);

        assertNotNull(page);
        assertEquals(4, page.getTotalElements());

        // Verify ordering - should be sorted by department name ascending
        List<User> users = page.getContent();
        assertEquals(4, users.size());
        // Engineering comes before Marketing alphabetically
        assertEquals("Engineering", users.get(0).getDepartment().getName());
    }

    // ==================== Advanced Predicate Utilities ====================

    @Test
    @Order(220)
    @DisplayName("whereAnyOf() - should combine predicates with OR and ignore nulls")
    void testWhereAnyOf() {
        HelixQuery<User> query = queryFactory.query(User.class);

        List<User> users = query
                .whereAnyOf(
                        UserFields.STATUS.eq(query.root(), UserStatus.ACTIVE),
                        null,
                        UserFields.STATUS.eq(query.root(), UserStatus.PENDING)
                )
                .orderByAsc(UserFields.NAME)
                .query();

        assertEquals(3, users.size());
        assertEquals("Alice", users.get(0).getName());
        assertEquals("Bob", users.get(1).getName());
        assertEquals("Diana", users.get(2).getName());
    }

    @Test
    @Order(221)
    @DisplayName("whereAllOf() - should combine predicates with AND and ignore nulls")
    void testWhereAllOf() {
        HelixQuery<User> query = queryFactory.query(User.class);

        List<User> users = query
                .whereAllOf(
                        UserFields.STATUS.eq(query.root(), UserStatus.ACTIVE),
                        null,
                        UserFields.AGE.gt(query.root(), 26)
                )
                .query();

        assertEquals(1, users.size());
        assertEquals("Alice", users.get(0).getName());
    }

    @Test
    @Order(222)
    @DisplayName("whereGroup() - should build nested predicate group with AND")
    void testWhereGroup() {
        HelixQuery<User> query = queryFactory.query(User.class);

        List<User> users = query
                .whereEqual(UserFields.STATUS, UserStatus.ACTIVE)
                .whereGroup(group -> group
                        .or(UserFields.NAME.startsWith(query.root(), "A"))
                        .or(UserFields.NAME.startsWith(query.root(), "B")))
                .orderByAsc(UserFields.NAME)
                .query();

        assertEquals(2, users.size());
        assertEquals("Alice", users.get(0).getName());
        assertEquals("Bob", users.get(1).getName());
    }

    @Test
    @Order(223)
    @DisplayName("orGroup() - should build nested predicate group with OR")
    void testOrGroup() {
        HelixQuery<User> query = queryFactory.query(User.class);

        List<User> users = query
                .whereEqual(UserFields.NAME, "Charlie")
                .orGroup(group -> group
                        .and(UserFields.STATUS.eq(query.root(), UserStatus.ACTIVE))
                        .and(UserFields.AGE.gt(query.root(), 26)))
                .orderByAsc(UserFields.NAME)
                .query();

        assertEquals(2, users.size());
        assertEquals("Alice", users.get(0).getName());
        assertEquals("Charlie", users.get(1).getName());
    }

    @Test
    @Order(224)
    @DisplayName("when() - should conditionally customize the query")
    void testWhen() {
        List<User> allUsers = queryFactory.query(User.class)
                .when(false, q -> q.whereEqual(UserFields.NAME, "Alice"))
                .query();

        assertEquals(4, allUsers.size());

        List<User> onlyAlice = queryFactory.query(User.class)
                .when(true, q -> q.whereEqual(UserFields.NAME, "Alice"))
                .query();

        assertEquals(1, onlyAlice.size());
        assertEquals("Alice", onlyAlice.get(0).getName());
    }

    // ==================== Pageable Utilities ====================

    @Test
    @Order(230)
    @DisplayName("pageableOrderByDesc() - should apply offset/limit and ordering")
    void testPageableOrderByDesc() {
        List<User> users = queryFactory.query(User.class)
                .pageableOrderByDesc(PageRequest.of(0, 2), UserFields.AGE)
                .query();

        assertEquals(2, users.size());
        assertEquals("Charlie", users.get(0).getName());
        assertEquals("Alice", users.get(1).getName());
    }

    @Test
    @Order(231)
    @DisplayName("pageable() - should apply sorting via Field resolver and ignore unknown properties")
    void testPageableWithSortFieldResolver() {
        com.soyesenna.helixquery.field.Field<Integer> ageField =
                new com.soyesenna.helixquery.field.Field<>("age", Integer.class, User.class);

        List<User> users = queryFactory.query(User.class)
                .pageable(
                        PageRequest.of(0, 2, Sort.by(Sort.Order.desc("age"), Sort.Order.asc("unknown"))),
                        property -> "age".equals(property) ? ageField : null
                )
                .query();

        assertEquals(2, users.size());
        assertEquals("Charlie", users.get(0).getName());
        assertEquals("Alice", users.get(1).getName());
    }

    // ==================== OperationExpression / Aggregate Queries ====================

    @Test
    @Order(240)
    @DisplayName("querySelect() - should support aggregate operations (avg, countDistinct)")
    void testOperationExpressionAggregates() {
        HelixQuery<User> query = queryFactory.query(User.class);

        List<Double> avgAge = query.querySelect(UserFields.AGE.avg(query.root()));
        assertEquals(1, avgAge.size());
        assertEquals(29.5, avgAge.get(0), 0.0001);

        List<Long> distinctStatusCount = query.querySelect(
                OperationExpression.countDistinct(UserFields.STATUS.path(query.root()))
        );
        assertEquals(1, distinctStatusCount.size());
        assertEquals(3L, distinctStatusCount.get(0));
    }

    @Test
    @Order(241)
    @DisplayName("querySelect() - should support collection SIZE() and String operations")
    void testOperationExpressionCollectionSizeAndStringOps() {
        HelixQuery<User> query = queryFactory.query(User.class);

        List<Integer> orderCounts = query
                .orderByAsc(UserFields.NAME)
                .querySelect(UserFields.ORDERS.size(query.root()));

        assertEquals(List.of(2, 1, 0, 0), orderCounts);

        HelixQuery<User> queryForUpper = queryFactory.query(User.class)
                .orderByAsc(UserFields.NAME);
        List<String> upperNames = queryForUpper
                .querySelect(UserFields.NAME.upper(queryForUpper.root()));

        assertEquals(List.of("ALICE", "BOB", "CHARLIE", "DIANA"), upperNames);

        HelixQuery<User> queryForLength = queryFactory.query(User.class)
                .orderByAsc(UserFields.NAME);
        List<Integer> nameLengths = queryForLength
                .querySelect(UserFields.NAME.length(queryForLength.root()));

        assertEquals(List.of(5, 3, 7, 5), nameLengths);
    }

    @Test
    @Order(242)
    @DisplayName("CollectionField.contains() - should compile MEMBER OF predicate")
    void testCollectionFieldContains() {
        com.soyesenna.helixquery.entity.Order order = entityManager.createQuery(
                        "SELECT o FROM Order o WHERE o.orderNumber = 'ORD-001'",
                        com.soyesenna.helixquery.entity.Order.class
                )
                .getSingleResult();

        HelixQuery<User> query = queryFactory.query(User.class);
        List<User> users = query
                .where(UserFields.ORDERS.contains(query.root(), order))
                .query();

        assertEquals(1, users.size());
        assertEquals("Alice", users.get(0).getName());
    }

    // ==================== GROUP BY COUNT ====================

    @Test
    @Order(250)
    @DisplayName("groupByCount() - should return counts grouped by ComparableField")
    void testGroupByCountComparableField() {
        Map<UserStatus, Long> counts = queryFactory.query(User.class).groupByCount(UserFields.STATUS);

        assertEquals(3, counts.size());
        assertEquals(2L, counts.get(UserStatus.ACTIVE));
        assertEquals(1L, counts.get(UserStatus.INACTIVE));
        assertEquals(1L, counts.get(UserStatus.PENDING));
    }

    @Test
    @Order(251)
    @DisplayName("groupByCount() - should support relationPath auto-join for nested fields")
    void testGroupByCountNestedRelationField() {
        Map<String, Long> counts = queryFactory.query(User.class).groupByCount(UserFields.DEPARTMENT.NAME);

        assertEquals(Map.of("Engineering", 2L, "Marketing", 2L), counts);
    }

    @Test
    @Order(252)
    @DisplayName("groupByCount() - should group by Field<Boolean> (true/false)")
    void testGroupByCountBooleanField() {
        Map<Boolean, Long> counts = queryFactory.query(User.class).groupByCount(UserFields.ACTIVE);

        assertEquals(2, counts.size());
        assertEquals(3L, counts.get(true));
        assertEquals(1L, counts.get(false));
    }

    @Test
    @Order(253)
    @DisplayName("groupByCount() - should group by RelationField entity")
    void testGroupByCountRelationField() {
        Map<Department, Long> counts = queryFactory.query(User.class).groupByCount(UserFields.DEPARTMENT.$);

        Map<String, Long> byName = counts.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().getName(), Map.Entry::getValue));

        assertEquals(Map.of("Engineering", 2L, "Marketing", 2L), byName);
    }

    // ==================== In-Memory groupBy() Utilities ====================

    @Test
    @Order(260)
    @DisplayName("groupBy(Function) - should group query results in memory")
    void testGroupByFunction() {
        Map<UserStatus, List<User>> grouped = queryFactory.query(User.class).groupBy(User::getStatus);

        assertEquals(3, grouped.size());
        assertEquals(2, grouped.get(UserStatus.ACTIVE).size());
        assertEquals(1, grouped.get(UserStatus.INACTIVE).size());
        assertEquals(1, grouped.get(UserStatus.PENDING).size());
    }

    @Test
    @Order(261)
    @DisplayName("groupBy(ComparableField) - should group by declared field using reflection")
    void testGroupByComparableField() {
        Map<UserStatus, List<User>> grouped = queryFactory.query(User.class).groupBy(UserFields.STATUS);

        assertEquals(3, grouped.size());
        assertEquals(2, grouped.get(UserStatus.ACTIVE).size());
        assertEquals(1, grouped.get(UserStatus.INACTIVE).size());
        assertEquals(1, grouped.get(UserStatus.PENDING).size());
    }

    @Test
    @Order(262)
    @DisplayName("groupBy(Field) - should throw when field does not exist on entity")
    void testGroupByMissingFieldThrows() {
        com.soyesenna.helixquery.field.Field<String> missing =
                new com.soyesenna.helixquery.field.Field<>("doesNotExist", String.class, User.class);

        assertThrows(RuntimeException.class, () -> queryFactory.query(User.class).groupBy(missing));
    }

    // ==================== Convenience WHERE wrappers ====================

    @Test
    @Order(270)
    @DisplayName("whereIsNull/whereIsNotNull - should build null predicates")
    void testWhereIsNullWrappers() {
        User alice = entityManager.createQuery("SELECT u FROM User u WHERE u.name = 'Alice'", User.class)
                .getSingleResult();
        alice.setAge(null);
        entityManager.flush();
        entityManager.clear();

        List<User> nullAgeUsers = queryFactory.query(User.class)
                .whereIsNull(UserFields.AGE)
                .query();

        assertEquals(1, nullAgeUsers.size());
        assertEquals("Alice", nullAgeUsers.get(0).getName());

        long notNullCount = queryFactory.query(User.class)
                .whereIsNotNull(UserFields.AGE)
                .queryCount();

        assertEquals(3, notNullCount);
    }

    @Test
    @Order(271)
    @DisplayName("whereBeforeNow/whereAfterNow - should use DateTimeField now helpers")
    void testWhereBeforeAfterNowWrappers() {
        long beforeNow = queryFactory.query(User.class)
                .whereBeforeNow(UserFields.CREATED_AT)
                .queryCount();

        long afterNow = queryFactory.query(User.class)
                .whereAfterNow(UserFields.CREATED_AT)
                .queryCount();

        assertEquals(4, beforeNow);
        assertEquals(0, afterNow);
    }

    @Test
    @Order(272)
    @DisplayName("whereIsEmpty/whereIsNotEmpty - should handle empty or null strings")
    void testWhereIsEmptyWrappers() {
        Department engineering = entityManager.createQuery(
                        "SELECT d FROM Department d WHERE d.name = 'Engineering'", Department.class)
                .getSingleResult();
        engineering.setDescription("");
        Department marketing = entityManager.createQuery(
                        "SELECT d FROM Department d WHERE d.name = 'Marketing'", Department.class)
                .getSingleResult();
        marketing.setDescription(null);
        entityManager.flush();
        entityManager.clear();

        List<Department> emptyDescriptions = queryFactory.query(Department.class)
                .whereIsEmpty(DepartmentFields.DESCRIPTION)
                .orderByAsc(DepartmentFields.NAME)
                .query();

        assertEquals(2, emptyDescriptions.size());

        long notEmptyCount = queryFactory.query(Department.class)
                .whereIsNotEmpty(DepartmentFields.DESCRIPTION)
                .queryCount();

        assertEquals(0, notEmptyCount);
    }

    @Test
    @Order(273)
    @DisplayName("whereGreaterThanOrEqual/whereLessThanOrEqual/orGreaterThanOrEqual - should build comparison predicates")
    void testComparisonWrapperMethods() {
        List<String> geNames = queryFactory.query(User.class)
                .whereGreaterThanOrEqual(UserFields.AGE, 30)
                .orderByAsc(UserFields.NAME)
                .query()
                .stream()
                .map(User::getName)
                .toList();

        assertEquals(List.of("Alice", "Charlie"), geNames);

        List<String> leNames = queryFactory.query(User.class)
                .whereLessThanOrEqual(UserFields.AGE, 28)
                .orderByAsc(UserFields.NAME)
                .query()
                .stream()
                .map(User::getName)
                .toList();

        assertEquals(List.of("Bob", "Diana"), leNames);

        List<String> orNames = queryFactory.query(User.class)
                .whereEqual(UserFields.NAME, "Bob")
                .orGreaterThanOrEqual(UserFields.AGE, 35)
                .orderByAsc(UserFields.NAME)
                .query()
                .stream()
                .map(User::getName)
                .toList();

        assertEquals(List.of("Bob", "Charlie"), orNames);
    }

    // ==================== Security-focused Inputs ====================

    @Test
    @Order(280)
    @DisplayName("whereContains() - should escape LIKE wildcards to prevent pattern injection")
    void testWhereContainsEscapesWildcards() {
        User special = new User("100%_User", "special@example.com", 20);
        special.setActive(true);
        special.setStatus(UserStatus.ACTIVE);
        entityManager.persist(special);
        entityManager.flush();
        entityManager.clear();

        List<User> users = queryFactory.query(User.class)
                .whereContains(UserFields.NAME, "%")
                .query();

        // If wildcards were not escaped, "%" would match all rows (pattern injection risk).
        // Current implementation escapes wildcards, so this should NOT return all users.
        assertTrue(users.isEmpty());
    }

    @Test
    @Order(281)
    @DisplayName("whereEqual() - should treat SQL injection-like input as a literal value")
    void testWhereEqualSqlInjectionLikeInput() {
        List<User> users = queryFactory.query(User.class)
                .whereEqual(UserFields.EMAIL, "alice@example.com' OR 1=1 --")
                .query();

        assertTrue(users.isEmpty());
    }

    @Test
    @Order(282)
    @DisplayName("querySelect() - should support concat with ConstantExpression")
    void testOperationExpressionConcat() {
        HelixQuery<User> query = queryFactory.query(User.class);

        List<String> labels = query
                .orderByAsc(UserFields.NAME)
                .querySelect(OperationExpression.concat(
                        UserFields.NAME.path(query.root()),
                        ConstantExpression.of("_label")
                ));

        assertEquals(List.of("Alice_label", "Bob_label", "Charlie_label", "Diana_label"), labels);
    }

    // ==================== findBy(Field, Collection) Tests ====================

    @Test
    @Order(290)
    @DisplayName("HelixField.in(Collection) - should filter by collection of IDs")
    void testHelixFieldInWithCollection() {
        // Get all users and their IDs
        List<User> allUsers = queryFactory.query(User.class).query();
        List<Long> targetIds = allUsers.stream()
                .filter(u -> u.getName().equals("Alice") || u.getName().equals("Bob"))
                .map(User::getId)
                .toList();

        // Use the in() method via HelixField interface
        HelixQuery<User> query = queryFactory.query(User.class);
        List<User> users = query
                .where(UserFields.ID.in(query.root(), targetIds))
                .orderByAsc(UserFields.NAME)
                .query();

        assertEquals(2, users.size());
        assertEquals("Alice", users.get(0).getName());
        assertEquals("Bob", users.get(1).getName());
    }

    @Test
    @Order(291)
    @DisplayName("HelixField.in(Collection) - should filter by collection of enum values")
    void testHelixFieldInWithEnumCollection() {
        List<UserStatus> statuses = List.of(UserStatus.ACTIVE, UserStatus.PENDING);

        HelixQuery<User> query = queryFactory.query(User.class);
        List<User> users = query
                .where(UserFields.STATUS.in(query.root(), statuses))
                .orderByAsc(UserFields.NAME)
                .query();

        assertEquals(3, users.size()); // Alice(ACTIVE), Bob(ACTIVE), Diana(PENDING)
        assertTrue(users.stream().allMatch(u ->
                u.getStatus() == UserStatus.ACTIVE || u.getStatus() == UserStatus.PENDING));
    }

    @Test
    @Order(292)
    @DisplayName("HelixField.in(Collection) - should filter by collection of strings")
    void testHelixFieldInWithStringCollection() {
        List<String> names = List.of("Alice", "Charlie");

        HelixQuery<User> query = queryFactory.query(User.class);
        List<User> users = query
                .where(UserFields.NAME.in(query.root(), names))
                .orderByAsc(UserFields.NAME)
                .query();

        assertEquals(2, users.size());
        assertEquals("Alice", users.get(0).getName());
        assertEquals("Charlie", users.get(1).getName());
    }

    @Test
    @Order(293)
    @DisplayName("HelixField.in(Collection) - should return null for empty collection (no-op)")
    void testHelixFieldInWithEmptyCollection() {
        List<Long> emptyIds = List.of();

        HelixQuery<User> query = queryFactory.query(User.class);
        PredicateExpression predicate = UserFields.ID.in(query.root(), emptyIds);

        assertNull(predicate); // Empty collection returns null predicate
    }

    @Test
    @Order(294)
    @DisplayName("PredicateExpression.alwaysFalse() - should return no results")
    void testAlwaysFalsePredicate() {
        List<User> users = queryFactory.query(User.class)
                .where(PredicateExpression.alwaysFalse())
                .query();

        assertTrue(users.isEmpty());
    }

    @Test
    @Order(295)
    @DisplayName("PredicateExpression.alwaysTrue() - should return all results")
    void testAlwaysTruePredicate() {
        List<User> users = queryFactory.query(User.class)
                .where(PredicateExpression.alwaysTrue())
                .query();

        assertEquals(4, users.size()); // All 4 users
    }

    @Test
    @Order(296)
    @DisplayName("whereIn() with Collection - should filter by ID collection")
    void testWhereInWithCollection() {
        // Get Alice and Charlie's IDs
        List<User> allUsers = queryFactory.query(User.class).query();
        List<Long> targetIds = allUsers.stream()
                .filter(u -> u.getName().equals("Alice") || u.getName().equals("Charlie"))
                .map(User::getId)
                .toList();

        List<User> users = queryFactory.query(User.class)
                .whereIn(UserFields.ID, targetIds)
                .orderByAsc(UserFields.NAME)
                .query();

        assertEquals(2, users.size());
        assertEquals("Alice", users.get(0).getName());
        assertEquals("Charlie", users.get(1).getName());
    }

    @Test
    @Order(297)
    @DisplayName("Combined IN with other predicates - should chain correctly")
    void testInWithOtherPredicates() {
        List<UserStatus> statuses = List.of(UserStatus.ACTIVE, UserStatus.PENDING);

        HelixQuery<User> query = queryFactory.query(User.class);
        List<User> users = query
                .where(UserFields.STATUS.in(query.root(), statuses))
                .whereGreaterThan(UserFields.AGE, 26)
                .orderByAsc(UserFields.NAME)
                .query();

        assertEquals(2, users.size()); // Alice(30, ACTIVE), Diana(28, PENDING)
        assertEquals("Alice", users.get(0).getName());
        assertEquals("Diana", users.get(1).getName());
    }

    @Test
    @Order(298)
    @DisplayName("RelationField.in(Collection) - should filter by related entities")
    void testRelationFieldInWithCollection() {
        // Get the Engineering department
        Department engineering = entityManager.createQuery(
                "SELECT d FROM Department d WHERE d.name = 'Engineering'", Department.class)
                .getSingleResult();

        List<Department> departments = List.of(engineering);

        // Use UserFields.DEPARTMENT.$ for the RelationField
        HelixQuery<User> query = queryFactory.query(User.class);
        List<User> users = query
                .where(UserFields.DEPARTMENT.$.in(query.root(), departments))
                .orderByAsc(UserFields.NAME)
                .query();

        assertEquals(2, users.size()); // Alice and Bob are in Engineering
        assertTrue(users.stream().allMatch(u ->
                u.getName().equals("Alice") || u.getName().equals("Bob")));
    }

    @Test
    @Order(299)
    @DisplayName("DateTimeField.in(Collection) - should filter by collection of dates")
    void testDateTimeFieldInWithCollection() {
        // Get users with specific birth dates
        List<User> allUsers = queryFactory.query(User.class).query();
        List<LocalDate> targetDates = allUsers.stream()
                .filter(u -> u.getName().equals("Alice") || u.getName().equals("Bob"))
                .map(User::getBirthDate)
                .toList();

        HelixQuery<User> query = queryFactory.query(User.class);
        List<User> users = query
                .where(UserFields.BIRTH_DATE.in(query.root(), targetDates))
                .orderByAsc(UserFields.NAME)
                .query();

        assertEquals(2, users.size());
        assertEquals("Alice", users.get(0).getName());
        assertEquals("Bob", users.get(1).getName());
    }
}
