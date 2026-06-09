package com.mss.surrealdbspringbootstarter.repository;

import com.surrealdb.Relation;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Extension of {@link SurrealRepository} that exposes SurrealDB graph-edge
 * operations ({@code RELATE}).
 *
 * <p>Extend this interface instead of {@link SurrealRepository} when the
 * repository's domain type participates in graph relationships and you want
 * to create edges directly from the repository layer.
 *
 * <p>The {@code fromId} in every method refers to the id of an entity managed
 * by <em>this</em> repository (i.e. an instance of {@code T}).  The
 * {@code toTable} + {@code toId} pair identifies the target record in any
 * table (which may be a different entity type).
 *
 * <h3>Usage</h3>
 * <pre>
 * {@literal @}Relate("wrote")
 * public class Wrote extends Relation {
 *     public Instant since;
 * }
 *
 * public interface PersonRepository
 *         extends SurrealGraphRepository&lt;Person, String&gt; { }
 *
 * // In a service:
 * personRepo.relate(Wrote.class, "alice", "article", "surreal");
 * </pre>
 *
 * @param <T>  the domain type the repository manages
 * @param <ID> the type of the domain's identifier
 *
 * @see SurrealRepository
 * @see com.mss.surrealdbspringbootstarter.mapping.Relate
 */
@NoRepositoryBean
public interface SurrealGraphRepository<T, ID> extends SurrealRepository<T, ID> {

    /**
     * Creates a bare graph edge (no metadata) from the record identified by
     * {@code fromId} in this repository's table to the record identified by
     * {@code toId} in {@code toTable}.
     *
     * <p>The edge-table name is resolved from the
     * {@link com.mss.surrealdbspringbootstarter.mapping.Relate @Relate}
     * annotation on {@code edgeType}.
     *
     * @param edgeType edge domain class (must extend {@link Relation})
     * @param fromId   id of the source record (owned by this repository)
     * @param toTable  table of the target record
     * @param toId     id of the target record
     * @param <R>      edge type
     * @return the created edge deserialised into {@code edgeType}
     */
    <R extends Relation> R relate(Class<R> edgeType, ID fromId, String toTable, Object toId);

    /**
     * Creates a graph edge with attached metadata content from the record
     * identified by {@code fromId} to the record identified by {@code toId} in
     * {@code toTable}.
     *
     * @param edgeType edge domain class
     * @param fromId   id of the source record
     * @param toTable  table of the target record
     * @param toId     id of the target record
     * @param content  object whose fields are stored as edge metadata
     * @param <R>      edge type
     * @param <C>      content type
     * @return the created edge deserialised into {@code edgeType}
     */
    <R extends Relation, C> R relate(Class<R> edgeType, ID fromId, String toTable, Object toId, C content);
}
