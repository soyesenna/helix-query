# HelixQuery

[![Maven Central](https://img.shields.io/maven-central/v/com.soyesenna/helixquery.svg)](https://central.sonatype.com/artifact/com.soyesenna/helixquery)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://www.oracle.com/java/)

> 엔티티 필드 상수를 컴파일 타임에 생성하고, 자체 표현식 트리를 JPA Criteria API 로 변환해 실행하는 타입 세이프 JPA 쿼리 빌더

[English](./README.en.md) | 한국어

## 개요

HelixQuery 는 Spring Boot 와 JPA 위에서 동작하는 쿼리 빌더 라이브러리입니다. Querydsl 같은 별도 쿼리 라이브러리나 Q 타입 생성 없이 동작합니다.

- **필드 상수 생성**: 어노테이션 프로세서(`HelixQueryProcessor`)가 `@Entity` 클래스마다 `XxxFields` 클래스를 생성합니다. 필드 타입에 따라 `StringField`, `NumberField`, `DateTimeField`, `ComparableField`(enum 등), `CollectionField`, `RelationField`, `Field` 상수가 만들어집니다.
- **자체 표현식 트리와 Criteria API 변환**: `HelixQuery` 체인에 쌓인 조건, 정렬, 조인을 실행 시점에 `CriteriaExpressionVisitor` 가 JPA Criteria API 로 컴파일합니다.
- **관계와 임베디드 필드**: `~ToOne` 관계는 중첩 클래스(`UserFields.DEPARTMENT.NAME`)로, `@Embedded` 필드는 펼친 상수(`UserFields.ADDRESS_CITY`)로 생성됩니다. 관계 필드로 조건을 걸면 해당 관계에 LEFT JOIN 이 자동으로 붙습니다.
- **컬렉션 페치 조인과 페이지네이션**: 컬렉션 페치 조인과 offset, limit 을 함께 쓰면 메모리 페이지네이션 대신 2단계 조회(페이지 범위 ID 조회 후 IN 조회)로 실행합니다. 카운트 쿼리에서는 페치 조인을 제외합니다.
- **Spring 통합**: Spring Boot 자동 설정이 Spring 관리 `EntityManager` 로 `HelixQueryFactory` 빈을 등록하고, `AbstractHelixService<T>` 를 상속하면 생성자 없이 바로 쓸 수 있습니다.

### 0.1.0 부터 Querydsl 을 쓰지 않습니다

0.0.x 는 Querydsl 위에 만든 판이었습니다. 0.1.0 에서 Querydsl 의존을 걷어내고 자체 표현식 트리를 JPA Criteria API 로 변환하는 구조로 바꿨습니다. 0.0.x 의 `AbstractQueryService`, `QueryChain`, `JPAQueryFactory` 설정은 더 이상 쓰지 않으며, 각각 `AbstractHelixService`, `HelixQuery`, 자동 설정되는 `HelixQueryFactory` 가 대신합니다.

## 설치

최신 버전은 Maven Central 의 `com.soyesenna:helixquery:0.1.7` 입니다. Spring Boot 프로젝트(Spring Boot Gradle 플러그인과 `io.spring.dependency-management` 적용)를 기준으로 합니다. 어노테이션 프로세서가 같은 jar 에 들어 있으므로 `annotationProcessor` 에도 같은 좌표를 넣어야 `XxxFields` 클래스가 생성됩니다.

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

0.1.7 의 jar 는 Maven Central 에 `helixquery-0.1.7-plain.jar` 로 올라가 있어 Maven 에서는 `plain` 분류자를 붙여야 합니다. Gradle 은 함께 배포된 Gradle 모듈 메타데이터로 이 jar 를 찾으므로 분류자가 필요 없습니다.

```xml
<dependency>
    <groupId>com.soyesenna</groupId>
    <artifactId>helixquery</artifactId>
    <version>0.1.7</version>
    <classifier>plain</classifier>
</dependency>
```

`maven-compiler-plugin` 의 `annotationProcessorPaths` 를 쓰는 프로젝트라면 같은 좌표(분류자 `plain` 포함)를 거기에도 넣습니다.

## 빠른 시작

### 1. 엔티티 정의

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

### 2. 생성되는 `MemberFields`

컴파일하면 같은 패키지에 다음과 같은 클래스가 생성됩니다(요약).

```java
public final class MemberFields {

    public static final NumberField<Long> ID = new NumberField<>("id", Long.class, Member.class);
    public static final StringField NAME = new StringField("name", Member.class);
    public static final NumberField<Integer> AGE = new NumberField<>("age", Integer.class, Member.class);
    public static final ComparableField<MemberStatus> STATUS = ...;
    public static final DateTimeField<LocalDateTime> CREATED_AT = ...;
    public static final CollectionField<Order> ORDERS = ...;

    // @Embedded 필드는 펼쳐서 생성
    public static final StringField ADDRESS_CITY = ...;

    // ~ToOne 관계는 중첩 클래스로 생성
    public static final class TEAM {
        public static final RelationField<Team> $ = ...;   // 조인에 쓰는 관계 필드
        public static final StringField NAME = ...;        // team.name, 조건에 쓰면 LEFT JOIN 자동 추가
    }
}
```

### 3. 서비스 작성

```java
@Service
@Transactional(readOnly = true)
public class MemberService extends AbstractHelixService<Member> {
    // 생성자 불필요: HelixQueryFactory 와 EntityManager 는 Spring 이 주입하고,
    // 엔티티 타입은 제네릭 인자에서 해석합니다.

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
        return findBy(MemberFields.ID, ids).query();   // IN 조회, 빈 목록이면 빈 결과
    }
}
```

`AbstractHelixService` 는 `find()`, `findBy(...)`, `where(...)` 같은 쿼리 시작점과 `save`, `persist`, `merge`, `remove`, `findById` 같은 영속성 메서드를 제공합니다.

## 주요 기능

### 조건

```java
.whereEqual(MemberFields.STATUS, MemberStatus.ACTIVE)
.whereGreaterThan(MemberFields.AGE, 20)
.whereLessThanOrEqual(MemberFields.CREATED_AT, LocalDateTime.now())
.whereLike(MemberFields.NAME, "Kim%")
.whereContains(MemberFields.NAME, "jin")
.whereIn(MemberFields.STATUS, List.of(MemberStatus.ACTIVE, MemberStatus.PENDING))
.whereIsNull(MemberFields.AGE)
.whereBeforeNow(MemberFields.CREATED_AT)

// OR 조건과 묶음
.orEqual(MemberFields.STATUS, MemberStatus.PENDING)
.whereAnyOf(predicateA, predicateB)
.whereGroup(g -> g.and(predicateA).or(predicateB))
```

필드 상수에서 직접 조건식(`PredicateExpression`)을 만들 수도 있습니다.

```java
HelixQuery<Member> q = memberService.find();
q.where(MemberFields.AGE.between(q.root(), 20, 30))
 .and(MemberFields.NAME.startsWith(q.root(), "Kim"))
 .and(MemberFields.ORDERS.isNotEmpty(q.root()));
```

### 정렬과 페이지

```java
.orderByAsc(MemberFields.NAME)
.orderByDesc(MemberFields.CREATED_AT)
.offset(20).limit(10)

// Spring Data Page
Page<Member> page = memberService.find()
    .whereEqual(MemberFields.STATUS, MemberStatus.ACTIVE)
    .queryPageOrderByDesc(pageable, MemberFields.CREATED_AT);
```

### 조인

```java
.join(MemberFields.TEAM.$)
.leftJoin(MemberFields.TEAM.$)
.fetchJoin(MemberFields.TEAM.$)
.leftFetchJoin(MemberFields.TEAM.$)
.leftFetchJoin(MemberFields.ORDERS)          // 컬렉션 페치 조인
.leftFetchJoinDistinct(MemberFields.ORDERS)  // 컬렉션 페치 조인 + DISTINCT
```

### 컬렉션 페치 조인과 페이지네이션

컬렉션 페치 조인과 offset, limit 을 함께 쓰면 Hibernate 는 전체 결과를 메모리에 올린 뒤 잘라냅니다(HHH90003004 경고). HelixQuery 는 이 조합을 감지하면 두 단계로 나눠 실행합니다.

1. 페치 조인 없이 조건, 정렬, 페이지를 적용해 페이지 범위의 ID 만 조회합니다.
2. `id IN (...)` 으로 컬렉션까지 페치 조인해 엔티티를 가져오고, 1단계의 순서대로 다시 정렬합니다.

```java
Page<Member> page = memberService.find()
    .leftFetchJoin(MemberFields.ORDERS)
    .whereEqual(MemberFields.STATUS, MemberStatus.ACTIVE)
    .queryPageOrderByDesc(PageRequest.of(0, 20), MemberFields.CREATED_AT);
```

카운트 쿼리(`queryCount`, `queryPage` 의 전체 건수)는 페치 조인을 빼고 만들며, 컬렉션 페치가 있으면 `count(distinct)` 로 셉니다. 복합 키 엔티티에서는 2단계 조회를 지원하지 않습니다. 2단계 조회에서 컬렉션 페치 조인이 2개 이상이면 카테시안 곱 경고를 로그로 남깁니다.

### 실행

```java
List<Member> list = query.query();
Optional<Member> one = query.queryOne();
Member first = query.queryFirstOrNull();
long count = query.queryCount();
boolean exists = query.exists();

// 삭제: WHERE 조건이 없으면 예외 (전체 삭제는 deleteAll)
long deleted = query.delete();      // 영속성 컨텍스트를 거치는 삭제 (생명주기 콜백, cascade 반영)
long bulk = query.deleteBulk();     // CriteriaDelete 벌크 삭제 (영속성 컨텍스트를 거치지 않음)
```

### 프로젝션과 집계

```java
HelixQuery<Member> q = memberService.find()
    .whereEqual(MemberFields.STATUS, MemberStatus.ACTIVE);

List<MemberDto> dtos = q.queryAs(Projections.constructor(MemberDto.class,
    MemberFields.NAME.path(q.root()),
    MemberFields.AGE.path(q.root())));

List<String> names = q.querySelect(MemberFields.NAME.path(q.root()));

Map<MemberStatus, Long> countByStatus = memberService.find().groupByCount(MemberFields.STATUS);
```

## 어노테이션과 프로세서 옵션

```java
@Entity
@GenerateFields(false)   // 이 엔티티는 Fields 클래스를 만들지 않음
public class InternalEntity { ... }

@Entity
public class User {
    @IgnoreField          // UserFields 에서 제외
    private String password;
}
```

`@GenerateFields` 를 `@Entity` 가 아닌 클래스에 붙이면 그 클래스의 Fields 클래스도 생성합니다.

| 옵션 | 기본값 | 설명 |
| --- | --- | --- |
| `HelixQuery.generateRelations` | `true` | 관계 필드(`RelationField`, 중첩 클래스) 생성 |
| `HelixQuery.includeTransient` | `false` | `@Transient` 필드 포함 |
| `HelixQuery.relationDepth` | `1` | 관계 중첩 클래스를 몇 단계까지 만들지 |

```groovy
tasks.withType(JavaCompile).configureEach {
    options.compilerArgs += ['-AHelixQuery.relationDepth=2']
}
```

## 프로젝트 구조

```
src/main/java/com/soyesenna/helixquery/
├── HelixQuery.java                 # 플루언트 쿼리 빌더, Criteria API 로 실행
├── HelixQueryFactory.java          # HelixQuery 생성
├── AbstractHelixService.java       # 서비스 베이스 클래스
├── expression/                     # 자체 표현식 트리와 CriteriaExpressionVisitor
├── field/                          # Field, StringField, NumberField, DateTimeField, ComparableField, CollectionField, RelationField
├── predicate/PredicateBuilder.java # 조건 묶음
├── order/                          # 정렬
├── projection/                     # Projections, TupleResult
├── annotations/                    # @GenerateFields, @IgnoreField
├── processor/HelixQueryProcessor.java        # 어노테이션 프로세서
└── autoconfigure/HelixQueryAutoConfiguration.java  # Spring Boot 자동 설정
```

## 요구사항

- Java 21 이상
- Spring Boot 3 와 Spring Data JPA (`jakarta.persistence`)
- 라이브러리는 Spring Boot 3.5.7 BOM 으로 빌드하고, H2 위 통합 테스트로 검증합니다.

## 개발

```bash
git clone https://github.com/soyesenna/helix-query.git
cd helix-query
./gradlew build
./gradlew test
```

## 변경 이력

- **0.1.7**: 컬렉션 페치 조인과 페이지네이션을 함께 쓸 때 2단계 조회로 실행, 컬렉션 페치가 있으면 `count(distinct)` 로 집계
- **0.1.1 ~ 0.1.5**: `~ToOne` 관계 중첩 필드, `@Embedded` 펼침, 페이지 편의 메서드, 카운트 쿼리에서 페치 조인 제외, null 과 OR 조건 유틸, groupBy 유틸, 컬렉션 IN `findBy`, 페치 조인과 ORDER BY 를 함께 쓸 때 중복 조인 수정
- **0.1.0**: Querydsl 의존 제거, 자체 표현식 트리와 JPA Criteria API 변환으로 재구현
- **0.0.x**: Querydsl 기반 초기 판

자세한 내용은 [커밋 이력](https://github.com/soyesenna/helix-query/commits/main)을 참고하세요.

## 라이선스

[Apache License 2.0](LICENSE)

## 문의

- **작성자**: Jooyoung Kim (soyesenna)
- **이메일**: kjy915875@gmail.com
- **Issues**: [https://github.com/soyesenna/helix-query/issues](https://github.com/soyesenna/helix-query/issues)
