# HelixQuery

[![Maven Central](https://img.shields.io/maven-central/v/com.soyesenna/helixquery.svg)](https://central.sonatype.com/artifact/com.soyesenna/helixquery)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://www.oracle.com/java/)

> **타입 세이프한 JPA 쿼리 빌더** - Annotation Processing 기반의 컴파일 타임 메타데이터 자동 생성

[English](./README.en.md) | 한국어

## 개요

HelixQuery는 **JPA + Querydsl + Annotation Processing** 기반의 Spring 라이브러리로, 타입 세이프한 동적 쿼리 작성을 위한 혁신적인 솔루션을 제공합니다.

### 핵심 기능

- 🎯 **컴파일 타임 타입 안전성**: `@Entity` 클래스에서 자동으로 필드 메타데이터 생성
- 🔗 **플루언트 쿼리 DSL**: 직관적이고 체이닝 가능한 쿼리 API
- 🔄 **Persistence Context 보장**: Spring 관리 EntityManager와 완전한 통합
- ⚡ **보일러플레이트 최소화**: 수동 필드 정의 불필요
- 🛡️ **Querydsl 친화적**: 기존 Querydsl 기능과 완벽한 호환

### 왜 HelixQuery인가?

```java
// ❌ 기존 방식: 문자열 기반 (런타임 오류 가능)
List<Member> results = memberRepository.findByName("홍길동");

// ✅ HelixQuery: 타입 세이프 + 동적 쿼리
List<Member> results = memberService
    .findBy(MemberFields.NAME, "홍길동")
    .whereGreaterThan(MemberFields.AGE, 20)
    .orderByDesc(MemberFields.CREATED_AT)
    .limit(10)
    .query();
```

## 빠른 시작

### 1. 의존성 추가

**Gradle (Kotlin DSL)**
```kotlin
dependencies {
    implementation("com.soyesenna:helixquery:0.0.1")

    // Querydsl 의존성
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

### 2. Querydsl 설정

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

### 3. 엔티티 정의

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

빌드 시 `MemberFields` 클래스가 자동 생성됩니다:

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

### 4. 서비스 구현

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

## 주요 기능

### 1. 타입 세이프한 쿼리 조건

```java
// 등호 조건
.whereEqual(MemberFields.NAME, "홍길동")

// 비교 조건
.whereGreaterThan(MemberFields.AGE, 20)
.whereLessThan(MemberFields.AGE, 60)

// 문자열 패턴 매칭
.whereLike(MemberFields.EMAIL, "%@example.com")

// IN 조건
.whereIn(MemberFields.STATUS, List.of(Status.ACTIVE, Status.PENDING))

// 커스텀 조건 (Querydsl Predicate 직접 사용)
.where(MemberFields.AGE.path(root).between(20, 30))
```

### 2. 정렬 및 페이징

```java
// 정렬
.orderByAsc(MemberFields.NAME)
.orderByDesc(MemberFields.CREATED_AT)

// 페이징
.offset(20)
.limit(10)

// Spring Data Pageable 통합
.pageable(pageable, this::resolveField)

private Field<?, QMember> resolveField(String property) {
    return switch (property) {
        case "name" -> MemberFields.NAME;
        case "age" -> MemberFields.AGE;
        default -> null;
    };
}
```

### 3. 조인 쿼리

```java
// 기본 조인
.join(QTeam.team, QTeam.team.id.eq(root().team.id))

// Left 조인
.leftJoin(QTeam.team, QTeam.team.id.eq(root().team.id))

// Fetch 조인 (N+1 문제 해결)
.fetchJoin(QTeam.team, QTeam.team.id.eq(root().team.id))

// RelationField 기반 조인
.joinRelation(MemberFields.TEAM, QTeam.team, QTeam.team.active.isTrue())
```

### 4. 동적 쿼리 빌더

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

### 5. 집계 및 그룹화

```java
// Count 쿼리
long count = find()
    .whereGreaterThan(MemberFields.AGE, 20)
    .queryCount();

// 존재 여부 확인
boolean exists = find()
    .whereEqual(MemberFields.EMAIL, "test@example.com")
    .exists();

// Group By
.groupBy(MemberFields.AGE.path(root()))
.having(MemberFields.AGE.path(root()).count().gt(10))

// 프로젝션 (특정 필드만 조회)
List<String> names = find()
    .queryAs(q -> q.name);
```

### 6. Persistence Context 활용

```java
@Transactional
public Member updateMember(Long id, String newName) {
    // 1. 조회 (영속성 컨텍스트에 로딩)
    Member member = findBy(MemberFields.ID, id)
        .queryOneOrNull();

    if (member == null) {
        throw new EntityNotFoundException();
    }

    // 2. 수정 (Dirty Checking 자동 적용)
    member.setName(newName);

    // 3. flush() 호출 없이도 트랜잭션 커밋 시 자동 UPDATE
    return member;
}
```

## 고급 기능

### 애노테이션 옵션

#### @GenerateFields

특정 엔티티에 대한 Fields 클래스 생성 제어:

```java
@Entity
@GenerateFields(false)  // Fields 클래스 생성 안 함
public class InternalEntity {
    // ...
}
```

#### @IgnoreField

특정 필드를 Fields 클래스에서 제외:

```java
@Entity
public class User {

    @Id
    private Long id;

    private String username;

    @IgnoreField  // UserFields에 생성되지 않음
    @JsonIgnore
    private String password;
}
```

### Processor 옵션

`build.gradle`에서 Annotation Processor 옵션 설정:

```gradle
tasks.withType(JavaCompile) {
    options.compilerArgs += [
        '-AHelixQuery.generateRelations=true',    // RelationField 생성 (기본: true)
        '-AHelixQuery.includeTransient=false'     // @Transient 필드 포함 (기본: false)
    ]
}
```

### 컬렉션 필드 지원

```java
@Entity
public class Team {

    @Id
    private Long id;

    @OneToMany(mappedBy = "team")
    private List<Member> members;
}
```

생성된 `TeamFields`:

```java
public final class TeamFields {

    public static final CollectionField<Member, QTeam,
        CollectionExpressionBase<?, Member>> MEMBERS =
            new CollectionField<>("members", Member.class, t -> t.members);
}
```

### Querydsl 고급 기능 활용

QueryChain의 `unwrap()` 메서드로 원본 JPAQuery 접근:

```java
JPAQuery<Member> query = find()
    .whereGreaterThan(MemberFields.AGE, 20)
    .unwrap();

// Querydsl의 모든 기능 사용 가능
query.select(Projections.constructor(
    MemberDTO.class,
    QMember.member.name,
    QMember.member.email
));
```

## 프로젝트 구조

```
helixquery/
├── src/main/java/com/soyesenna/helixquery/
│   ├── Field.java                     # 필드 메타데이터 타입
│   ├── CollectionField.java           # 컬렉션 필드 타입
│   ├── RelationField.java             # 관계 필드 타입
│   ├── AbstractQueryService.java      # 서비스 베이스 클래스
│   ├── QueryChain.java                # 플루언트 쿼리 빌더
│   ├── annotations/
│   │   ├── GenerateFields.java        # Fields 생성 제어 애노테이션
│   │   └── IgnoreField.java           # 필드 제외 애노테이션
│   ├── processor/
│   │   └── HelixQueryProcessor.java   # Annotation Processor 구현
│   └── autoconfigure/
│       └── HelixQueryAutoConfiguration.java  # Spring Boot 자동 설정
└── src/main/resources/
    └── META-INF/
        ├── services/
        │   └── javax.annotation.processing.Processor
        └── spring/
            └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

## 요구사항

- **Java**: 21 이상
- **Spring Boot**: 3.x
- **Jakarta Persistence API**: 3.x
- **Querydsl**: 5.1.0 이상

## 라이선스

이 프로젝트는 [Apache License 2.0](LICENSE) 하에 배포됩니다.

## 기여

기여는 언제나 환영합니다! 이슈를 열거나 Pull Request를 제출해 주세요.

### 개발 환경 설정

```bash
# 저장소 클론
git clone https://github.com/soyesenna/helix-query.git
cd helix-query

# 빌드
./gradlew build

# 테스트
./gradlew test
```

## 문의 및 지원

- **작성자**: Jooyoung Kim (soyesenna)
- **이메일**: kjy915875@gmail.com
- **GitHub**: [https://github.com/soyesenna/helix-query](https://github.com/soyesenna/helix-query)
- **Issues**: [https://github.com/soyesenna/helix-query/issues](https://github.com/soyesenna/helix-query/issues)

## 변경 이력

### 0.0.1 (2025-01-24)
- 🎉 초기 릴리즈
- ✨ 핵심 기능 구현
  - Field, CollectionField, RelationField
  - AbstractQueryService
  - QueryChain 플루언트 API
  - HelixQueryProcessor
- 🔧 Spring Boot 3.x 지원
- 📝 완전한 문서화

---

**⭐ 이 프로젝트가 유용하다면 GitHub에서 Star를 눌러주세요!**
