# HelixQuery

[![Maven Central](https://img.shields.io/maven-central/v/com.soyesenna/helixquery.svg)](https://central.sonatype.com/artifact/com.soyesenna/helixquery)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://www.oracle.com/java/)

> **Type-safe JPA Query Builder** - Automatic metadata generation at compile-time via Annotation Processing

English | [한국어](./README.md)

## Overview

HelixQuery is a Spring library based on **JPA + Querydsl + Annotation Processing**, providing an innovative solution for writing type-safe dynamic queries.

### Key Features

- 🎯 **Compile-time Type Safety**: Automatically generates field metadata from `@Entity` classes
- 🔗 **Fluent Query DSL**: Intuitive, chainable query API
- 🔄 **Persistence Context Guarantee**: Full integration with Spring-managed EntityManager
- ⚡ **Minimal Boilerplate**: No manual field definitions required
- 🛡️ **Querydsl Compatible**: Perfect compatibility with existing Querydsl features

### Why HelixQuery?

```java
// ❌ Traditional approach: String-based (runtime errors possible)
List<Member> results = memberRepository.findByName("John Doe");

// ✅ HelixQuery: Type-safe + Dynamic queries
List<Member> results = memberService
    .findBy(MemberFields.NAME, "John Doe")
    .whereGreaterThan(MemberFields.AGE, 20)
    .orderByDesc(MemberFields.CREATED_AT)
    .limit(10)
    .query();
```

## Quick Start

### 1. Add Dependencies

**Gradle (Kotlin DSL)**
```kotlin
dependencies {
    implementation("com.soyesenna:helixquery:0.0.1")

    // Querydsl dependencies
    implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    annotationProcessor("com.querydsl:querydsl-apt:5.1.0:jakarta")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api:3.1.0")
}
```

**Gradle (Groovy)**
```groovy
dependencies {
    implementation 'com.soyesenna:helixquery:0.0.1'

    implementation 'com.querydsl:querydsl-jpa:5.1.0:jakarta'
    annotationProcessor 'com.querydsl:querydsl-apt:5.1.0:jakarta'
    annotationProcessor 'jakarta.persistence:jakarta.persistence-api:3.1.0'
}
```

**Maven**
```xml
<dependencies>
    <dependency>
        <groupId>com.soyesenna</groupId>
        <artifactId>helixquery</artifactId>
        <version>0.0.1</version>
    </dependency>

    <dependency>
        <groupId>com.querydsl</groupId>
        <artifactId>querydsl-jpa</artifactId>
        <version>5.1.0</version>
        <classifier>jakarta</classifier>
    </dependency>
</dependencies>
```

### 2. Configure Querydsl

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

### 3. Define Entity

```java
@Entity
@Table(name = "members")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private Integer age;
    private LocalDateTime createdAt;

    // getters, setters, constructors...
}
```

During build, `MemberFields` class is automatically generated:

```java
public final class MemberFields {

    public static final Field<String, QMember> NAME =
        new Field<>("name", String.class, m -> m.name);

    public static final Field<String, QMember> EMAIL =
        new Field<>("email", String.class, m -> m.email);

    public static final Field<Integer, QMember> AGE =
        new Field<>("age", Integer.class, m -> m.age);

    public static final Field<LocalDateTime, QMember> CREATED_AT =
        new Field<>("createdAt", LocalDateTime.class, m -> m.createdAt);
}
```

### 4. Implement Service

```java
@Service
public class MemberService extends AbstractQueryService<Member, QMember> {

    public MemberService(JPAQueryFactory queryFactory) {
        super(queryFactory, QMember.member);
    }

    @Transactional
    public Member create(String name, String email, int age) {
        Member member = new Member(name, email, age);
        return persist(member);
    }

    @Transactional(readOnly = true)
    public List<Member> searchMembers(String name, Integer minAge, Integer maxAge) {
        return find()
            .when(name != null,
                  q -> q.whereLike(MemberFields.NAME, "%" + name + "%"))
            .when(minAge != null,
                  q -> q.whereGreaterThan(MemberFields.AGE, minAge))
            .when(maxAge != null,
                  q -> q.whereLessThan(MemberFields.AGE, maxAge))
            .orderByDesc(MemberFields.CREATED_AT)
            .query();
    }

    @Transactional(readOnly = true)
    public List<Member> findTop10ByNameOrderByAge(String name) {
        return findBy(MemberFields.NAME, name)
            .orderByAsc(MemberFields.AGE)
            .limit(10)
            .query();
    }
}
```

## Core Features

### 1. Type-safe Query Conditions

```java
// Equality
.whereEqual(MemberFields.NAME, "John Doe")

// Comparison
.whereGreaterThan(MemberFields.AGE, 20)
.whereLessThan(MemberFields.AGE, 60)

// String pattern matching
.whereLike(MemberFields.EMAIL, "%@example.com")

// IN clause
.whereIn(MemberFields.STATUS, List.of(Status.ACTIVE, Status.PENDING))

// Custom condition (direct Querydsl Predicate)
.where(MemberFields.AGE.path(root).between(20, 30))
```

### 2. Ordering and Pagination

```java
// Ordering
.orderByAsc(MemberFields.NAME)
.orderByDesc(MemberFields.CREATED_AT)

// Pagination
.offset(20)
.limit(10)

// Spring Data Pageable integration
.pageable(pageable, this::resolveField)

private Field<?, QMember> resolveField(String property) {
    return switch (property) {
        case "name" -> MemberFields.NAME;
        case "age" -> MemberFields.AGE;
        default -> null;
    };
}
```

### 3. Join Queries

```java
// Inner join
.join(QTeam.team, QTeam.team.id.eq(root().team.id))

// Left join
.leftJoin(QTeam.team, QTeam.team.id.eq(root().team.id))

// Fetch join (N+1 problem solver)
.fetchJoin(QTeam.team, QTeam.team.id.eq(root().team.id))

// RelationField-based join
.joinRelation(MemberFields.TEAM, QTeam.team, QTeam.team.active.isTrue())
```

### 4. Dynamic Query Builder

```java
public List<Member> dynamicSearch(SearchCriteria criteria) {
    return find()
        .when(criteria.getName() != null,
              q -> q.whereLike(MemberFields.NAME, "%" + criteria.getName() + "%"))
        .when(criteria.getMinAge() != null,
              q -> q.whereGreaterThan(MemberFields.AGE, criteria.getMinAge()))
        .when(criteria.getMaxAge() != null,
              q -> q.whereLessThan(MemberFields.AGE, criteria.getMaxAge()))
        .when(criteria.getEmail() != null,
              q -> q.whereEqual(MemberFields.EMAIL, criteria.getEmail()))
        .orderByDesc(MemberFields.CREATED_AT)
        .query();
}
```

### 5. Aggregation and Grouping

```java
// Count query
long count = find()
    .whereGreaterThan(MemberFields.AGE, 20)
    .queryCount();

// Existence check
boolean exists = find()
    .whereEqual(MemberFields.EMAIL, "test@example.com")
    .exists();

// Group By
.groupBy(MemberFields.AGE.path(root()))
.having(MemberFields.AGE.path(root()).count().gt(10))

// Projection (select specific fields)
List<String> names = find()
    .queryAs(q -> q.name);
```

### 6. Persistence Context Usage

```java
@Transactional
public Member updateMember(Long id, String newName) {
    // 1. Query (loaded into persistence context)
    Member member = findBy(MemberFields.ID, id)
        .queryOneOrNull();

    if (member == null) {
        throw new EntityNotFoundException();
    }

    // 2. Modify (Dirty Checking automatically applied)
    member.setName(newName);

    // 3. Automatic UPDATE on transaction commit without flush()
    return member;
}
```

## Advanced Features

### Annotation Options

#### @GenerateFields

Control Fields class generation for specific entities:

```java
@Entity
@GenerateFields(false)  // Skip Fields class generation
public class InternalEntity {
    // ...
}
```

#### @IgnoreField

Exclude specific fields from Fields class:

```java
@Entity
public class User {

    @Id
    private Long id;

    private String username;

    @IgnoreField  // Will not be generated in UserFields
    @JsonIgnore
    private String password;
}
```

### Processor Options

Configure Annotation Processor options in `build.gradle`:

```gradle
tasks.withType(JavaCompile) {
    options.compilerArgs += [
        '-AHelixQuery.generateRelations=true',    // Generate RelationField (default: true)
        '-AHelixQuery.includeTransient=false'     // Include @Transient fields (default: false)
    ]
}
```

### Collection Field Support

```java
@Entity
public class Team {

    @Id
    private Long id;

    @OneToMany(mappedBy = "team")
    private List<Member> members;
}
```

Generated `TeamFields`:

```java
public final class TeamFields {

    public static final CollectionField<Member, QTeam,
        CollectionExpressionBase<?, Member>> MEMBERS =
            new CollectionField<>("members", Member.class, t -> t.members);
}
```

### Leveraging Advanced Querydsl Features

Access the original JPAQuery via QueryChain's `unwrap()` method:

```java
JPAQuery<Member> query = find()
    .whereGreaterThan(MemberFields.AGE, 20)
    .unwrap();

// Use all Querydsl features
query.select(Projections.constructor(
    MemberDTO.class,
    QMember.member.name,
    QMember.member.email
));
```

## Project Structure

```
helixquery/
├── src/main/java/com/soyesenna/helixquery/
│   ├── Field.java                     # Field metadata type
│   ├── CollectionField.java           # Collection field type
│   ├── RelationField.java             # Relation field type
│   ├── AbstractQueryService.java      # Service base class
│   ├── QueryChain.java                # Fluent query builder
│   ├── annotations/
│   │   ├── GenerateFields.java        # Fields generation control annotation
│   │   └── IgnoreField.java           # Field exclusion annotation
│   ├── processor/
│   │   └── HelixQueryProcessor.java   # Annotation Processor implementation
│   └── autoconfigure/
│       └── HelixQueryAutoConfiguration.java  # Spring Boot auto-configuration
└── src/main/resources/
    └── META-INF/
        ├── services/
        │   └── javax.annotation.processing.Processor
        └── spring/
            └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

## Requirements

- **Java**: 21+
- **Spring Boot**: 3.x
- **Jakarta Persistence API**: 3.x
- **Querydsl**: 5.1.0+

## License

This project is licensed under the [Apache License 2.0](LICENSE).

## Contributing

Contributions are always welcome! Please feel free to open issues or submit Pull Requests.

### Development Setup

```bash
# Clone repository
git clone https://github.com/soyesenna/helix-query.git
cd helix-query

# Build
./gradlew build

# Test
./gradlew test
```

## Contact & Support

- **Author**: Jooyoung Kim (soyesenna)
- **Email**: kjy915875@gmail.com
- **GitHub**: [https://github.com/soyesenna/helix-query](https://github.com/soyesenna/helix-query)
- **Issues**: [https://github.com/soyesenna/helix-query/issues](https://github.com/soyesenna/helix-query/issues)

## Changelog

### 0.0.1 (2025-01-24)
- 🎉 Initial release
- ✨ Core features implementation
  - Field, CollectionField, RelationField
  - AbstractQueryService
  - QueryChain fluent API
  - HelixQueryProcessor
- 🔧 Spring Boot 3.x support
- 📝 Complete documentation

---

**⭐ If you find this project useful, please give it a Star on GitHub!**
