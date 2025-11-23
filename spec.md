# 1. 개요 (Overview)

`HelixQuery`(가칭)는 **JPA + Querydsl + Annotation Processing** 기반의 Spring 라이브러리로:

1. `@Entity` 가 붙은 엔티티 클래스를 컴파일 타임에 스캔하여
   각 엔티티마다 타입 세이프한 필드 메타데이터 클래스 `XxxFields` 를 자동 생성한다.

2. 이 `XxxFields` 와 Querydsl Q-타입을 연결하여, 다음과 같은 타입 세이프한 쿼리 DSL을 제공한다.

   ```java
   findBy(MemberFields.NAME, "홍길동")
       .orderByDesc(MemberFields.AGE)
       .limit(10)
       .query();   // List<Member>
   ```

3. 서비스 계층에서 상속받아 사용하는 `AbstractQueryService<T, Q>` 와
   플루언트 쿼리 빌더 `QueryChain<T, Q>` 를 통해:

    * `findBy(...)`, `findAll()`, `where(...)` 로 쿼리 시작
    * `.eq(...)`, `.like(...)`, `.orderBy(...)`, `.limit(...)`, `.join(...)` 등 체이닝
    * `.query()`, `.queryOne()`, `.queryCount()` 등으로 실행

4. **Persistence Context 보장**

    * `AbstractQueryService<T, Q>` 는 Spring이 관리하는 Bean으로 사용하며,
    * 내부에서 `@PersistenceContext` 로 주입받은 `EntityManager` 와 그로부터 생성된 `JPAQueryFactory` 를 사용해,
    * 서비스 메서드가 `@Transactional` 경계 내에서 실행될 경우,
      JPA의 1차 캐시, LAZY 로딩, Dirty Checking 등 **모든 Persistence Context 기능이 보장**된다.

---

# 2. 설계 목표 / 비목표

## 2.1 목표

* **타입 세이프 동적 쿼리**

    * 문자열 필드명 대신 `Field<T, Q>` 기반으로 컴파일 타임 체크
* **보일러플레이트 최소화**

    * 엔티티마다 Field 상수를 직접 정의하지 않고, AP로 `XxxFields` 자동 생성
* **Querydsl 친화적**

    * Querydsl `JPAQueryFactory`, `QEntity` 를 그대로 활용
    * Querydsl의 기능은 최대한 노출하면서 더 안전한 래핑 제공
* **서비스 계층 집중**

    * `AbstractQueryService<T, Q>` 상속으로 서비스는 **비즈니스 로직 + DSL조합**에만 집중
* **Persistence Context 관리 보장**

    * Spring에서 Bean으로 관리되는 서비스 + `@Transactional` 을 통해
      JPA 영속성 컨텍스트의 일관된 사용을 보장

## 2.2 비목표

* Spring Data Repository 메커니즘의 완전한 대체
* SQL Dialect 추상화 / Native Query DSL 제공
* Querydsl 자체의 기능을 숨기거나 대체하는 것

---

# 3. 모듈 구조

1. **`entity-query-core` (runtime)**

    * 런타임 타입 정의: `Field`, `AbstractQueryService`, `QueryChain` 등
    * 애플리케이션에서 `implementation` 으로 의존
2. **`entity-query-processor` (annotationProcessor)**

    * Annotation Processing 구현
    * `@Entity`/`@GenerateFields` 스캔 → `XxxFields` 생성
    * 빌드 시 `annotationProcessor`로만 추가

Gradle 예:

```groovy
dependencies {
    implementation "com.soyesenna:helix-query-core:1.0.0"
    annotationProcessor "com.soyesenna:helix-query-processor:1.0.0"

    implementation "com.querydsl:querydsl-jpa:5.x.x"
    annotationProcessor "com.querydsl:querydsl-apt:5.x.x:jpa"
}
```

---

# 4. Annotation & Processor 스펙

## 4.1 애노테이션

