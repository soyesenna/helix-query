package com.soyesenna.helixquery.expression;

/**
 * Enumeration of all supported operators in HelixQuery DSL.
 * These operators map to JPA Criteria API operations.
 */
public enum Operator {

    // ==================== Comparison Operators ====================
    /** Equality: a = b */
    EQ,
    /** Inequality: a != b */
    NE,
    /** Greater than: a > b */
    GT,
    /** Greater than or equal: a >= b */
    GE,
    /** Less than: a < b */
    LT,
    /** Less than or equal: a <= b */
    LE,
    /** Between: a BETWEEN b AND c */
    BETWEEN,

    // ==================== Null Check Operators ====================
    /** Null check: a IS NULL */
    IS_NULL,
    /** Not null check: a IS NOT NULL */
    IS_NOT_NULL,

    // ==================== Collection Operators ====================
    /** In collection: a IN (b, c, d) */
    IN,
    /** Not in collection: a NOT IN (b, c, d) */
    NOT_IN,
    /** Member of collection: a MEMBER OF b */
    MEMBER_OF,
    /** Collection is empty: a IS EMPTY */
    IS_EMPTY,
    /** Collection is not empty: a IS NOT EMPTY */
    IS_NOT_EMPTY,
    /** Collection size: SIZE(a) */
    SIZE,

    // ==================== Boolean/Logical Operators ====================
    /** Logical AND: a AND b */
    AND,
    /** Logical OR: a OR b */
    OR,
    /** Logical NOT: NOT a */
    NOT,
    /** Boolean literal TRUE */
    TRUE,
    /** Boolean literal FALSE */
    FALSE,

    // ==================== String Operators ====================
    /** Like pattern: a LIKE b */
    LIKE,
    /** Like with escape: a LIKE b ESCAPE c */
    LIKE_ESCAPE,
    /** Contains substring (internal) */
    CONTAINS,
    /** Starts with prefix (internal) */
    STARTS_WITH,
    /** Ends with suffix (internal) */
    ENDS_WITH,
    /** Upper case: UPPER(a) */
    UPPER,
    /** Lower case: LOWER(a) */
    LOWER,
    /** Trim whitespace: TRIM(a) */
    TRIM,
    /** String length: LENGTH(a) */
    LENGTH,
    /** String concatenation: CONCAT(a, b) */
    CONCAT,
    /** Substring: SUBSTRING(a, b, c) */
    SUBSTRING,
    /** Locate substring: LOCATE(a, b) */
    LOCATE,

    // ==================== Numeric Operators ====================
    /** Addition: a + b */
    ADD,
    /** Subtraction: a - b */
    SUBTRACT,
    /** Multiplication: a * b */
    MULTIPLY,
    /** Division: a / b */
    DIVIDE,
    /** Modulo: a % b or MOD(a, b) */
    MOD,
    /** Absolute value: ABS(a) */
    ABS,
    /** Negation: -a */
    NEGATE,
    /** Square root: SQRT(a) */
    SQRT,

    // ==================== Aggregate Operators ====================
    /** Count: COUNT(a) */
    COUNT,
    /** Count distinct: COUNT(DISTINCT a) */
    COUNT_DISTINCT,
    /** Sum: SUM(a) */
    SUM,
    /** Average: AVG(a) */
    AVG,
    /** Minimum: MIN(a) */
    MIN,
    /** Maximum: MAX(a) */
    MAX,

    // ==================== Date/Time Operators ====================
    /** Current date: CURRENT_DATE */
    CURRENT_DATE,
    /** Current time: CURRENT_TIME */
    CURRENT_TIME,
    /** Current timestamp: CURRENT_TIMESTAMP */
    CURRENT_TIMESTAMP,

    // ==================== Type/Conversion Operators ====================
    /** Coalesce: COALESCE(a, b) */
    COALESCE,
    /** Null if equal: NULLIF(a, b) */
    NULLIF,
    /** Type cast */
    CAST
}
