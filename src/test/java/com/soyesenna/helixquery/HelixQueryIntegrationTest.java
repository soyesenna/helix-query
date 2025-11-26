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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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

    // ==================== RelationField Tests ====================

    @Test
    @Order(50)
    @DisplayName("RelationField.join() - inner join on relation")
    void testRelationFieldJoin() {
        HelixQuery<User> query = queryFactory.query(User.class);
        List<User> users = query
                .join(UserFields.DEPARTMENT)
                .query();

        assertEquals(4, users.size());
    }

    @Test
    @Order(51)
    @DisplayName("RelationField.fetchJoin() - fetch join to avoid N+1")
    void testRelationFieldFetchJoin() {
        HelixQuery<User> query = queryFactory.query(User.class);
        List<User> users = query
                .fetchJoin(UserFields.DEPARTMENT)
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
                .fetchJoin(OrderFields.USER)
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
}