### 4.1.1 `@GenerateFields`

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface GenerateFields {
    /**
     * true면 이 엔티티에 대해 XxxFields 클래스를 생성한다.
     * 기본값: true
     */
    boolean value() default true;
}
```

### 4.1.2 `@Entity` 자동 감지

* `javax.persistence.Entity` 또는 `jakarta.persistence.Entity` 가 붙은 클래스는 기본적으로 처리 대상
* `@GenerateFields(false)` 가 붙어 있으면 제외
* `@GenerateFields` 가 붙어 있으면, `@Entity` 유무와 상관없이 처리 대상

## 4.2 필드 선택 규칙

Processor는 “DB 컬럼이 되는 필드”만 선택:

**포함**

* 인스턴스 필드 (`ElementKind.FIELD`)
* `static`/`final` 이 아닌 필드
* `@Transient` 가 **붙지 않은** 필드

**제외**

* `static`, `final` 필드
* Java `transient` 키워드가 붙은 필드
* JPA `@Transient` 필드
* `@IgnoreField` 같은 커스텀 제외 애노테이션이 붙은 필드

## 4.3 타입 매핑

엔티티 필드 타입 → `Field<T, Q>` 의 제네릭 `T`:

* 단순 타입: 그대로 사용

    * `String`, `Integer`, `Long`, `Boolean`, `LocalDate`, `LocalDateTime`, `Instant` 등
* Enum:

    * `Status` → `Field<Status, Q>`
* ManyToOne / OneToOne:

    * 연관 엔티티 타입 그대로 → `Field<Member, QOrder>` 처럼
* 컬렉션(`List<T>`, `Set<T>`) `CollectionField` 타입

---

# 5. 생성되는 `XxxFields` 클래스

## 5.1 위치 & 이름

* 패키지: 엔티티와 동일 패키지

    * `com.example.domain.member.Member` → `com.example.domain.member.MemberFields`
* 클래스명: `{EntitySimpleName}Fields`

## 5.2 구조 예시

```java
package com.example.domain.member;

import java.util.function.Function;

import com.example.HelixQuery.Field;
import com.querydsl.core.types.dsl.*;
import com.querydsl.core.types.dsl.EntityPathBase;

import com.example.domain.member.QMember;

public final class MemberFields {

    private MemberFields() {}

    public static final Field<String, QMember> NAME =
        new Field<>("name", String.class, m -> m.name);

    public static final Field<String, QMember> EMAIL =
        new Field<>("email", String.class, m -> m.email);

    public static final Field<Integer, QMember> AGE =
        new Field<>("age", Integer.class, m -> m.age);
}
```

* 상수명: 필드명 `camelCase` → UPPER_SNAKE_CASE

    * `createdAt` → `CREATED_AT`
    * `memberType` → `MEMBER_TYPE`

## 5.3 관계 필드

`RelationField<T, RQ, JQ>` 타입을 만들어 관계 필드에 대해 생성.

```java
public record RelationField<T, RQ extends EntityPathBase<?>, JQ extends EntityPathBase<T>>(
        String name,
        Class<T> type,
        Function<RQ, JQ> joinPathGetter
) {}
```

Processor가 ManyToOne 등의 필드에 대해 생성:

```java
public static final RelationField<Team, QMember, QTeam> TEAM =
    new RelationField<>("team", Team.class, m -> m.team);
```

---

# 6. core 모듈 런타임 타입

## 6.1 `Field<T, Q>`

```java
package com.example.HelixQuery;

import java.util.function.Function;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.SimpleExpression;

public record Field<T, Q extends EntityPathBase<?>>(
        String name,
        Class<T> type,
        Function<Q, SimpleExpression<T>> pathGetter
) {

    public SimpleExpression<T> path(Q root) {
        return pathGetter.apply(root);
    }

    public OrderSpecifier<T> asc(Q root) {
        return path(root).asc();
    }

    public OrderSpecifier<T> desc(Q root) {
        return path(root).desc();
    }
}
```

### 사용 예

```java
Field<String, QMember> f = MemberFields.NAME;

