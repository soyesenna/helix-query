package com.soyesenna.helixquery;

import com.soyesenna.helixquery.expression.ConstructorExpression;
import com.soyesenna.helixquery.expression.CriteriaContext;
import com.soyesenna.helixquery.expression.CriteriaExpressionVisitor;
import com.soyesenna.helixquery.expression.PathExpression;
import com.soyesenna.helixquery.expression.PredicateExpression;
import com.soyesenna.helixquery.field.*;
import com.soyesenna.helixquery.order.OrderSpecifier;
import com.soyesenna.helixquery.predicate.PredicateBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Main fluent query builder replacing QueryChain.
 * Provides type-safe query construction using the HelixQuery expression system
 * and compiles to JPA Criteria API at execution time.
 *
 * @param <T> the entity type being queried
 */
public class HelixQuery<T> {

    private static final Logger log = LoggerFactory.getLogger(HelixQuery.class);

    private final EntityManager entityManager;
    private final Class<T> entityClass;
    private final PathExpression<T> root;
    private final PredicateBuilder predicateBuilder;
    private final List<OrderSpecifier> orders;
    private final List<JoinSpec> joins;
    private final List<com.soyesenna.helixquery.expression.Expression<?>> groupByExpressions;
    private PredicateExpression havingPredicate;
    private Long offset;
    private Long limit;
    private boolean distinct;

