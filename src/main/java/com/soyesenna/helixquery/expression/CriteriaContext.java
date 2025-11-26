package com.soyesenna.helixquery.expression;

import jakarta.persistence.criteria.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Context object for compiling HelixQuery expressions to JPA Criteria API.
 * Maintains the state needed during expression compilation including
 * the CriteriaBuilder, root, and registered joins.
 */
public class CriteriaContext {

    private final CriteriaBuilder criteriaBuilder;
    private final Root<?> root;
    private final CriteriaQuery<?> query;
    private final Map<String, Join<?, ?>> joins;
    private final Map<String, Fetch<?, ?>> fetches;

    /**
     * Create a new criteria context.
     *
     * @param criteriaBuilder the JPA criteria builder
     * @param root            the query root
     * @param query           the criteria query (may be null for delete/update operations)
     */
    public CriteriaContext(CriteriaBuilder criteriaBuilder, Root<?> root, CriteriaQuery<?> query) {
        this.criteriaBuilder = Objects.requireNonNull(criteriaBuilder, "criteriaBuilder must not be null");
        this.root = Objects.requireNonNull(root, "root must not be null");
        this.query = query; // nullable for CriteriaDelete/CriteriaUpdate operations
        this.joins = new HashMap<>();
        this.fetches = new HashMap<>();
    }

    // ==================== Getters ====================

    public CriteriaBuilder getCriteriaBuilder() {
        return criteriaBuilder;
    }

    public Root<?> getRoot() {
        return root;
    }

    public CriteriaQuery<?> getQuery() {
        return query;
    }

    // ==================== Join Management ====================

    /**
     * Register a join for later retrieval.
     *
     * @param path the join path
     * @param join the join object
     */
    public void registerJoin(String path, Join<?, ?> join) {
        joins.put(path, join);
    }

    /**
     * Get a registered join by path.
     *
     * @param path the join path
     * @return the join, or null if not registered
     */
    public Join<?, ?> getJoin(String path) {
        return joins.get(path);
    }

    /**
     * Check if a join exists for the given path.
     *
     * @param path the join path
     * @return true if a join exists
     */
    public boolean hasJoin(String path) {
        return joins.containsKey(path);
    }

    /**
     * Get or create a join for a path.
     *
     * @param path     the attribute path (e.g., "department" or "department.manager")
     * @param joinType the join type
     * @return the join
     */
    @SuppressWarnings("unchecked")
    public <X, Y> Join<X, Y> getOrCreateJoin(String path, JoinType joinType) {
        if (joins.containsKey(path)) {
            return (Join<X, Y>) joins.get(path);
        }

        String[] segments = path.split("\\.");
        From<?, ?> current = root;

        StringBuilder currentPath = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                currentPath.append(".");
            }
            currentPath.append(segments[i]);
            String segmentPath = currentPath.toString();

            if (joins.containsKey(segmentPath)) {
                current = joins.get(segmentPath);
            } else {
                Join<?, ?> join = current.join(segments[i], joinType);
                joins.put(segmentPath, join);
                current = join;
            }
        }

        return (Join<X, Y>) current;
    }

    // ==================== Fetch Management ====================

    /**
     * Register a fetch for later retrieval.
     *
     * @param path  the fetch path
     * @param fetch the fetch object
     */
    public void registerFetch(String path, Fetch<?, ?> fetch) {
        fetches.put(path, fetch);
    }

    /**
     * Get or create a fetch for a path.
     *
     * @param path     the attribute path
     * @param joinType the join type for the fetch
     * @return the fetch
     */
    @SuppressWarnings("unchecked")
    public <X, Y> Fetch<X, Y> getOrCreateFetch(String path, JoinType joinType) {
        if (fetches.containsKey(path)) {
            return (Fetch<X, Y>) fetches.get(path);
        }

        String[] segments = path.split("\\.");
        FetchParent<?, ?> current = root;

        StringBuilder currentPath = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                currentPath.append(".");
            }
            currentPath.append(segments[i]);
            String segmentPath = currentPath.toString();

            if (fetches.containsKey(segmentPath)) {
                current = fetches.get(segmentPath);
            } else {
                Fetch<?, ?> fetch = current.fetch(segments[i], joinType);
                fetches.put(segmentPath, fetch);
                current = fetch;
            }
        }

        return (Fetch<X, Y>) current;
    }

    // ==================== Path Resolution ====================

    /**
     * Resolve a path expression to a JPA Path.
     * Handles nested paths by navigating through attributes.
     *
     * @param pathExpr the HelixQuery path expression
     * @return the JPA criteria Path
     */
    @SuppressWarnings("unchecked")
    public <T> Path<T> resolvePath(PathExpression<T> pathExpr) {
        if (pathExpr.isRoot()) {
            return (Path<T>) root;
        }

        String fullPath = pathExpr.getFullPath();
        String[] segments = fullPath.split("\\.");

        Path<?> current = root;
        StringBuilder currentPath = new StringBuilder();

        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                currentPath.append(".");
            }
            currentPath.append(segments[i]);
            String segmentPath = currentPath.toString();

            // Check if this segment is a registered join
            if (joins.containsKey(segmentPath)) {
                current = joins.get(segmentPath);
            } else {
                current = current.get(segments[i]);
            }
        }

        return (Path<T>) current;
    }
}