BooleanExpression predicate = f.path(QMember.member).eq("홍길동");
OrderSpecifier<String> order = f.asc(QMember.member);
```

---

## 6.2 `AbstractQueryService<T, Q>` – Persistence Context 보장 설계

```java
package com.example.HelixQuery;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.jpa.impl.JPAQueryFactory;

public abstract class AbstractQueryService<T, Q extends EntityPathBase<T>> {

    protected final JPAQueryFactory queryFactory;
    protected final Q q;

    /**
     * Spring의 Persistence Context 관리 대상 EntityManager.
     * - Spring Boot JPA 설정 + @EnableTransactionManagement 환경에서
     *   트랜잭션마다 적절한 영속성 컨텍스트를 바인딩하여 관리한다.
     */
    @PersistenceContext
    protected EntityManager em;

    protected AbstractQueryService(JPAQueryFactory queryFactory, Q q) {
        this.queryFactory = queryFactory;
        this.q = q;
    }

    // --- 기본 JPA CRUD 유틸 ---

    /**
     * 현재 Persistence Context에서 엔티티를 persist 한다.
     * 이 메서드는 반드시 @Transactional 경계 내에서 호출되어야 한다.
     */
    public T persist(T entity) {
        em.persist(entity);
        return entity;
    }

    public T merge(T entity) {
        return em.merge(entity);
    }

    public void remove(T entity) {
        em.remove(entity);
    }

    public void flush() {
        em.flush();
    }

    // --- QueryChain 시작점 ---

    /**
     * 조건 없이 selectFrom(q) 에서 시작하는 QueryChain.
     * 이 쿼리는 em 기반 JPAQueryFactory를 사용하므로,
     * 동일 트랜잭션 내에서는 같은 Persistence Context를 공유한다.
     */
    protected QueryChain<T, Q> findAll() {
        return new QueryChain<>(q, queryFactory.selectFrom(q));
    }

    /**
     * Field == value 조건으로 시작하는 QueryChain.
     */
    protected <V> QueryChain<T, Q> findBy(Field<V, Q> field, V value) {
        return new QueryChain<>(
                q,
                queryFactory
                        .selectFrom(q)
                        .where(field.path(q).eq(value))
        );
    }

    /**
     * 직접 BooleanExpression을 사용하여 시작하는 QueryChain.
     */
    protected QueryChain<T, Q> where(com.querydsl.core.types.dsl.BooleanExpression predicate) {
        return new QueryChain<>(q, queryFactory.selectFrom(q).where(predicate));
    }
}
```

### 6.2.1 Persistence Context 보장 규약

1. `AbstractQueryService<T, Q>` 를 상속하는 클래스는 **반드시 Spring Bean** 이어야 한다.

    * 일반적으로 `@Service`, `@Component` 사용

2. 트랜잭션 경계는 상위 서비스 메서드에 `@Transactional` 로 정의한다.

    * 메서드 또는 클래스 레벨에서 지정

3. `em`은 `@PersistenceContext` 로 주입되며,

    * Spring Boot / JPA 설정 + `@EnableTransactionManagement` 환경에서
    * 트랜잭션마다 해당 스레드에 바인딩된 Persistence Context를 제공한다.

4. `JPAQueryFactory` 는 보통 다음 설정으로 생성:

   ```java
   @Configuration
   public class QuerydslConfig {

       @PersistenceContext
       private EntityManager em;

       @Bean
       public JPAQueryFactory jpaQueryFactory() {
           return new JPAQueryFactory(em);
       }
   }
   ```

    * 이때 `JPAQueryFactory` 는 **동일한 EntityManager 프록시**를 사용하므로,
    * `QueryChain` 에서 수행되는 Querydsl 쿼리는 항상 **현재 트랜잭션의 Persistence Context**를 사용한다.

5. 따라서 `AbstractQueryService` 를 상속한 서비스의 메서드는:

    * `@Transactional` 내에서 호출될 경우

        * 엔티티는 `em`에 의해 관리되고,
        * Querydsl 쿼리도 동일 영속성 컨텍스트를 사용하여

            * 1차 캐시, LAZY 로딩, Dirty Checking 등이 완전히 동작한다.

---

## 6.3 `QueryChain<T, Q>` – 플루언트 쿼리 빌더

```java
package com.example.HelixQuery;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.*;
import com.querydsl.jpa.impl.JPAQuery;

