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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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
        TypedQuery<T> typedQuery = buildQuery();
        typedQuery.setMaxResults(2);
        List<T> results = typedQuery.getResultList();
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

        countQuery.select(distinct ? cb.countDistinct(countRoot) : cb.count(countRoot));

        CriteriaContext ctx = new CriteriaContext(cb, countRoot, countQuery);
        applyJoins(ctx, countRoot);

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

    private TypedQuery<T> buildQuery() {
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
