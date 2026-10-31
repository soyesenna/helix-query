# HelixQuery

[![Maven Central](https://img.shields.io/maven-central/v/com.soyesenna/helixquery.svg)](https://central.sonatype.com/artifact/com.soyesenna/helixquery)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://www.oracle.com/java/)

> A type-safe JPA query builder that generates entity field constants at compile time and compiles its own expression tree to the JPA Criteria API

English | [한국어](./README.md)

## Overview

HelixQuery is a query builder library for Spring Boot and JPA. It works without Querydsl or any other query library, and without Q-type generation.

- **Generated field constants**: an annotation processor (`HelixQueryProcessor`) generates an `XxxFields` class for every `@Entity`. Depending on the field type it emits `StringField`, `NumberField`, `DateTimeField`, `ComparableField` (enums and other comparables), `CollectionField`, `RelationField` or `Field` constants.
- **Own expression tree, compiled to the Criteria API**: conditions, ordering and joins collected by a `HelixQuery` chain are compiled to the JPA Criteria API by `CriteriaExpressionVisitor` at execution time.
- **Relations and embedded fields**: `~ToOne` relations become nested classes (`UserFields.DEPARTMENT.NAME`) and `@Embedded` fields are flattened into constants (`UserFields.ADDRESS_CITY`). Filtering on a relation field adds a LEFT JOIN for that relation automatically.
- **Collection fetch joins with pagination**: when a collection fetch join is combined with offset and limit, the query runs in two phases (page of IDs first, then an IN query) instead of paginating in memory. Count queries leave fetch joins out.
- **Spring integration**: Spring Boot auto-configuration registers a `HelixQueryFactory` bean backed by the Spring-managed `EntityManager`, and a service extending `AbstractHelixService<T>` needs no constructor.

### No Querydsl since 0.1.0

The 0.0.x releases were built on Querydsl. Version 0.1.0 removed the Querydsl dependency and replaced it with an own expression tree compiled to the JPA Criteria API. The 0.0.x `AbstractQueryService`, `QueryChain` and `JPAQueryFactory` setup are gone; `AbstractHelixService`, `HelixQuery` and the auto-configured `HelixQueryFactory` take their place.

## Installation

The latest version on Maven Central is `com.soyesenna:helixquery:0.1.7`. The instructions assume a Spring Boot project (Spring Boot Gradle plugin with `io.spring.dependency-management`). The annotation processor ships in the same jar, so add the same coordinates to `annotationProcessor` as well, otherwise no `XxxFields` classes are generated.

**Gradle (Groovy)**
```groovy
dependencies {
    implementation 'com.soyesenna:helixquery:0.1.7'
    annotationProcessor 'com.soyesenna:helixquery:0.1.7'
}
```

**Gradle (Kotlin DSL)**
```kotlin
dependencies {
    implementation("com.soyesenna:helixquery:0.1.7")
    annotationProcessor("com.soyesenna:helixquery:0.1.7")
}
```

**Maven**

For 0.1.7 the jar is published on Maven Central as `helixquery-0.1.7-plain.jar`, so Maven needs the `plain` classifier. Gradle finds this jar through the published Gradle module metadata and needs no classifier.

```xml
<dependency>
    <groupId>com.soyesenna</groupId>
    <artifactId>helixquery</artifactId>
    <version>0.1.7</version>
    <classifier>plain</classifier>
</dependency>
```

If your project configures `annotationProcessorPaths` on `maven-compiler-plugin`, add the same coordinates (including the `plain` classifier) there too.

## Quick Start

### 1. Define an entity

```java
@Entity
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Integer age;

    @Enumerated(EnumType.STRING)
    private MemberStatus status;

    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    private Team team;

    @OneToMany(mappedBy = "member")
    private List<Order> orders = new ArrayList<>();

    @Embedded
    private Address address;   // city, street ...
}
```

### 2. The generated `MemberFields`

Compilation generates a class like this in the same package (abridged).

```java
public final class MemberFields {

    public static final NumberField<Long> ID = new NumberField<>("id", Long.class, Member.class);
    public static final StringField NAME = new StringField("name", Member.class);
    public static final NumberField<Integer> AGE = new NumberField<>("age", Integer.class, Member.class);
    public static final ComparableField<MemberStatus> STATUS = ...;
    public static final DateTimeField<LocalDateTime> CREATED_AT = ...;
    public static final CollectionField<Order> ORDERS = ...;

    // @Embedded fields are flattened
    public static final StringField ADDRESS_CITY = ...;

    // ~ToOne relations become nested classes
    public static final class TEAM {
        public static final RelationField<Team> $ = ...;   // relation field used for joins
        public static final StringField NAME = ...;        // team.name, adds a LEFT JOIN when used in a condition
    }
}
```

### 3. Write a service

```java
@Service
@Transactional(readOnly = true)
public class MemberService extends AbstractHelixService<Member> {
    // No constructor needed: Spring injects HelixQueryFactory and EntityManager,
    // and the entity type is resolved from the generic argument.

    public List<Member> search(String name, Integer minAge) {
        return find()
            .when(name != null, q -> q.whereContains(MemberFields.NAME, name))
            .when(minAge != null, q -> q.whereGreaterThanOrEqual(MemberFields.AGE, minAge))
            .whereEqual(MemberFields.TEAM.NAME, "Platform")
            .orderByDesc(MemberFields.CREATED_AT)
            .limit(10)
            .query();
    }

    public List<Member> findAllByIds(List<Long> ids) {
        return findBy(MemberFields.ID, ids).query();   // IN query, empty list gives an empty result
    }
}
```

`AbstractHelixService` provides query entry points such as `find()`, `findBy(...)` and `where(...)`, and persistence methods such as `save`, `persist`, `merge`, `remove` and `findById`.

## Features

### Conditions

```java
.whereEqual(MemberFields.STATUS, MemberStatus.ACTIVE)
.whereGreaterThan(MemberFields.AGE, 20)
.whereLessThanOrEqual(MemberFields.CREATED_AT, LocalDateTime.now())
.whereLike(MemberFields.NAME, "Kim%")
.whereContains(MemberFields.NAME, "jin")
.whereIn(MemberFields.STATUS, List.of(MemberStatus.ACTIVE, MemberStatus.PENDING))
.whereIsNull(MemberFields.AGE)
.whereBeforeNow(MemberFields.CREATED_AT)

// OR conditions and groups
.orEqual(MemberFields.STATUS, MemberStatus.PENDING)
.whereAnyOf(predicateA, predicateB)
.whereGroup(g -> g.and(predicateA).or(predicateB))
```

Field constants can also build predicates (`PredicateExpression`) directly.

```java
HelixQuery<Member> q = memberService.find();
q.where(MemberFields.AGE.between(q.root(), 20, 30))
 .and(MemberFields.NAME.startsWith(q.root(), "Kim"))
 .and(MemberFields.ORDERS.isNotEmpty(q.root()));
```

### Ordering and paging

```java
.orderByAsc(MemberFields.NAME)
.orderByDesc(MemberFields.CREATED_AT)
.offset(20).limit(10)

// Spring Data Page
Page<Member> page = memberService.find()
    .whereEqual(MemberFields.STATUS, MemberStatus.ACTIVE)
    .queryPageOrderByDesc(pageable, MemberFields.CREATED_AT);
```

### Joins

```java
.join(MemberFields.TEAM.$)
.leftJoin(MemberFields.TEAM.$)
.fetchJoin(MemberFields.TEAM.$)
.leftFetchJoin(MemberFields.TEAM.$)
.leftFetchJoin(MemberFields.ORDERS)          // collection fetch join
.leftFetchJoinDistinct(MemberFields.ORDERS)  // collection fetch join + DISTINCT
```

### Collection fetch joins with pagination

When a collection fetch join is combined with offset and limit, Hibernate loads the whole result into memory and slices it there (warning HHH90003004). HelixQuery detects this combination and runs the query in two phases.

1. Without fetch joins, apply the conditions, ordering and paging, and select only the IDs of the page.
2. Load the entities with `id IN (...)`, fetch-joining the collections, and restore the order from phase 1.

```java
Page<Member> page = memberService.find()
    .leftFetchJoin(MemberFields.ORDERS)
    .whereEqual(MemberFields.STATUS, MemberStatus.ACTIVE)
    .queryPageOrderByDesc(PageRequest.of(0, 20), MemberFields.CREATED_AT);
```

Count queries (`queryCount` and the total of `queryPage`) are built without fetch joins and use `count(distinct)` when a collection fetch is present. The two-phase query is not supported for entities with composite IDs. When the two-phase query sees two or more collection fetch joins, it logs a cartesian product warning.

### Execution

```java
List<Member> list = query.query();
Optional<Member> one = query.queryOne();
Member first = query.queryFirstOrNull();
long count = query.queryCount();
boolean exists = query.exists();

// Delete: throws without WHERE conditions (use deleteAll for everything)
long deleted = query.delete();      // delete through the persistence context (lifecycle callbacks, cascades)
long bulk = query.deleteBulk();     // CriteriaDelete bulk delete (bypasses the persistence context)
```

### Projections and aggregation

```java
HelixQuery<Member> q = memberService.find()
    .whereEqual(MemberFields.STATUS, MemberStatus.ACTIVE);

List<MemberDto> dtos = q.queryAs(Projections.constructor(MemberDto.class,
    MemberFields.NAME.path(q.root()),
    MemberFields.AGE.path(q.root())));

List<String> names = q.querySelect(MemberFields.NAME.path(q.root()));

Map<MemberStatus, Long> countByStatus = memberService.find().groupByCount(MemberFields.STATUS);
```

## Annotations and processor options

```java
@Entity
@GenerateFields(false)   // no Fields class for this entity
public class InternalEntity { ... }

@Entity
public class User {
    @IgnoreField          // left out of UserFields
    private String password;
}
```

Putting `@GenerateFields` on a class that is not an `@Entity` generates a Fields class for it as well.

| Option | Default | Description |
| --- | --- | --- |
| `HelixQuery.generateRelations` | `true` | Generate relation fields (`RelationField`, nested classes) |
| `HelixQuery.includeTransient` | `false` | Include `@Transient` fields |
| `HelixQuery.relationDepth` | `1` | How many levels of nested relation classes to generate |

```groovy
tasks.withType(JavaCompile).configureEach {
    options.compilerArgs += ['-AHelixQuery.relationDepth=2']
}
```

## Project structure

```
src/main/java/com/soyesenna/helixquery/
├── HelixQuery.java                 # fluent query builder, executed through the Criteria API
├── HelixQueryFactory.java          # creates HelixQuery instances
├── AbstractHelixService.java       # service base class
├── expression/                     # own expression tree and CriteriaExpressionVisitor
├── field/                          # Field, StringField, NumberField, DateTimeField, ComparableField, CollectionField, RelationField
├── predicate/PredicateBuilder.java # predicate groups
├── order/                          # ordering
├── projection/                     # Projections, TupleResult
├── annotations/                    # @GenerateFields, @IgnoreField
├── processor/HelixQueryProcessor.java        # annotation processor
└── autoconfigure/HelixQueryAutoConfiguration.java  # Spring Boot auto-configuration
```

## Requirements

- Java 21 or later
- Spring Boot 3 with Spring Data JPA (`jakarta.persistence`)
- The library is built against the Spring Boot 3.5.7 BOM and verified with integration tests on H2.

## Development

```bash
git clone https://github.com/soyesenna/helix-query.git
cd helix-query
./gradlew build
./gradlew test
```

## Changelog

- **0.1.7**: two-phase query for collection fetch joins combined with pagination, `count(distinct)` when a collection fetch is present
- **0.1.1 to 0.1.5**: nested fields for `~ToOne` relations, flattened `@Embedded` fields, paging helpers, fetch joins excluded from count queries, null and OR condition helpers, groupBy helpers, collection IN `findBy`, fix for duplicate joins when fetch joins meet ORDER BY
- **0.1.0**: Querydsl dependency removed, reimplemented on an own expression tree compiled to the JPA Criteria API
- **0.0.x**: initial Querydsl-based releases

See the [commit history](https://github.com/soyesenna/helix-query/commits/main) for details.

## License

[Apache License 2.0](LICENSE)

## Contact

- **Author**: Jooyoung Kim (soyesenna)
- **Email**: kjy915875@gmail.com
- **Issues**: [https://github.com/soyesenna/helix-query/issues](https://github.com/soyesenna/helix-query/issues)