public class QueryChain<T, Q extends EntityPathBase<T>> {

    private final Q root;
    private final JPAQuery<T> query;

    QueryChain(Q root, JPAQuery<T> query) {
        this.root = root;
        this.query = query;
    }

    /** Querydsl Q-type root. */
    public Q root() {
        return root;
    }

    /**
     * 내부 JPAQuery<T> 를 그대로 반환한다.
     * - 고급 사용자가 직접 Querydsl API(fetch 등)를 호출할 때 사용.
     */
    public JPAQuery<T> unwrap() {
        return query;
    }

    // --- where / 조건 조합 ---

    public QueryChain<T, Q> where(Predicate predicate) {
        if (predicate != null) {
            query.where(predicate);
        }
        return this;
    }

    public QueryChain<T, Q> and(Predicate predicate) {
        if (predicate != null) {
            query.where(predicate);
        }
        return this;
    }

    public QueryChain<T, Q> or(Predicate predicate) {
        if (predicate != null) {
            query.where(predicate);
        }
        return this;
    }

    // Field 기반 조건
    public <V> QueryChain<T, Q> eq(Field<V, Q> field, V value) {
        if (value != null) {
            query.where(field.path(root).eq(value));
        }
        return this;
    }

    public <V extends Comparable<?>> QueryChain<T, Q> gt(Field<V, Q> field, V value) {
        if (value != null) {
            query.where(field.path(root).gt(value));
        }
        return this;
    }

    public <V extends Comparable<?>> QueryChain<T, Q> lt(Field<V, Q> field, V value) {
        if (value != null) {
            query.where(field.path(root).lt(value));
        }
        return this;
    }

    public QueryChain<T, Q> like(Field<String, Q> field, String pattern) {
        if (pattern != null) {
            query.where(field.path(root).like(pattern));
        }
        return this;
    }

    public QueryChain<T, Q> in(Field<String, Q> field, List<String> values) {
        if (values != null && !values.isEmpty()) {
            query.where(field.path(root).in(values));
        }
        return this;
    }

    // --- order by ---

    public QueryChain<T, Q> orderBy(OrderSpecifier<?>... orders) {
        query.orderBy(orders);
        return this;
    }

    public <V> QueryChain<T, Q> orderByAsc(Field<V, Q> field) {
        query.orderBy(field.asc(root));
        return this;
    }

    public <V> QueryChain<T, Q> orderByDesc(Field<V, Q> field) {
        query.orderBy(field.desc(root));
        return this;
    }

    // --- pagination ---

    public QueryChain<T, Q> limit(long limit) {
        query.limit(limit);
        return this;
    }

    public QueryChain<T, Q> offset(long offset) {
        query.offset(offset);
        return this;
    }

    /**
     * Spring Data Pageable 연동.
     * sortFieldResolver는 Sort의 property(String) -> Field 로 매핑하는 함수.
     */
    public QueryChain<T, Q> pageable(
            org.springframework.data.domain.Pageable pageable,
            Function<String, Field<?, Q>> sortFieldResolver
    ) {
        query.offset(pageable.getOffset());
        query.limit(pageable.getPageSize());

        pageable.getSort().forEach(order -> {
            Field<?, Q> field = sortFieldResolver.apply(order.getProperty());
            if (field != null) {
                if (order.isAscending()) {
                    query.orderBy(field.asc(root));
                } else {
                    query.orderBy(field.desc(root));
                }
            }
        });

        return this;
    }

    // --- join ---

    public <J> QueryChain<T, Q> join(EntityPath<J> joinTarget, Predicate on) {
        query.join(joinTarget).on(on);
        return this;
    }