    /**
     * Create a new query for the given entity class.
     *
     * @param entityManager the JPA entity manager
     * @param entityClass   the entity class to query
     */
    public HelixQuery(EntityManager entityManager, Class<T> entityClass) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager must not be null");
        this.entityClass = Objects.requireNonNull(entityClass, "entityClass must not be null");
        this.root = new PathExpression<>(entityClass);
        this.predicateBuilder = new PredicateBuilder();
        this.orders = new ArrayList<>();
        this.joins = new ArrayList<>();
        this.groupByExpressions = new ArrayList<>();
    }

    // ==================== Root Access ====================

    /**
     * Get the root path expression for this query.
     * Used to build expressions referencing entity fields.
     *
     * @return the root path expression
     */
    public PathExpression<T> root() {
        return root;
    }

    // ==================== WHERE Clause ====================

    /**
     * Add a WHERE condition.
     *
     * @param predicate the predicate to add
     * @return this query for chaining
     */
    public HelixQuery<T> where(PredicateExpression predicate) {
        predicateBuilder.and(predicate);
        return this;
    }

    /**
     * Add a WHERE condition with AND.
     *
     * @param predicate the predicate to add
     * @return this query for chaining
     */
    public HelixQuery<T> and(PredicateExpression predicate) {
        predicateBuilder.and(predicate);
        return this;
    }

    /**
     * Add a WHERE condition with OR.
     *
     * @param predicate the predicate to add
     * @return this query for chaining
     */
    public HelixQuery<T> or(PredicateExpression predicate) {
        predicateBuilder.or(predicate);
        return this;
    }

    // ==================== Complex Condition Utilities ====================

    /**
     * Add a WHERE condition that combines multiple predicates with OR.
     * Useful for complex OR conditions in a single call.
     *
     * <pre>{@code
     * // Find users with ACTIVE or PENDING status
     * List<User> users = queryFactory.query(User.class)
     *     .whereAnyOf(
     *         UserFields.STATUS.eq(root(), UserStatus.ACTIVE),
     *         UserFields.STATUS.eq(root(), UserStatus.PENDING)
     *     )
     *     .query();
     * }</pre>
     *
     * @param predicates the predicates to combine with OR
     * @return this query for chaining
     */
    public HelixQuery<T> whereAnyOf(PredicateExpression... predicates) {
        PredicateExpression combined = PredicateExpression.or(predicates);
        if (combined != null) {
            predicateBuilder.and(combined);
        }
        return this;
    }

    /**
     * Add a WHERE condition that combines multiple predicates with AND.
     * Useful for grouping AND conditions explicitly.
     *
     * <pre>{@code
     * // Find users where (status = ACTIVE AND age > 18)
     * List<User> users = queryFactory.query(User.class)
     *     .whereAllOf(
     *         UserFields.STATUS.eq(root(), UserStatus.ACTIVE),
     *         UserFields.AGE.gt(root(), 18)
     *     )
     *     .query();
     * }</pre>
     *
     * @param predicates the predicates to combine with AND
     * @return this query for chaining
     */
    public HelixQuery<T> whereAllOf(PredicateExpression... predicates) {
        PredicateExpression combined = PredicateExpression.and(predicates);
        if (combined != null) {
            predicateBuilder.and(combined);
        }
        return this;
    }

    /**
     * Add a complex WHERE condition using a builder pattern.
     * Allows building nested AND/OR conditions.
     *
     * <pre>{@code
     * // Find users where status = ACTIVE AND (role = ADMIN OR role = MANAGER)
     * List<User> users = queryFactory.query(User.class)
     *     .whereEqual(UserFields.STATUS, UserStatus.ACTIVE)
     *     .whereGroup(group -> group
     *         .or(UserFields.ROLE.eq(root(), Role.ADMIN))
     *         .or(UserFields.ROLE.eq(root(), Role.MANAGER)))
     *     .query();
     * }</pre>
     *
     * @param groupBuilder consumer to build the nested predicate group
     * @return this query for chaining
     */
    public HelixQuery<T> whereGroup(Consumer<PredicateBuilder> groupBuilder) {
        PredicateBuilder nested = new PredicateBuilder();
        groupBuilder.accept(nested);
        if (nested.hasValue()) {
            predicateBuilder.and(nested.build());
        }
        return this;
    }

    /**
     * Add an OR condition using a builder pattern.
     * Adds the built group with OR to the existing conditions.
     *
     * <pre>{@code
     * // Find users where name = 'John' OR (status = ACTIVE AND age > 18)
     * List<User> users = queryFactory.query(User.class)
     *     .whereEqual(UserFields.NAME, "John")
     *     .orGroup(group -> group
     *         .and(UserFields.STATUS.eq(root(), UserStatus.ACTIVE))
     *         .and(UserFields.AGE.gt(root(), 18)))
     *     .query();
     * }</pre>
     *
     * @param groupBuilder consumer to build the nested predicate group
     * @return this query for chaining
     */
    public HelixQuery<T> orGroup(Consumer<PredicateBuilder> groupBuilder) {
        PredicateBuilder nested = new PredicateBuilder();
        groupBuilder.accept(nested);
        if (nested.hasValue()) {
            predicateBuilder.or(nested.build());
        }
        return this;
    }

    // ==================== Field-Based WHERE Conditions ====================

    /**
     * Add equality condition using unified HelixField interface.
     * This method works with all field types: Field, StringField, NumberField,
     * ComparableField, DateTimeField, RelationField.
     *
     * @param field the field to compare
     * @param value the value to match
     * @param <V>   the field value type
     * @return this query for chaining
     */
    public <V> HelixQuery<T> whereEqual(HelixField<V> field, V value) {
        if (value != null) {
            predicateBuilder.and(field.eq(root, value));
        }
        return this;
    }

    /**
     * Add greater-than condition: field > value
     */
    public <V extends Comparable<? super V>> HelixQuery<T> whereGreaterThan(ComparableField<V> field, V value) {
        if (value != null) {
            predicateBuilder.and(field.gt(root, value));
        }
        return this;
    }

    /**
     * Add greater-than condition for number fields.
     */
    public <V extends Number & Comparable<V>> HelixQuery<T> whereGreaterThan(NumberField<V> field, V value) {
        if (value != null) {
            predicateBuilder.and(field.gt(root, value));
        }
        return this;
    }

    /**
     * Add less-than condition: field < value
     */
    public <V extends Comparable<? super V>> HelixQuery<T> whereLessThan(ComparableField<V> field, V value) {
        if (value != null) {
            predicateBuilder.and(field.lt(root, value));
        }
        return this;
    }

    /**
     * Add less-than condition for number fields.
     */
    public <V extends Number & Comparable<V>> HelixQuery<T> whereLessThan(NumberField<V> field, V value) {
        if (value != null) {
            predicateBuilder.and(field.lt(root, value));
        }
        return this;
    }

    /**
     * Add greater-than-or-equal condition: field >= value
     */
    public <V extends Comparable<? super V>> HelixQuery<T> whereGreaterThanOrEqual(ComparableField<V> field, V value) {
        if (value != null) {
            predicateBuilder.and(field.ge(root, value));
        }
        return this;
    }

    /**
     * Add greater-than-or-equal condition for number fields.
     */
    public <V extends Number & Comparable<V>> HelixQuery<T> whereGreaterThanOrEqual(NumberField<V> field, V value) {
        if (value != null) {
            predicateBuilder.and(field.ge(root, value));
        }
        return this;
    }

    /**
     * Add less-than-or-equal condition: field <= value
     */
    public <V extends Comparable<? super V>> HelixQuery<T> whereLessThanOrEqual(ComparableField<V> field, V value) {
        if (value != null) {
            predicateBuilder.and(field.le(root, value));
        }
        return this;
    }

    /**
     * Add less-than-or-equal condition for number fields.
     */
    public <V extends Number & Comparable<V>> HelixQuery<T> whereLessThanOrEqual(NumberField<V> field, V value) {
        if (value != null) {
            predicateBuilder.and(field.le(root, value));
        }
        return this;
    }

    /**
     * Add greater-than condition for datetime fields: field > value
     *
     * <pre>{@code
     * // Find contests where application hasn't started yet
     * LocalDateTime now = LocalDateTime.now();
     * List<Contest> upcoming = queryFactory.query(Contest.class)
     *     .whereGreaterThan(ContestFields.APPLICATION_START_AT, now)
     *     .query();
     * }</pre>
     *
     * @param field the datetime field to compare
     * @param value the value to compare against
     * @param <V>   the temporal type
     * @return this query for chaining
     */
    public <V extends java.time.temporal.Temporal & Comparable<? super V>> HelixQuery<T> whereGreaterThan(DateTimeField<V> field, V value) {
        if (value != null) {
            predicateBuilder.and(field.after(root, value));
        }
        return this;
    }

    /**
     * Add less-than condition for datetime fields: field < value
     *
     * <pre>{@code
     * // Find contests that have ended
     * LocalDateTime now = LocalDateTime.now();
     * List<Contest> ended = queryFactory.query(Contest.class)
     *     .whereLessThan(ContestFields.PROGRESS_END_AT, now)
     *     .query();
     * }</pre>
     *
     * @param field the datetime field to compare
     * @param value the value to compare against
     * @param <V>   the temporal type
     * @return this query for chaining
     */
    public <V extends java.time.temporal.Temporal & Comparable<? super V>> HelixQuery<T> whereLessThan(DateTimeField<V> field, V value) {
        if (value != null) {
            predicateBuilder.and(field.before(root, value));
        }
        return this;
    }

    /**
     * Add greater-than-or-equal condition for datetime fields: field >= value
     */
    public <V extends java.time.temporal.Temporal & Comparable<? super V>> HelixQuery<T> whereGreaterThanOrEqual(DateTimeField<V> field, V value) {
        if (value != null) {
            predicateBuilder.and(field.onOrAfter(root, value));
        }
        return this;
    }

    /**
     * Add less-than-or-equal condition for datetime fields: field <= value
     */
    public <V extends java.time.temporal.Temporal & Comparable<? super V>> HelixQuery<T> whereLessThanOrEqual(DateTimeField<V> field, V value) {
        if (value != null) {
            predicateBuilder.and(field.onOrBefore(root, value));
        }
        return this;
    }

    /**
     * Add LIKE condition: field LIKE pattern
     */
    public HelixQuery<T> whereLike(StringField field, String pattern) {
        if (pattern != null) {
            predicateBuilder.and(field.like(root, pattern));
        }
        return this;
    }

    /**
     * Add contains condition: field LIKE '%value%'
     */
    public HelixQuery<T> whereContains(StringField field, String value) {
        if (value != null && !value.isEmpty()) {
            predicateBuilder.and(field.contains(root, value));
        }
        return this;
    }

    /**
     * Add IN condition: field IN (values)
     */
    public <V> HelixQuery<T> whereIn(Field<V> field, Collection<? extends V> values) {
        if (values != null && !values.isEmpty()) {
            predicateBuilder.and(field.in(root, values));
        }
        return this;
    }

    /**
     * Add IN condition for string fields: field IN (values)
     */
    public HelixQuery<T> whereIn(StringField field, Collection<String> values) {
        if (values != null && !values.isEmpty()) {
            predicateBuilder.and(field.in(root, values));
        }
        return this;
    }

    /**
     * Add IN condition for number fields: field IN (values)
     */
    public <V extends Number & Comparable<V>> HelixQuery<T> whereIn(NumberField<V> field, Collection<? extends V> values) {
        if (values != null && !values.isEmpty()) {
            predicateBuilder.and(field.in(root, values));
        }
        return this;
    }

    /**
     * Add IN condition for comparable fields: field IN (values)
     */
    public <V extends Comparable<? super V>> HelixQuery<T> whereIn(ComparableField<V> field, Collection<? extends V> values) {
        if (values != null && !values.isEmpty()) {
            predicateBuilder.and(field.in(root, values));
        }
        return this;
    }

    // ==================== Field-Based NULL Conditions ====================

    /**
     * Add IS NULL condition using unified HelixField interface.
     * This method works with all field types: Field, StringField, NumberField,
     * ComparableField, DateTimeField, RelationField.
     *
     * <pre>{@code
     * // Find users with no assigned department
     * List<User> users = queryFactory.query(User.class)
     *     .whereIsNull(UserFields.DEPARTMENT)
     *     .query();
     * }</pre>
     *
     * @param field the field to check for null
     * @return this query for chaining
     */
    public HelixQuery<T> whereIsNull(HelixField<?> field) {
        predicateBuilder.and(field.isNull(root));
        return this;
    }

    /**
     * Add IS NOT NULL condition using unified HelixField interface.
     * This method works with all field types: Field, StringField, NumberField,
     * ComparableField, DateTimeField, RelationField.
     *
     * <pre>{@code
     * // Find users with assigned department
     * List<User> users = queryFactory.query(User.class)
     *     .whereIsNotNull(UserFields.DEPARTMENT)
     *     .query();
     * }</pre>
     *
     * @param field the field to check for not null
     * @return this query for chaining
     */
    public HelixQuery<T> whereIsNotNull(HelixField<?> field) {
        predicateBuilder.and(field.isNotNull(root));
        return this;
    }

    /**
     * Add IS EMPTY condition for StringField: field = '' OR field IS NULL
     *
     * <pre>{@code
     * // Find users with empty or null nickname
     * List<User> users = queryFactory.query(User.class)
     *     .whereIsEmpty(UserFields.NICKNAME)
     *     .query();
     * }</pre>
     *
     * @param field the string field to check
     * @return this query for chaining
     */
    public HelixQuery<T> whereIsEmpty(StringField field) {
        predicateBuilder.and(field.isEmpty(root));
        return this;
    }

    /**
     * Add IS NOT EMPTY condition for StringField: field != '' AND field IS NOT NULL
     *
     * <pre>{@code
     * // Find users with non-empty nickname
     * List<User> users = queryFactory.query(User.class)
     *     .whereIsNotEmpty(UserFields.NICKNAME)
     *     .query();
     * }</pre>
     *
     * @param field the string field to check
     * @return this query for chaining
     */
    public HelixQuery<T> whereIsNotEmpty(StringField field) {
        predicateBuilder.and(field.isNotEmpty(root));
        return this;
    }

    /**
     * Add before-now condition for DateTime fields: field < now()
     */
    public HelixQuery<T> whereBeforeNow(DateTimeField<LocalDateTime> field) {
        predicateBuilder.and(field.beforeNow(root));
        return this;
    }

    /**
     * Add after-now condition for DateTime fields: field > now()
     */
    public HelixQuery<T> whereAfterNow(DateTimeField<LocalDateTime> field) {
        predicateBuilder.and(field.afterNow(root));
        return this;
    }

    // ==================== Field-Based OR Conditions ====================

    /**
     * Add OR equality condition using unified HelixField interface.
     * This method works with all field types.
     *
     * <pre>{@code
     * // Find users with name 'John' OR status ACTIVE
     * List<User> users = queryFactory.query(User.class)
     *     .whereEqual(UserFields.NAME, "John")
     *     .orEqual(UserFields.STATUS, UserStatus.ACTIVE)
     *     .query();
     * }</pre>
     *
     * @param field the field to compare
     * @param value the value to match
     * @param <V>   the field value type
     * @return this query for chaining
     */
    public <V> HelixQuery<T> orEqual(HelixField<V> field, V value) {
        if (value != null) {
            predicateBuilder.or(field.eq(root, value));
        }
        return this;
    }

    /**
     * Add OR greater-than condition: field > value
     */
    public <V extends Comparable<? super V>> HelixQuery<T> orGreaterThan(ComparableField<V> field, V value) {
        if (value != null) {
            predicateBuilder.or(field.gt(root, value));
        }
        return this;
    }

    /**
     * Add OR greater-than condition for number fields.
     */
    public <V extends Number & Comparable<V>> HelixQuery<T> orGreaterThan(NumberField<V> field, V value) {
        if (value != null) {
            predicateBuilder.or(field.gt(root, value));
        }
        return this;
    }

    /**
     * Add OR less-than condition: field < value
     */
    public <V extends Comparable<? super V>> HelixQuery<T> orLessThan(ComparableField<V> field, V value) {
        if (value != null) {
            predicateBuilder.or(field.lt(root, value));
        }
        return this;
    }

    /**
     * Add OR less-than condition for number fields.
     */
    public <V extends Number & Comparable<V>> HelixQuery<T> orLessThan(NumberField<V> field, V value) {
        if (value != null) {
            predicateBuilder.or(field.lt(root, value));
        }
        return this;
    }

    /**
     * Add OR greater-than-or-equal condition: field >= value
     */
    public <V extends Comparable<? super V>> HelixQuery<T> orGreaterThanOrEqual(ComparableField<V> field, V value) {
        if (value != null) {
            predicateBuilder.or(field.ge(root, value));
        }
        return this;
    }

    /**
     * Add OR greater-than-or-equal condition for number fields.
     */
    public <V extends Number & Comparable<V>> HelixQuery<T> orGreaterThanOrEqual(NumberField<V> field, V value) {
        if (value != null) {
            predicateBuilder.or(field.ge(root, value));
        }
        return this;
    }

    /**
     * Add OR less-than-or-equal condition: field <= value
     */
    public <V extends Comparable<? super V>> HelixQuery<T> orLessThanOrEqual(ComparableField<V> field, V value) {
        if (value != null) {
            predicateBuilder.or(field.le(root, value));
        }
        return this;
    }

    /**
     * Add OR less-than-or-equal condition for number fields.
     */
    public <V extends Number & Comparable<V>> HelixQuery<T> orLessThanOrEqual(NumberField<V> field, V value) {
        if (value != null) {
            predicateBuilder.or(field.le(root, value));
        }
        return this;
    }

    /**
     * Add OR greater-than condition for datetime fields: field > value
     */
    public <V extends java.time.temporal.Temporal & Comparable<? super V>> HelixQuery<T> orGreaterThan(DateTimeField<V> field, V value) {
        if (value != null) {
            predicateBuilder.or(field.after(root, value));
        }
        return this;
    }

    /**
     * Add OR less-than condition for datetime fields: field < value
     */
    public <V extends java.time.temporal.Temporal & Comparable<? super V>> HelixQuery<T> orLessThan(DateTimeField<V> field, V value) {
        if (value != null) {
            predicateBuilder.or(field.before(root, value));
        }
        return this;
    }

    /**
     * Add OR greater-than-or-equal condition for datetime fields: field >= value
     */
    public <V extends java.time.temporal.Temporal & Comparable<? super V>> HelixQuery<T> orGreaterThanOrEqual(DateTimeField<V> field, V value) {
        if (value != null) {
            predicateBuilder.or(field.onOrAfter(root, value));
        }
        return this;
    }

    /**
     * Add OR less-than-or-equal condition for datetime fields: field <= value
     */
    public <V extends java.time.temporal.Temporal & Comparable<? super V>> HelixQuery<T> orLessThanOrEqual(DateTimeField<V> field, V value) {
        if (value != null) {
            predicateBuilder.or(field.onOrBefore(root, value));
        }
        return this;
    }

    /**
     * Add OR LIKE condition: field LIKE pattern
     */
    public HelixQuery<T> orLike(StringField field, String pattern) {
        if (pattern != null) {
            predicateBuilder.or(field.like(root, pattern));
        }
        return this;
    }

    /**
     * Add OR contains condition: field LIKE '%value%'
     */
    public HelixQuery<T> orContains(StringField field, String value) {
        if (value != null && !value.isEmpty()) {
            predicateBuilder.or(field.contains(root, value));
        }
        return this;
    }

    /**
     * Add OR IN condition: field IN (values)
     */
    public <V> HelixQuery<T> orIn(Field<V> field, Collection<? extends V> values) {
        if (values != null && !values.isEmpty()) {
            predicateBuilder.or(field.in(root, values));
        }
        return this;
    }

    /**
     * Add OR IN condition for string fields: field IN (values)
     */
    public HelixQuery<T> orIn(StringField field, Collection<String> values) {
        if (values != null && !values.isEmpty()) {
            predicateBuilder.or(field.in(root, values));
        }
        return this;
    }

    /**
     * Add OR IN condition for number fields: field IN (values)
     */
    public <V extends Number & Comparable<V>> HelixQuery<T> orIn(NumberField<V> field, Collection<? extends V> values) {
        if (values != null && !values.isEmpty()) {
            predicateBuilder.or(field.in(root, values));
        }
        return this;
    }

    /**
     * Add OR IN condition for comparable fields: field IN (values)
     */
    public <V extends Comparable<? super V>> HelixQuery<T> orIn(ComparableField<V> field, Collection<? extends V> values) {
        if (values != null && !values.isEmpty()) {
            predicateBuilder.or(field.in(root, values));
        }
        return this;
    }

    /**
     * Add OR IS NULL condition.
     *
     * <pre>{@code
     * // Find users with name 'John' OR department is null
     * List<User> users = queryFactory.query(User.class)
     *     .whereEqual(UserFields.NAME, "John")
     *     .orIsNull(UserFields.DEPARTMENT)
     *     .query();
     * }</pre>
     *
     * @param field the field to check for null
     * @return this query for chaining
     */
    public HelixQuery<T> orIsNull(HelixField<?> field) {
        predicateBuilder.or(field.isNull(root));
        return this;
    }

    /**
     * Add OR IS NOT NULL condition.
     *
     * @param field the field to check for not null
     * @return this query for chaining
     */
    public HelixQuery<T> orIsNotNull(HelixField<?> field) {
        predicateBuilder.or(field.isNotNull(root));
        return this;
    }

    /**
     * Add OR IS EMPTY condition for StringField: field = '' OR field IS NULL
     *
     * @param field the string field to check
     * @return this query for chaining
     */
    public HelixQuery<T> orIsEmpty(StringField field) {
        predicateBuilder.or(field.isEmpty(root));
        return this;
    }

    /**
     * Add OR IS NOT EMPTY condition for StringField: field != '' AND field IS NOT NULL
     *
     * @param field the string field to check
     * @return this query for chaining
     */
    public HelixQuery<T> orIsNotEmpty(StringField field) {
        predicateBuilder.or(field.isNotEmpty(root));
        return this;
    }

    /**
     * Add OR before-now condition for DateTime fields: field < now()
     */
    public HelixQuery<T> orBeforeNow(DateTimeField<LocalDateTime> field) {
        predicateBuilder.or(field.beforeNow(root));
        return this;
    }

    /**
     * Add OR after-now condition for DateTime fields: field > now()
     */
    public HelixQuery<T> orAfterNow(DateTimeField<LocalDateTime> field) {
        predicateBuilder.or(field.afterNow(root));
        return this;
    }

    // ==================== ORDER BY ====================

    /**
     * Add ordering specification.
     *
     * @param specifiers the order specifiers
     * @return this query for chaining
     */
    public HelixQuery<T> orderBy(OrderSpecifier... specifiers) {
        orders.addAll(Arrays.asList(specifiers));
        return this;
    }

    /**
     * Add ascending order by field.
     */
    public <V> HelixQuery<T> orderByAsc(Field<V> field) {
        orders.add(field.asc(root));
        return this;
    }

    /**
     * Add descending order by field.
     */
    public <V> HelixQuery<T> orderByDesc(Field<V> field) {
        orders.add(field.desc(root));
        return this;
    }

    /**
     * Add ascending order by string field.
     */
    public HelixQuery<T> orderByAsc(StringField field) {
        orders.add(field.asc(root));
        return this;
    }

    /**
     * Add descending order by string field.
     */
    public HelixQuery<T> orderByDesc(StringField field) {
        orders.add(field.desc(root));
        return this;
    }

    /**
     * Add ascending order by number field.
     */
    public <V extends Number & Comparable<V>> HelixQuery<T> orderByAsc(NumberField<V> field) {
        orders.add(field.asc(root));
        return this;
    }

    /**
     * Add descending order by number field.
     */
    public <V extends Number & Comparable<V>> HelixQuery<T> orderByDesc(NumberField<V> field) {
        orders.add(field.desc(root));
        return this;
    }

    /**
     * Add ascending order by datetime field.
     */
    public <V extends java.time.temporal.Temporal & Comparable<? super V>> HelixQuery<T> orderByAsc(DateTimeField<V> field) {
        orders.add(field.asc(root));
        return this;
    }

    /**
     * Add descending order by datetime field.
     */
    public <V extends java.time.temporal.Temporal & Comparable<? super V>> HelixQuery<T> orderByDesc(DateTimeField<V> field) {
        orders.add(field.desc(root));
        return this;
    }

    // ==================== PAGINATION ====================

    /**
     * Set the maximum number of results.
     *
     * @param limit the limit
     * @return this query for chaining
     */
    public HelixQuery<T> limit(long limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Set the offset (number of results to skip).
     *
     * @param offset the offset
     * @return this query for chaining
     */
    public HelixQuery<T> offset(long offset) {
        this.offset = offset;
        return this;
    }

    /**
     * Apply Spring Pageable for pagination and sorting.
     *
     * @param pageable          the pageable
     * @param sortFieldResolver function to resolve sort property names to fields
     * @return this query for chaining
     */
    public HelixQuery<T> pageable(Pageable pageable, Function<String, Field<?>> sortFieldResolver) {
        if (pageable == null) {
            return this;
        }

        this.offset = pageable.getOffset();
        this.limit = (long) pageable.getPageSize();

        for (Sort.Order order : pageable.getSort()) {
            Field<?> field = sortFieldResolver.apply(order.getProperty());
            if (field != null) {
                if (order.isAscending()) {
                    orders.add(field.asc(root));
                } else {
                    orders.add(field.desc(root));
                }
            }
        }
        return this;
    }

    /**
     * Apply Spring Pageable with a specific field for ordering.
     *
     * <pre>{@code
     * // Sort by embedded field with ASC
     * contestService.find()
     *     .pageableOrderByAsc(pageable, ContestFields.APPLICATION_PERIOD_APPLICATION_START_AT)
     *     .query();
     * }</pre>
     *
     * @param pageable  the pageable (offset and limit are applied)
     * @param field     the field to order by
     * @param ascending true for ASC, false for DESC
     * @return this query for chaining
     */
    public HelixQuery<T> pageableOrderBy(Pageable pageable, HelixField<?> field, boolean ascending) {
        if (pageable == null) {
            return this;
        }

        this.offset = pageable.getOffset();
        this.limit = (long) pageable.getPageSize();

        if (field != null) {
            if (ascending) {
                orders.add(field.asc(root));
            } else {
                orders.add(field.desc(root));
            }
        }
        return this;
    }

    /**
     * Apply Spring Pageable with ascending order by the specified field.
     *
     * <pre>{@code
     * contestService.find()
     *     .pageableOrderByAsc(pageable, ContestFields.APPLICATION_PERIOD_APPLICATION_START_AT)
     *     .query();
     * }</pre>
     *
     * @param pageable the pageable (offset and limit are applied)
     * @param field    the field to order by ascending
     * @return this query for chaining
     */
    public HelixQuery<T> pageableOrderByAsc(Pageable pageable, HelixField<?> field) {
        return pageableOrderBy(pageable, field, true);
    }

    /**
     * Apply Spring Pageable with descending order by the specified field.
     *
     * <pre>{@code
     * contestService.find()
     *     .pageableOrderByDesc(pageable, ContestFields.CREATED_AT)
     *     .query();
     * }</pre>
     *
     * @param pageable the pageable (offset and limit are applied)
     * @param field    the field to order by descending
     * @return this query for chaining
     */
    public HelixQuery<T> pageableOrderByDesc(Pageable pageable, HelixField<?> field) {
        return pageableOrderBy(pageable, field, false);
    }

    // ==================== JOINS ====================

    /**
     * Add an inner join.
     *
     * @param relation the relation field to join
     * @return this query for chaining
     */
    public <J> HelixQuery<T> join(RelationField<J> relation) {
        joins.add(new JoinSpec(relation.name(), JoinType.INNER, false));
        return this;
    }

    /**
     * Add a left join.
     *
     * @param relation the relation field to join
     * @return this query for chaining
     */
    public <J> HelixQuery<T> leftJoin(RelationField<J> relation) {
        joins.add(new JoinSpec(relation.name(), JoinType.LEFT, false));
        return this;
    }

    /**
     * Add a fetch join (eager loading).
     *
     * @param relation the relation field to fetch join
     * @return this query for chaining
     */
    public <J> HelixQuery<T> fetchJoin(RelationField<J> relation) {
        joins.add(new JoinSpec(relation.name(), JoinType.INNER, true));
        return this;
    }

    /**
     * Add a left fetch join (eager loading).
     *
     * @param relation the relation field to fetch join
     * @return this query for chaining
     */
    public <J> HelixQuery<T> leftFetchJoin(RelationField<J> relation) {
        joins.add(new JoinSpec(relation.name(), JoinType.LEFT, true));
        return this;
    }

    /**
     * Add a left fetch join for collection fields (eager loading).
     *
     * <pre>{@code
     * // Fetch join a OneToMany collection
     * List<TicketTemplate> templates = queryFactory.query(TicketTemplate.class)
     *     .leftFetchJoin(TicketTemplateFields.TICKET_OPTIONS)
     *     .distinct()
     *     .query();
     * }</pre>
     *
     * @param collection the collection field to fetch join
     * @return this query for chaining
     */
    public <E> HelixQuery<T> leftFetchJoin(CollectionField<E> collection) {
        joins.add(new JoinSpec(collection.name(), JoinType.LEFT, true));
        return this;
    }

    public <E> HelixQuery<T> leftFetchJoinDistinct(CollectionField<E> collection) {
        joins.add(new JoinSpec(collection.name(), JoinType.LEFT, true));
        this.distinct = true;
        return this;
    }

    /**
     * Add a fetch join for collection fields (eager loading).
     *
     * @param collection the collection field to fetch join
     * @return this query for chaining
     */
    public <E> HelixQuery<T> fetchJoin(CollectionField<E> collection) {
        joins.add(new JoinSpec(collection.name(), JoinType.INNER, true));
        return this;
    }

    // ==================== GROUP BY / HAVING ====================

    /**
     * Add GROUP BY expressions.
     *
     * @param expressions the expressions to group by
     * @return this query for chaining
     */
    public HelixQuery<T> groupBy(com.soyesenna.helixquery.expression.Expression<?>... expressions) {
        groupByExpressions.addAll(Arrays.asList(expressions));
        return this;
    }

    /**
     * Add HAVING condition.
     *
     * @param predicate the having predicate
     * @return this query for chaining
     */
    public HelixQuery<T> having(PredicateExpression predicate) {
        this.havingPredicate = predicate;
        return this;
    }

    // ==================== DISTINCT ====================

    /**
     * Set query to return distinct results.
     *
     * @return this query for chaining
     */
    public HelixQuery<T> distinct() {
        this.distinct = true;
        return this;
    }

    // ==================== CONDITIONAL APPLICATION ====================

    /**
     * Conditionally apply modifications to this query.
     *
     * @param condition  if true, apply the customizer
     * @param customizer the function to apply
     * @return this query for chaining
     */
    public HelixQuery<T> when(boolean condition, Consumer<HelixQuery<T>> customizer) {
        if (condition) {
            customizer.accept(this);
        }
        return this;
    }

    // ==================== EXECUTION - LIST ====================

    /**
     * Execute the query and return all results.
     *
     * @return the list of results
     */
    public List<T> query() {
        if (requiresTwoPhaseQuery()) {
            return buildQueryWithTwoPhase();
        }
        return buildQuery().getResultList();
    }

    /**
     * Execute the query and return all results (alias for query()).
     *
     * @return the list of results
     */
    public List<T> list() {
        return query();
    }

    // ==================== EXECUTION - PAGE ====================

    /**
     * Execute the query and return results as a Spring Data Page.
     * Uses the current offset/limit settings for pagination.
     *
     * <pre>{@code
     * Page<Contest> page = contestService.find()
     *     .whereEqual(ContestFields.VISIBLE, true)
     *     .pageableOrderByAsc(pageable, ContestFields.CREATED_AT)
     *     .queryPage();
     * }</pre>
     *
     * @return a Page containing the results
     */
    public Page<T> queryPage() {
        long total = queryCount();
        List<T> content = query();

        // Create a Pageable from current offset/limit
        int page = (offset != null && limit != null && limit > 0) ? (int) (offset / limit) : 0;
        int size = (limit != null && limit > 0) ? limit.intValue() : content.size();

        return new PageImpl<>(content, org.springframework.data.domain.PageRequest.of(page, Math.max(size, 1)), total);
    }

    /**
     * Execute the query with Pageable and return results as a Spring Data Page.
     *
     * <pre>{@code
     * Page<Contest> page = contestService.find()
     *     .whereEqual(ContestFields.VISIBLE, true)
     *     .queryPage(pageable, sortProperty -> switch (sortProperty) {
     *         case "createdAt" -> ContestFields.CREATED_AT;
     *         default -> ContestFields.ID;
     *     });
     * }</pre>
     *
     * @param pageable          the pageable for pagination and sorting
     * @param sortFieldResolver function to resolve sort property names to fields
     * @return a Page containing the results
     */
    public Page<T> queryPage(Pageable pageable, Function<String, Field<?>> sortFieldResolver) {
        pageable(pageable, sortFieldResolver);
        long total = queryCount();
        List<T> content = query();
        return new PageImpl<>(content, pageable, total);
    }

    /**
     * Execute the query with Pageable and specific sort field, return as Page.
     *
     * <pre>{@code
     * Page<Contest> page = contestService.find()
     *     .whereEqual(ContestFields.VISIBLE, true)
     *     .queryPage(pageable, ContestFields.APPLICATION_PERIOD_APPLICATION_START_AT, true);
     * }</pre>
     *
     * @param pageable  the pageable for pagination
     * @param field     the field to order by
     * @param ascending true for ASC, false for DESC
     * @return a Page containing the results
     */
    public Page<T> queryPage(Pageable pageable, HelixField<?> field, boolean ascending) {
        pageableOrderBy(pageable, field, ascending);
        long total = queryCount();
        List<T> content = query();
        return new PageImpl<>(content, pageable, total);
    }

    /**
     * Execute the query with Pageable and ascending sort, return as Page.
     *
     * <pre>{@code
     * Page<Contest> page = contestService.find()
     *     .whereEqual(ContestFields.VISIBLE, true)
     *     .queryPageOrderByAsc(pageable, ContestFields.APPLICATION_PERIOD_APPLICATION_START_AT);
     * }</pre>
     *
     * @param pageable the pageable for pagination
     * @param field    the field to order by ascending
     * @return a Page containing the results
     */
    public Page<T> queryPageOrderByAsc(Pageable pageable, HelixField<?> field) {
        return queryPage(pageable, field, true);
    }

    /**
     * Execute the query with Pageable and descending sort, return as Page.
     *
     * <pre>{@code
     * Page<Contest> page = contestService.find()
     *     .whereEqual(ContestFields.VISIBLE, true)
     *     .queryPageOrderByDesc(pageable, ContestFields.CREATED_AT);
     * }</pre>
     *
     * @param pageable the pageable for pagination
     * @param field    the field to order by descending
     * @return a Page containing the results
     */
    public Page<T> queryPageOrderByDesc(Pageable pageable, HelixField<?> field) {
        return queryPage(pageable, field, false);
    }

    // ==================== EXECUTION - SINGLE ====================

    /**
     * Execute the query expecting exactly one result.
     *
     * @return Optional containing the result, or empty if no result
     * @throws IllegalStateException if more than one result
     */
    public Optional<T> queryOne() {
        List<T> results;
        if (hasCollectionFetch()) {
            this.limit = 2L;
            results = buildQueryWithTwoPhase();
            this.limit = null;
        } else {
            TypedQuery<T> typedQuery = buildQuery();
            typedQuery.setMaxResults(2);
            results = typedQuery.getResultList();
        }
        if (results.size() > 1) {
            throw new IllegalStateException("Expected at most one result but found " + results.size());
        }
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Execute the query expecting at most one result.
     *
     * @return the result or null if no result
     * @throws IllegalStateException if more than one result
     */
    public T queryOneOrNull() {
        return queryOne().orElse(null);
    }

    /**
     * Execute the query and return the first result.
     *
     * @return the first result or null if no results
     */
    public T queryFirstOrNull() {
        if (hasCollectionFetch()) {
            this.limit = 1L;
            List<T> results = buildQueryWithTwoPhase();
            this.limit = null;
            return results.isEmpty() ? null : results.get(0);
        }
        TypedQuery<T> typedQuery = buildQuery();
        typedQuery.setMaxResults(1);
        try {
            return typedQuery.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Execute the query and return the first result.
     *
     * @return Optional containing the first result
     */
    public Optional<T> queryFirst() {
        return Optional.ofNullable(queryFirstOrNull());
    }

    // ==================== EXECUTION - COUNT ====================

    /**
     * Execute a count query.
     *
     * @return the count of matching results
     */
    public long queryCount() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<T> countRoot = countQuery.from(entityClass);

        boolean useDistinctCount = distinct || hasCollectionFetch();
        countQuery.select(useDistinctCount ? cb.countDistinct(countRoot) : cb.count(countRoot));

        CriteriaContext ctx = new CriteriaContext(cb, countRoot, countQuery);
        // Use applyJoinsForCount to exclude fetch joins from count query
        // Fetch joins cause SemanticException in Hibernate 6+ when owner is not in select list
        applyJoinsForCount(ctx, countRoot);

        if (predicateBuilder.hasValue()) {
            CriteriaExpressionVisitor visitor = new CriteriaExpressionVisitor();
            Predicate predicate = visitor.compilePredicate(predicateBuilder.build(), ctx);
            countQuery.where(predicate);
        }

        return entityManager.createQuery(countQuery).getSingleResult();
    }

    /**
     * Check if any results exist.
     *
     * @return true if at least one result exists
     */
    public boolean exists() {
        return queryFirstOrNull() != null;
    }

    // ==================== EXECUTION - DELETE ====================

    /**
     * Delete entities matching the current WHERE conditions using managed persistence context.
     * This method properly integrates with JPA lifecycle callbacks and cascade operations.
     *
     * <pre>{@code
     * // Delete all inactive users
     * long deleted = queryFactory.query(User.class)
     *     .whereEqual(UserFields.STATUS, UserStatus.INACTIVE)
     *     .delete();
     *
     * // Delete users older than a certain date
     * long deleted = queryFactory.query(User.class)
     *     .where(UserFields.CREATED_AT.before(root(), cutoffDate))
     *     .delete();
     * }</pre>
     *
     * <p><b>Features:</b>
     * <ul>
     *   <li>Triggers JPA lifecycle callbacks (@PreRemove, @PostRemove)</li>
     *   <li>Respects cascade settings on relationships</li>
     *   <li>Updates persistence context properly</li>
     *   <li>Requires an active transaction</li>
     * </ul>
     *
     * <p>For bulk delete without persistence context management (faster but bypasses
     * lifecycle callbacks), use {@link #deleteBulk()}.
     *
     * @return the number of entities deleted
     * @throws IllegalStateException if no WHERE conditions are specified (use deleteAll() for that)
     */
    public long delete() {
        if (!predicateBuilder.hasValue()) {
            throw new IllegalStateException(
                    "Cannot execute delete() without WHERE conditions. " +
                    "Use deleteAll() if you intend to delete all records.");
        }
        return executeManagedDelete();
    }

    /**
     * Delete entities and immediately flush to database.
     * This ensures DELETE SQL is executed immediately before subsequent operations.
     *
     * <pre>{@code
     * // Delete expired records and ensure it's committed before checking existence
     * service.findBy(Fields.EMAIL, email)
     *     .whereBeforeNow(Fields.EXPIRED_AT)
     *     .deleteAndFlush();
     *
     * // Now safely check for existing records
     * boolean exists = service.findBy(Fields.EMAIL, email)
     *     .whereAfterNow(Fields.EXPIRED_AT)
     *     .exists();
     * }</pre>
     *
     * <p>Use this when you need to ensure DELETE is executed before subsequent
     * queries or inserts to avoid issues with Hibernate's default flush order
     * (INSERT → UPDATE → DELETE).
     *
     * @return the number of entities deleted
     * @throws IllegalStateException if no WHERE conditions are specified
     */
    public long deleteAndFlush() {
        long count = delete();
        entityManager.flush();
        return count;
    }

    /**
     * Delete ALL entities of this type using managed persistence context.
     *
     * <pre>{@code
     * // Delete all temporary records
     * long deleted = queryFactory.query(TempData.class).deleteAll();
     * }</pre>
     *
     * <p><b>Warning:</b> This will delete ALL records. Use with extreme caution.
     *
     * @return the number of entities deleted
     */
    public long deleteAll() {
        return executeManagedDelete();
    }

    /**
     * Delete entities with count validation.
     * Only proceeds if the count matches the expected number.
     *
     * <pre>{@code
     * // Delete exactly 5 expired sessions
     * long deleted = queryFactory.query(Session.class)
     *     .where(SessionFields.EXPIRED_AT.before(root(), now))
     *     .deleteExpecting(5);
     * }</pre>
     *
     * @param expectedCount the expected number of records to delete
     * @return the number of entities deleted
     * @throws IllegalStateException if the actual count doesn't match the expected count
     */
    public long deleteExpecting(long expectedCount) {
        long count = queryCount();
        if (count != expectedCount) {
            throw new IllegalStateException(
                    "Expected to delete " + expectedCount + " records but found " + count);
        }
        return executeManagedDelete();
    }

    /**
     * Delete entities only if at least one record matches.
     *
     * @return the number of entities deleted
     * @throws IllegalStateException if no records match the conditions
     */
    public long deleteIfExists() {
        if (!exists()) {
            throw new IllegalStateException("No records found matching the delete conditions");
        }
        return executeManagedDelete();
    }

    /**
     * Delete entities and return the deleted entities list.
     * Useful for logging or auditing deleted data.
     *
     * <pre>{@code
     * // Get deleted users for audit log
     * List<User> deletedUsers = queryFactory.query(User.class)
     *     .whereEqual(UserFields.STATUS, UserStatus.DELETED)
     *     .deleteAndReturn();
     * auditService.logDeletion(deletedUsers);
     * }</pre>
     *
     * @return the list of deleted entities
     */
    public List<T> deleteAndReturn() {
        List<T> toDelete = query();
        for (T entity : toDelete) {
            entityManager.remove(entity);
        }
        return toDelete;
    }

    /**
     * Execute managed delete through EntityManager.remove().
     * This properly integrates with JPA lifecycle and persistence context.
     */
    private long executeManagedDelete() {
        List<T> entities = query();
        for (T entity : entities) {
            entityManager.remove(entity);
        }
        return entities.size();
    }

    // ==================== EXECUTION - BULK DELETE ====================

    /**
     * Execute a bulk delete query with the current WHERE conditions.
     * This executes a JPQL/SQL DELETE statement directly in the database,
     * bypassing the persistence context for better performance.
     *
     * <pre>{@code
     * // Bulk delete for performance-critical operations
     * long deleted = queryFactory.query(LogEntry.class)
     *     .where(LogFields.CREATED_AT.before(root(), cutoffDate))
     *     .deleteBulk();
     * }</pre>
     *
     * <p><b>Important:</b> This operation:
     * <ul>
     *   <li>Does NOT trigger JPA lifecycle callbacks (@PreRemove, @PostRemove)</li>
     *   <li>Does NOT cascade to related entities</li>
     *   <li>Does NOT update the persistence context</li>
     *   <li>Is significantly faster for large datasets</li>
     * </ul>
     *
     * <p>For managed delete with lifecycle support, use {@link #delete()}.
     *
     * @return the number of entities deleted
     * @throws IllegalStateException if no WHERE conditions are specified
     */
    public long deleteBulk() {
        if (!predicateBuilder.hasValue()) {
            throw new IllegalStateException(
                    "Cannot execute deleteBulk() without WHERE conditions. " +
                    "Use deleteBulkAll() if you intend to delete all records.");
        }
        return executeBulkDelete();
    }

    /**
     * Execute a bulk delete query without WHERE conditions.
     * Deletes ALL records of this entity type directly in the database.
     *
     * <p><b>Warning:</b> This bypasses persistence context and lifecycle callbacks.
     *
     * @return the number of entities deleted
     */
    public long deleteBulkAll() {
        return executeBulkDelete();
    }

    /**
     * Execute bulk delete using CriteriaDelete.
     * Bypasses persistence context for better performance.
     */
    private long executeBulkDelete() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaDelete<T> deleteQuery = cb.createCriteriaDelete(entityClass);
        Root<T> deleteRoot = deleteQuery.from(entityClass);

        // Apply WHERE clause if present
        if (predicateBuilder.hasValue()) {
            CriteriaContext ctx = new CriteriaContext(cb, deleteRoot, null);
            CriteriaExpressionVisitor visitor = new CriteriaExpressionVisitor();
            Predicate predicate = visitor.compilePredicate(predicateBuilder.build(), ctx);
            deleteQuery.where(predicate);
        }

        return entityManager.createQuery(deleteQuery).executeUpdate();
    }

    // ==================== EXECUTION - PROJECTIONS ====================

    /**
     * Execute the query with a constructor projection.
     *
     * @param constructor the constructor expression
     * @param <R>         the result type
     * @return the list of projected results
     */
    @SuppressWarnings("unchecked")
    public <R> List<R> queryAs(ConstructorExpression<R> constructor) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<R> cq = cb.createQuery((Class<R>) constructor.getType());
        Root<T> criteriaRoot = cq.from(entityClass);

        CriteriaContext ctx = new CriteriaContext(cb, criteriaRoot, cq);
        CriteriaExpressionVisitor visitor = new CriteriaExpressionVisitor();

        applyJoins(ctx, criteriaRoot);

        // Build constructor selection
        List<Selection<?>> selections = new ArrayList<>();
        for (com.soyesenna.helixquery.expression.Expression<?> arg : constructor.getArguments()) {
            selections.add(visitor.compile(arg, ctx));
        }
        cq.select(cb.construct(constructor.getType(), selections.toArray(new Selection[0])));

        if (distinct) cq.distinct(true);
        applyWhereClause(ctx, visitor, cq);
        applyOrderBy(ctx, visitor, cq, cb);

        TypedQuery<R> typedQuery = entityManager.createQuery(cq);
        applyPagination(typedQuery);

        return typedQuery.getResultList();
    }

    /**
     * Execute the query with a single expression projection.
     *
     * @param selection the expression to select
     * @param <R>       the result type
     * @return the list of projected results
     */
    public <R> List<R> querySelect(com.soyesenna.helixquery.expression.Expression<R> selection) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        @SuppressWarnings("unchecked")
        CriteriaQuery<R> cq = cb.createQuery((Class<R>) selection.getType());
        Root<T> criteriaRoot = cq.from(entityClass);

        CriteriaContext ctx = new CriteriaContext(cb, criteriaRoot, cq);
        CriteriaExpressionVisitor visitor = new CriteriaExpressionVisitor();

        applyJoins(ctx, criteriaRoot);

        cq.select((jakarta.persistence.criteria.Selection<R>) visitor.compile(selection, ctx));

        if (distinct) cq.distinct(true);
        applyWhereClause(ctx, visitor, cq);
        applyOrderBy(ctx, visitor, cq, cb);

        TypedQuery<R> typedQuery = entityManager.createQuery(cq);
        applyPagination(typedQuery);

        return typedQuery.getResultList();
    }

    /**
     * Execute the query with tuple projection.
     *
     * @param selections the expressions to select
     * @return the list of tuple results
     */
    public List<Tuple> queryTuple(com.soyesenna.helixquery.expression.Expression<?>... selections) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<T> criteriaRoot = cq.from(entityClass);

        CriteriaContext ctx = new CriteriaContext(cb, criteriaRoot, cq);
        CriteriaExpressionVisitor visitor = new CriteriaExpressionVisitor();

        applyJoins(ctx, criteriaRoot);

        List<Selection<?>> selectionList = new ArrayList<>();
        for (com.soyesenna.helixquery.expression.Expression<?> expr : selections) {
            selectionList.add(visitor.compile(expr, ctx));
        }
        cq.multiselect(selectionList);

        if (distinct) cq.distinct(true);
        applyWhereClause(ctx, visitor, cq);
        applyOrderBy(ctx, visitor, cq, cb);

        TypedQuery<Tuple> typedQuery = entityManager.createQuery(cq);
        applyPagination(typedQuery);

        return typedQuery.getResultList();
    }

    // ==================== EXECUTION - GROUP BY COUNT ====================

    /**
     * Execute a GROUP BY query and return counts per group as a Map.
     * Generates: SELECT field, COUNT(*) FROM entity WHERE ... GROUP BY field
     *
     * <pre>{@code
     * // Count users by status
     * Map<UserStatus, Long> statusCounts = queryFactory.query(User.class)
     *     .groupByCount(UserFields.STATUS);
     *
     * // Count with additional conditions
     * Map<UserStatus, Long> activeCounts = queryFactory.query(User.class)
     *     .whereEqual(UserFields.ACTIVE, true)
     *     .groupByCount(UserFields.STATUS);
     * }</pre>
     *
     * @param field the field to group by
     * @param <V>   the field value type
     * @return a Map where keys are field values and values are counts
     */
    public <V> Map<V, Long> groupByCount(Field<V> field) {
        return executeGroupByCount(field.path(root), field.type());
    }

    /**
     * Execute a GROUP BY query for StringField and return counts per group.
     *
     * <pre>{@code
     * Map<String, Long> categoryCounts = queryFactory.query(Product.class)
     *     .groupByCount(ProductFields.CATEGORY);
     * }</pre>
     *
     * @param field the string field to group by
     * @return a Map where keys are string values and values are counts
     */
    public Map<String, Long> groupByCount(StringField field) {
        return executeGroupByCount(field.path(root), String.class);
    }

    /**
     * Execute a GROUP BY query for NumberField and return counts per group.
     *
     * <pre>{@code
     * Map<Integer, Long> priceTierCounts = queryFactory.query(Product.class)
     *     .groupByCount(ProductFields.PRICE_TIER);
     * }</pre>
     *
     * @param field the number field to group by
     * @param <V>   the number type
     * @return a Map where keys are number values and values are counts
     */
    public <V extends Number & Comparable<V>> Map<V, Long> groupByCount(NumberField<V> field) {
        return executeGroupByCount(field.path(root), field.type());
    }

    /**
     * Execute a GROUP BY query for ComparableField and return counts per group.
     *
     * <pre>{@code
     * Map<Priority, Long> priorityCounts = queryFactory.query(Task.class)
     *     .groupByCount(TaskFields.PRIORITY);
     * }</pre>
     *
     * @param field the comparable field to group by
     * @param <V>   the comparable type
     * @return a Map where keys are field values and values are counts
     */
    public <V extends Comparable<? super V>> Map<V, Long> groupByCount(ComparableField<V> field) {
        return executeGroupByCount(field.path(root), field.type());
    }

    /**
     * Execute a GROUP BY query for RelationField and return counts per group.
     * Groups by the related entity (ManyToOne, OneToOne relationships).
     *
     * <pre>{@code
     * // Count tickets per contest
     * Map<Contest, Long> ticketCountByContest = queryFactory.query(Ticket.class)
     *     .groupByCount(TicketFields.CONTEST);
     *
     * // With additional conditions
     * Map<Contest, Long> paidTicketsByContest = queryFactory.query(Ticket.class)
     *     .whereEqual(TicketFields.PAYMENT_STATUS, PaymentStatus.PAID)
     *     .groupByCount(TicketFields.CONTEST);
     * }</pre>
     *
     * @param field the relation field to group by
     * @param <R>   the related entity type
     * @return a Map where keys are related entities and values are counts
     */
    public <R> Map<R, Long> groupByCount(RelationField<R> field) {
        return executeGroupByCount(field.path(root), field.targetType());
    }

    // ==================== EXECUTION - GROUP BY TO LIST ====================

    /**
     * Execute the query and group results by a key extractor function.
     * Returns a Map where keys are extracted values and values are lists of matching entities.
     *
     * <pre>{@code
     * // Group tickets by contest
     * Map<Contest, List<Ticket>> ticketsByContest = queryFactory.query(Ticket.class)
     *     .whereEqual(TicketFields.PAYMENT_STATUS, PaymentStatus.PAID)
     *     .groupBy(Ticket::getContest);
     *
     * // Group users by status
     * Map<UserStatus, List<User>> usersByStatus = queryFactory.query(User.class)
     *     .groupBy(User::getStatus);
     * }</pre>
     *
     * @param keyExtractor function to extract the grouping key from each entity
     * @param <K>          the key type
     * @return a Map where keys are extracted values and values are lists of entities
     */
    public <K> Map<K, List<T>> groupBy(Function<T, K> keyExtractor) {
        return query().stream()
                .collect(java.util.stream.Collectors.groupingBy(keyExtractor));
    }

    /**
     * Execute the query and group results by a RelationField.
     * Returns a Map where keys are related entities and values are lists of matching entities.
     *
     * <pre>{@code
     * // Group tickets by contest using field reference
     * Map<Contest, List<Ticket>> ticketsByContest = queryFactory.query(Ticket.class)
     *     .whereEqual(TicketFields.PAYMENT_STATUS, PaymentStatus.PAID)
     *     .groupBy(TicketFields.CONTEST);
     * }</pre>
     *
     * @param field the relation field to group by
     * @param <R>   the related entity type
     * @return a Map where keys are related entities and values are lists of entities
     */
    @SuppressWarnings("unchecked")
    public <R> Map<R, List<T>> groupBy(RelationField<R> field) {
        List<T> results = query();
        return results.stream()
                .collect(java.util.stream.Collectors.groupingBy(entity -> {
                    try {
                        java.lang.reflect.Field entityField = entityClass.getDeclaredField(field.name());
                        entityField.setAccessible(true);
                        return (R) entityField.get(entity);
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        throw new RuntimeException("Failed to access field: " + field.name(), e);
                    }
                }));
    }

    /**
     * Execute the query and group results by a Field.
     * Returns a Map where keys are field values and values are lists of matching entities.
     *
     * <pre>{@code
     * // Group users by status field
     * Map<UserStatus, List<User>> usersByStatus = queryFactory.query(User.class)
     *     .groupBy(UserFields.STATUS);
     * }</pre>
     *
     * @param field the field to group by
     * @param <V>   the field value type
     * @return a Map where keys are field values and values are lists of entities
     */
    @SuppressWarnings("unchecked")
    public <V> Map<V, List<T>> groupBy(Field<V> field) {
        List<T> results = query();
        return results.stream()
                .collect(java.util.stream.Collectors.groupingBy(entity -> {
                    try {
                        java.lang.reflect.Field entityField = entityClass.getDeclaredField(field.name());
                        entityField.setAccessible(true);
                        return (V) entityField.get(entity);
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        throw new RuntimeException("Failed to access field: " + field.name(), e);
                    }
                }));
    }

    /**
     * Execute the query and group results by a StringField.
     *
     * @param field the string field to group by
     * @return a Map where keys are string values and values are lists of entities
     */
    public Map<String, List<T>> groupBy(StringField field) {
        List<T> results = query();
        return results.stream()
                .collect(java.util.stream.Collectors.groupingBy(entity -> {
                    try {
                        java.lang.reflect.Field entityField = entityClass.getDeclaredField(field.name());
                        entityField.setAccessible(true);
                        return (String) entityField.get(entity);
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        throw new RuntimeException("Failed to access field: " + field.name(), e);
                    }
                }));
    }

    /**
     * Execute the query and group results by a ComparableField.
     *
     * @param field the comparable field to group by
     * @param <V>   the comparable type
     * @return a Map where keys are field values and values are lists of entities
     */
    @SuppressWarnings("unchecked")
    public <V extends Comparable<? super V>> Map<V, List<T>> groupBy(ComparableField<V> field) {
        List<T> results = query();
        return results.stream()
                .collect(java.util.stream.Collectors.groupingBy(entity -> {
                    try {
                        java.lang.reflect.Field entityField = entityClass.getDeclaredField(field.name());
                        entityField.setAccessible(true);
                        return (V) entityField.get(entity);
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        throw new RuntimeException("Failed to access field: " + field.name(), e);
                    }
                }));
    }

    /**
     * Internal method to execute GROUP BY COUNT query.
     */
    @SuppressWarnings("unchecked")
    private <V> Map<V, Long> executeGroupByCount(com.soyesenna.helixquery.expression.Expression<V> fieldExpr, Class<V> fieldType) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<T> criteriaRoot = cq.from(entityClass);

        CriteriaContext ctx = new CriteriaContext(cb, criteriaRoot, cq);
        CriteriaExpressionVisitor visitor = new CriteriaExpressionVisitor();

        applyJoins(ctx, criteriaRoot);

        // Compile the field expression
        jakarta.persistence.criteria.Expression<V> fieldPath =
                (jakarta.persistence.criteria.Expression<V>) visitor.compile(fieldExpr, ctx);

        // SELECT field, COUNT(*)
        cq.multiselect(fieldPath.alias("groupKey"), cb.count(criteriaRoot).alias("cnt"));

        // GROUP BY field
        cq.groupBy(fieldPath);

        // Apply WHERE clause
        applyWhereClause(ctx, visitor, cq);

        // Execute query
        List<Tuple> results = entityManager.createQuery(cq).getResultList();

        // Convert to Map
        Map<V, Long> resultMap = new LinkedHashMap<>();
        for (Tuple tuple : results) {
            V key = tuple.get("groupKey", fieldType);
            Long count = tuple.get("cnt", Long.class);
            resultMap.put(key, count);
        }

        return resultMap;
    }

    // ==================== Internal Build Methods ====================

    /**
     * Check if any collection fetch joins are registered.
     * Collection fetch joins with pagination cause HHH90003004 warning
     * and in-memory pagination which can lead to OOM and poor performance.
     */
    private boolean hasCollectionFetch() {
        for (JoinSpec joinSpec : joins) {
            if (joinSpec.fetch()) {
                // Check if this is a collection field (OneToMany, ManyToMany)
                // We detect this by checking if the attribute exists as a collection in the entity
                try {
                    java.lang.reflect.Field field = entityClass.getDeclaredField(joinSpec.attribute());
                    Class<?> fieldType = field.getType();
                    if (Collection.class.isAssignableFrom(fieldType)) {
                        return true;
                    }
                } catch (NoSuchFieldException e) {
                    // Try to check in superclasses
                    Class<?> superClass = entityClass.getSuperclass();
                    while (superClass != null && superClass != Object.class) {
                        try {
                            java.lang.reflect.Field field = superClass.getDeclaredField(joinSpec.attribute());
                            Class<?> fieldType = field.getType();
                            if (Collection.class.isAssignableFrom(fieldType)) {
                                return true;
                            }
                            break;
                        } catch (NoSuchFieldException ex) {
                            superClass = superClass.getSuperclass();
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Check if pagination is set.
     */
    private boolean hasPagination() {
        return offset != null || limit != null;
    }

    private int countCollectionFetches() {
        int count = 0;
        for (JoinSpec joinSpec : joins) {
            if (joinSpec.fetch()) {
                try {
                    java.lang.reflect.Field field = findFieldInHierarchy(entityClass, joinSpec.attribute());
                    if (field != null && Collection.class.isAssignableFrom(field.getType())) {
                        count++;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return count;
    }

    private java.lang.reflect.Field findFieldInHierarchy(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Get the ID attribute name using JPA Metamodel.
     * Returns the name of the @Id field for the entity.
     */
    @SuppressWarnings("unchecked")
    private String getEntityIdAttributeName() {
        EntityType<T> entityType = entityManager.getMetamodel().entity(entityClass);
        // Get the ID attribute - handles both simple and composite IDs
        if (entityType.hasSingleIdAttribute()) {
            SingularAttribute<? super T, ?> idAttribute = entityType.getId(entityType.getIdType().getJavaType());
            return idAttribute.getName();
        } else {
            // For composite IDs, we'll use the entity itself for IN clause
            // This is a fallback - most entities have single ID
            throw new UnsupportedOperationException(
                "Two-phase query optimization is not supported for entities with composite IDs. " +
                "Consider removing pagination when using collection fetch joins for entity: " + entityClass.getName());
        }
    }

    /**
     * Build query using two-phase approach to avoid HHH90003004 warning.
     * Phase 1: Get IDs with pagination (no collection fetch)
     * Phase 2: Fetch entities with collections using IDs
     */
    private List<T> buildQueryWithTwoPhase() {
        int collectionFetchCount = countCollectionFetches();
        if (collectionFetchCount > 1) {
            log.warn("Multiple collection fetch joins detected ({}). This may cause cartesian product " +
                    "and performance issues. Consider using separate queries or @BatchSize annotation.", 
                    collectionFetchCount);
        }
        
        String idAttributeName = getEntityIdAttributeName();
        
        // Phase 1: Get IDs with pagination (without collection fetch joins)
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        Root<T> idRoot;
        List<Object> ids;
        
        if (orders.isEmpty()) {
            // No ORDER BY - simple ID query with DISTINCT
            CriteriaQuery<Object> idQuery = cb.createQuery(Object.class);
            idRoot = idQuery.from(entityClass);
            
            CriteriaContext idCtx = new CriteriaContext(cb, idRoot, idQuery);
            CriteriaExpressionVisitor idVisitor = new CriteriaExpressionVisitor();
            
            applyJoinsForIdQuery(idCtx, idRoot);
            idQuery.select(idRoot.get(idAttributeName)).distinct(true);
            applyWhereClause(idCtx, idVisitor, idQuery);
            
            TypedQuery<Object> idTypedQuery = entityManager.createQuery(idQuery);
            applyPagination(idTypedQuery);
            ids = idTypedQuery.getResultList();
        } else {
            // With ORDER BY - must include order columns in SELECT for DISTINCT to work
            // Use Tuple query to select ID + order columns, then extract IDs
            CriteriaQuery<Tuple> tupleQuery = cb.createTupleQuery();
            idRoot = tupleQuery.from(entityClass);
            
            CriteriaContext idCtx = new CriteriaContext(cb, idRoot, tupleQuery);
            CriteriaExpressionVisitor idVisitor = new CriteriaExpressionVisitor();
            
            applyJoinsForIdQuery(idCtx, idRoot);
            
            List<Selection<?>> selections = new ArrayList<>();
            selections.add(idRoot.get(idAttributeName));
            for (OrderSpecifier orderSpec : orders) {
                selections.add(idVisitor.compile(orderSpec.target(), idCtx));
            }
            tupleQuery.multiselect(selections).distinct(true);
            
            applyWhereClause(idCtx, idVisitor, tupleQuery);
            applyOrderBy(idCtx, idVisitor, tupleQuery, cb);
            
            TypedQuery<Tuple> tupleTypedQuery = entityManager.createQuery(tupleQuery);
            applyPagination(tupleTypedQuery);
            
            // Extract just the IDs from tuples
            List<Tuple> tuples = tupleTypedQuery.getResultList();
            ids = new ArrayList<>(tuples.size());
            for (Tuple tuple : tuples) {
                ids.add(tuple.get(0));
            }
        }
        
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Phase 2: Fetch entities with collections using IDs (no pagination)
        CriteriaQuery<T> entityQuery = cb.createQuery(entityClass);
        Root<T> entityRoot = entityQuery.from(entityClass);
        
        CriteriaContext entityCtx = new CriteriaContext(cb, entityRoot, entityQuery);
        CriteriaExpressionVisitor entityVisitor = new CriteriaExpressionVisitor();
        
        // Apply all joins including fetch joins
        applyJoins(entityCtx, entityRoot);
        
        entityQuery.select(entityRoot);
        // Always use distinct when fetching collections to avoid duplicates
        entityQuery.distinct(true);
        
        // WHERE id IN (:ids)
        entityQuery.where(entityRoot.get(idAttributeName).in(ids));
        
        // Apply ORDER BY to maintain consistent ordering
        applyOrderBy(entityCtx, entityVisitor, entityQuery, cb);
        
        // No pagination here - we already got the right IDs
        List<T> results = entityManager.createQuery(entityQuery).getResultList();
        
        // Re-order results to match the order from Phase 1
        // This is necessary because IN clause doesn't preserve order
        Map<Object, T> resultMap = new LinkedHashMap<>();
        for (T entity : results) {
            try {
                java.lang.reflect.Field idField = findIdField(entityClass, idAttributeName);
                idField.setAccessible(true);
                Object idValue = idField.get(entity);
                resultMap.put(idValue, entity);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to access ID field: " + idAttributeName, e);
            }
        }
        
        // Return results in the original order from Phase 1
        List<T> orderedResults = new ArrayList<>(ids.size());
        for (Object id : ids) {
            T entity = resultMap.get(id);
            if (entity != null) {
                orderedResults.add(entity);
            }
        }
        
        return orderedResults;
    }
    
    /**
     * Find the ID field in the entity class or its superclasses.
     */
    private java.lang.reflect.Field findIdField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new RuntimeException("ID field not found: " + fieldName + " in " + clazz.getName());
    }
    
    /**
     * Apply joins for ID query - converts fetch joins to regular joins.
     * Similar to applyJoinsForCount but used for the first phase of two-phase query.
     */
    private void applyJoinsForIdQuery(CriteriaContext ctx, Root<T> criteriaRoot) {
        for (JoinSpec joinSpec : joins) {
            // Convert all joins (including fetch) to regular joins for ID query
            ctx.getOrCreateJoin(joinSpec.attribute(), joinSpec.type());
        }
    }

    private boolean requiresTwoPhaseQuery() {
        return hasCollectionFetch() && hasPagination();
    }

    private TypedQuery<T> buildQuery() {
        return buildQueryStandard();
    }
    
    /**
     * Standard query building without two-phase optimization.
     */
    private TypedQuery<T> buildQueryStandard() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> cq = cb.createQuery(entityClass);
        Root<T> criteriaRoot = cq.from(entityClass);

        CriteriaContext ctx = new CriteriaContext(cb, criteriaRoot, cq);
        CriteriaExpressionVisitor visitor = new CriteriaExpressionVisitor();

        // Apply joins
        applyJoins(ctx, criteriaRoot);

        cq.select(criteriaRoot);
        if (distinct) cq.distinct(true);

        // Apply WHERE
        applyWhereClause(ctx, visitor, cq);

        // Apply GROUP BY
        if (!groupByExpressions.isEmpty()) {
            List<jakarta.persistence.criteria.Expression<?>> groupByList = new ArrayList<>();
            for (com.soyesenna.helixquery.expression.Expression<?> expr : groupByExpressions) {
                groupByList.add(visitor.compile(expr, ctx));
            }
            cq.groupBy(groupByList);
        }

        // Apply HAVING
        if (havingPredicate != null) {
            cq.having(visitor.compilePredicate(havingPredicate, ctx));
        }

        // Apply ORDER BY
        applyOrderBy(ctx, visitor, cq, cb);

        TypedQuery<T> typedQuery = entityManager.createQuery(cq);
        applyPagination(typedQuery);

        return typedQuery;
    }

    private void applyJoins(CriteriaContext ctx, Root<T> criteriaRoot) {
        for (JoinSpec joinSpec : joins) {
            if (joinSpec.fetch()) {
                ctx.getOrCreateFetch(joinSpec.attribute(), joinSpec.type());
            } else {
                ctx.getOrCreateJoin(joinSpec.attribute(), joinSpec.type());
            }
        }
    }

    /**
     * Apply joins for count query - converts fetch joins to regular joins.
     * Count queries should not include fetch joins because they only select COUNT(*),
     * not the entity itself. Hibernate 6+ throws SemanticException when fetch joins
     * are used in queries where the owner entity is not in the select list.
     */
    private void applyJoinsForCount(CriteriaContext ctx, Root<T> criteriaRoot) {
        for (JoinSpec joinSpec : joins) {
            // Convert all joins (including fetch) to regular joins for count query
            ctx.getOrCreateJoin(joinSpec.attribute(), joinSpec.type());
        }
    }

    private void applyWhereClause(CriteriaContext ctx, CriteriaExpressionVisitor visitor, CriteriaQuery<?> cq) {
        if (predicateBuilder.hasValue()) {
            Predicate predicate = visitor.compilePredicate(predicateBuilder.build(), ctx);
            cq.where(predicate);
        }
    }

    private void applyOrderBy(CriteriaContext ctx, CriteriaExpressionVisitor visitor, CriteriaQuery<?> cq, CriteriaBuilder cb) {
        if (!orders.isEmpty()) {
            List<Order> orderList = new ArrayList<>();
            for (OrderSpecifier spec : orders) {
                jakarta.persistence.criteria.Expression<?> orderExpr = visitor.compile(spec.target(), ctx);
                Order order;
                if (spec.isAscending()) {
                    order = switch (spec.nullHandling()) {
                        case NULLS_FIRST -> cb.asc(orderExpr);  // Note: JPA doesn't have standard nulls first
                        case NULLS_LAST -> cb.asc(orderExpr);
                        default -> cb.asc(orderExpr);
                    };
                } else {
                    order = switch (spec.nullHandling()) {
                        case NULLS_FIRST -> cb.desc(orderExpr);
                        case NULLS_LAST -> cb.desc(orderExpr);
                        default -> cb.desc(orderExpr);
                    };
                }
                orderList.add(order);
            }
            cq.orderBy(orderList);
        }
    }

    private void applyPagination(TypedQuery<?> typedQuery) {
        if (offset != null) {
            typedQuery.setFirstResult(offset.intValue());
        }
        if (limit != null) {
            typedQuery.setMaxResults(limit.intValue());
        }
    }

    // ==================== Internal Helper Record ====================

    private record JoinSpec(String attribute, JoinType type, boolean fetch) {
    }
}