    public <J> QueryChain<T, Q> leftJoin(EntityPath<J> joinTarget, Predicate on) {
        query.leftJoin(joinTarget).on(on);
        return this;
    }

    public <J> QueryChain<T, Q> fetchJoin(EntityPath<J> joinTarget, Predicate on) {
        query.join(joinTarget).fetchJoin().on(on);
        return this;
    }

    // RelationField 기반 join
    public <J, JQ extends EntityPathBase<J>> QueryChain<T, Q> joinRelation(
            RelationField<J, Q, JQ> relation,
            JQ joinRoot,
            Predicate on
    ) {
        query.join(relation.joinPathGetter().apply(root), joinRoot).on(on);
        return this;
    }

    // --- group by / having ---

    public QueryChain<T, Q> groupBy(Expression<?>... expressions) {
        query.groupBy(expressions);
        return this;
    }

    public QueryChain<T, Q> having(Predicate predicate) {
        if (predicate != null) {
            query.having(predicate);
        }
        return this;
    }

    // --- 동적 적용 유틸 ---

    public QueryChain<T, Q> when(boolean condition, Consumer<QueryChain<T, Q>> customizer) {
        if (condition) {
            customizer.accept(this);
        }
        return this;
    }

    // --- 결과 실행부---

    /**
     * Querydsl의 fetch() 역할.
     * - 전체 결과 리스트를 조회한다.
     */
    public List<T> query() {
        return query.fetch();
    }

    /**
     * 단일 결과 또는 null.
     * - Querydsl fetchOne()과 동일.
     */
    public T queryOneOrNull() {
        return query.fetchOne();
    }

    /**
     * Optional 단일 결과.
     * - 여러 개 결과가 있으면 NonUniqueResultException.
     */
    public Optional<T> queryOne() {
        return Optional.ofNullable(query.fetchOne());
    }

    /**
     * 첫 번째 결과 또는 null.
     * - Querydsl fetchFirst()와 동일.
     */
    public T queryFirstOrNull() {
        return query.fetchFirst();
    }

    /**
     * count 쿼리 실행.
     * - Querydsl fetchCount() 래핑.
     */
    public long queryCount() {
        return query.fetchCount();
    }

    /**
     * 결과 존재 여부.
     * - fetchFirst() != null 형태의 exists 패턴.
     */
    public boolean exists() {
        return query.fetchFirst() != null;
    }

    // --- Projection (고급) ---

    /**
     * Q root를 기반으로 projection expression을 만들어 그대로 select + fetch() 한다.
     */
    public <R> List<R> queryAs(Function<Q, Expression<R>> projectionBuilder) {
        return query.select(projectionBuilder.apply(root)).fetch();
    }
}
```

---

# 7. Persistence Context & Service 계층 사용 규약

## 7.1 필수 조건

`AbstractQueryService<T, Q>` 를 상속하는 서비스는:

1. **Spring이 관리하는 Bean** 이어야 한다.

    * 예: `@Service`, `@Component`, `@Transactional` 등으로 선언
2. `JPAQueryFactory` 는 Spring Config 에서 `@Bean` 으로 생성되고,

    * 내부에 주입되는 `EntityManager` 는 `@PersistenceContext` 기반

## 7.2 트랜잭션 & Persistence Context 흐름

* 클라이언트 → Service Bean의 메서드 호출
* Service Bean 메서드는 `@Transactional` 적용
* 트랜잭션 시작 시:

    * Spring이 Thread-Local에 Persistence Context(EntityManager)를 바인딩
* `AbstractQueryService`:

    * 같은 EntityManager를 `@PersistenceContext`로 받음
    * 그리고 이 EntityManager를 사용하는 `JPAQueryFactory` 로 Querydsl 쿼리 실행
* 결과:

    * Service 메서드 내 모든 JPA/Querydsl 작업은 **동일한 Persistence Context** 를 사용

이 구조 덕분에:

* `persist`, `merge`, `remove` 등 JPA 작업
* `findBy`, `findAll` 등을 통해 만들어진 Querydsl 쿼리
* LAZY 로딩, Dirty Checking 등은 모두 동일 컨텍스트/트랜잭션에서 동작한다.

---

# 8. 서비스 사용 예

```java
@Service
public class MemberService extends AbstractQueryService<Member, QMember> {

    public MemberService(JPAQueryFactory queryFactory) {
        super(queryFactory, QMember.member);
    }

    @Transactional
    public Member create(String name, String email, int age) {
        Member m = new Member(name, email, age);
        return persist(m); // Persistence Context에 관리됨
    }

    @Transactional(readOnly = true)
    public List<Member> findByNameOrderByAgeDescLimit10(String name) {
        return findBy(MemberFields.NAME, name)
                .orderByDesc(MemberFields.AGE)
                .limit(10)
                .query();  // List<Member>
    }

    @Transactional(readOnly = true)
    public List<Member> search(String name, Integer minAge, Integer maxAge) {
        return findAll()
                .when(name != null,
                      c -> c.like(MemberFields.NAME, "%" + name + "%"))
                .when(minAge != null,
                      c -> c.gt(MemberFields.AGE, minAge))
                .when(maxAge != null,
                      c -> c.lt(MemberFields.AGE, maxAge))
                .orderByAsc(MemberFields.NAME)
                .query();
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Member> searchPaged(
            String name,
            org.springframework.data.domain.Pageable pageable
    ) {
        QueryChain<Member, QMember> chain = findAll()
                .when(name != null,
                      c -> c.like(MemberFields.NAME, "%" + name + "%"))
                .pageable(pageable, this::resolveField);

        List<Member> content = chain.query();
        long total = chain.unwrap().fetchCount(); // 필요시 직접 Querydsl 사용

        return new org.springframework.data.domain.PageImpl<>(content, pageable, total);
    }

    private Field<?, QMember> resolveField(String property) {
        return switch (property) {
            case "name" -> MemberFields.NAME;
            case "email" -> MemberFields.EMAIL;
            case "age" -> MemberFields.AGE;
            default -> null;
        };
    }
}
```

---

# 9. Spring 통합 스펙

## 9.1 QuerydslConfig

```java
@Configuration
@EnableTransactionManagement
public class QuerydslConfig {

    @PersistenceContext
    private EntityManager em;

    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(em);
    }
}
```

* `EnableTransactionManagement` (Boot에선 자동)
* 이 설정으로 `AbstractQueryService` 의 `queryFactory` + `em` 이 모두
  **동일 트랜잭션 및 Persistence Context** 를 사용하게 된다.

## 9.2 Auto-Configuration

`entity-query-spring-boot-starter` 형태로:

* `JPAQueryFactory` 자동 등록
* Processor 옵션(Spring properties)로 제어:

    * 대상 패키지 제한
    * 관계 필드 생성 여부
    * 컬렉션 필드 포함 여부 등

---

# 10. Advanced

* Processor 옵션

    * `-AHelixQuery.generateRelations=true` : `RelationField` 생성
    * `-AHelixQuery.includeTransient=true` : `@Transient` 필드도 포함
* Field 서브 타입

    * `EnumField`, `DateTimeField` 등 Path 타입 특화
* QueryChain 확장

    * 서브클래스 또는 데코레이터로 도메인 특화 DSL 추가

---

# 11. 예외/에러 처리

* Processor:

    * 지원 불가 타입/필드 → warning + 해당 필드는 Fields에서 제외
    * Q타입이 컴파일 시점에 없으면 `XxxFields`는 생성되지만,
      pathGetter(예: `m -> m.name`) 컴파일 단계에서 에러 발생
* 런타임:

    * `queryOne()` 에서 결과 여러 개 → Querydsl의 `NonUniqueResultException` 전파
    * `eq`, `gt`, `like` 등 조건 메서드에 값이 `null` → 조건 추가하지 않고 무시 (동적 조건 빌더에 유리)
    * JPA 예외는 Spring이 `DataAccessException` 으로 변환 가능

