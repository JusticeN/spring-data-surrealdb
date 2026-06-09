package com.mss.surrealdbspringbootstarter.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mss.surrealdbspringbootstarter.mapping.SurrealEntityInformation;
import com.surrealdb.InsertRelation;
import com.surrealdb.NotFoundException;
import com.surrealdb.RecordId;
import com.surrealdb.Relation;
import com.surrealdb.Response;
import com.surrealdb.Surreal;
import com.surrealdb.UpType;
import com.surrealdb.Value;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Primary interface for interacting with a SurrealDB instance.
 *
 * <p>Provides high-level CRUD and graph operations. This class automatically
 * handles {@link com.mss.surrealdbspringbootstarter.mapping.Link @Link} and
 * {@link com.mss.surrealdbspringbootstarter.mapping.LinkList @LinkList} fields
 * by generating appropriate {@code FETCH} clauses or SurQL projections.
 *
 * <p>Entities with {@code @Id String id} use a SurQL projection path to ensure
 * the server-assigned {@code RecordId} is returned as a JSON string, allowing
 * clean deserialisation into the domain model.
 *
 * <p>Entities without such fields use the original {@code select()} path, so
 * existing behaviour is fully preserved.
 */
public class SurrealTemplate {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final Surreal surreal;

    public SurrealTemplate(Surreal surreal) {
        this.surreal = surreal;
    }

    /**
     * Inserts a single entity into the database.
     *
     * <p>If the entity has an {@code @Id} set, SurrealDB will use it. Otherwise,
     * a random ID is generated.
     *
     * @param entity the entity to save
     * @param <T>    entity type
     * @return the saved entity (with server-assigned {@code @Id} populated)
     */
    @SuppressWarnings("unchecked")
    public <T> T insert(T entity) {
        SurrealEntityInformation<T, Object> info =
                new SurrealEntityInformation<>((Class<T>) entity.getClass());

        if (!info.isStringId()) {
            List<T> created = surreal.create(info.type(), info.table(), entity);
            return created.isEmpty() ? entity : created.get(0);
        }

        // String ID path — use query to project ID as string
        String surql = "CREATE " + info.table() + " CONTENT $content RETURN *, <string>id AS id";
        Map<String, Object> bindings = Map.of("content", entity);
        Value result = surreal.query(surql, bindings).take(0);
        if (result.isArray() && result.getArray().len() > 0) {
            return result.getArray().get(0).get(info.type());
        }
        return result.get(info.type());
    }

    /**
     * Select a single record by id.
     *
     * <p>Automatically includes all {@code fetch=true} fields in a FETCH clause.
     *
     * @param type the domain class
     * @param id   the record identifier (String or RecordId)
     * @param <T>  entity type
     * @param <ID> identifier type
     * @return an Optional containing the entity if found
     */
    public <T, ID> Optional<T> findById(Class<T> type, ID id) {
        SurrealEntityInformation<T, ID> info = new SurrealEntityInformation<>(type);
        List<String> fetches = info.fetchFields();
        List<String> lazyLinks = info.lazyLinkFields();
        List<String> lazyLinkLists = info.lazyLinkListFields();

        if (fetches.isEmpty() && lazyLinks.isEmpty() && lazyLinkLists.isEmpty() && !info.isStringId()) {
            try {
                return surreal.select(type, recordId(info.table(), id));
            } catch (NotFoundException e) {
                return Optional.empty();
            }
        }

        // Projection/FETCH path — issue a SELECT query
        StringBuilder surql = new StringBuilder("SELECT *");
        if (info.isStringId()) {
            surql.append(", <string>id AS id");
        }
        for (String field : lazyLinks) {
            surql.append(", IF ").append(field).append(" != NONE THEN { id: <string>").append(field).append(".id } ELSE NONE END AS ").append(field);
        }
        for (String field : lazyLinkLists) {
            surql.append(", (SELECT <string>id AS id FROM $parent.").append(field).append(") AS ").append(field);
        }
        surql.append(" FROM ").append(recordId(info.table(), id).toString());

        if (!fetches.isEmpty()) {
            surql.append(" FETCH ").append(String.join(", ", fetches));
        }

        try {
            Value result = surreal.query(surql.toString()).take(0);
            if (result.isNone() || result.isNull()) {
                return Optional.empty();
            }
            if (result.isArray()) {
                if (result.getArray().len() == 0) {
                    return Optional.empty();
                }
                return Optional.ofNullable(result.getArray().get(0).get(type));
            }
            return Optional.ofNullable(result.get(type));
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }

    /**
     * Select every record from the table mapped to {@code type}.
     *
     * <p>Automatically includes all {@code fetch=true} fields in a FETCH clause.
     *
     * @param type the domain class
     * @param <T>  entity type
     * @return a list of all records in the table
     */
    public <T> List<T> findAll(Class<T> type) {
        SurrealEntityInformation<T, Object> info = new SurrealEntityInformation<>(type);
        List<String> fetches = info.fetchFields();
        List<String> lazyLinks = info.lazyLinkFields();
        List<String> lazyLinkLists = info.lazyLinkListFields();

        if (fetches.isEmpty() && lazyLinks.isEmpty() && lazyLinkLists.isEmpty() && !info.isStringId()) {
            List<T> out = new ArrayList<>();
            try {
                Iterator<T> it = surreal.select(type, info.table());
                while (it.hasNext()) {
                    out.add(it.next());
                }
            } catch (NotFoundException e) {
                // Table does not exist yet — return empty list
            }
            return out;
        }

        // Projection/FETCH path — issue a SELECT query
        StringBuilder surql = new StringBuilder("SELECT *");
        if (info.isStringId()) {
            surql.append(", <string>id AS id");
        }
        for (String field : lazyLinks) {
            surql.append(", IF ").append(field).append(" != NONE THEN { id: <string>").append(field).append(".id } ELSE NONE END AS ").append(field);
        }
        for (String field : lazyLinkLists) {
            surql.append(", (SELECT <string>id AS id FROM $parent.").append(field).append(") AS ").append(field);
        }
        surql.append(" FROM ").append(info.table());

        if (!fetches.isEmpty()) {
            surql.append(" FETCH ").append(String.join(", ", fetches));
        }

        Value result = surreal.query(surql.toString()).take(0);
        List<T> out = new ArrayList<>();
        if (result.isArray()) {
            for (Value element : result.getArray()) {
                T entity = element.get(type);
                if (entity != null) {
                    out.add(entity);
                }
            }
        }
        return out;
    }

    /**
     * Updates an existing entity in the database.
     *
     * <p>Requires the entity to have a non-null {@code @Id}.
     *
     * @param entity the entity to update
     * @param <T>    entity type
     * @return the updated entity as returned by the database
     * @throws IllegalArgumentException if the entity ID is missing
     */
    @SuppressWarnings("unchecked")
    public <T> T update(T entity) {
        SurrealEntityInformation<T, Object> info =
                new SurrealEntityInformation<>((Class<T>) entity.getClass());
        Object id = info.getId(entity);
        if (id == null) {
            throw new IllegalArgumentException(
                    "Cannot update " + info.type().getName() + " without an @Id value");
        }
        // RecordId @Id: use the inner key portion, not the full "table:key" string
        RecordId rid = (id instanceof RecordId r) ? r : recordId(info.table(), id);

        if (!info.isStringId()) {
            T updated = surreal.update(info.type(), rid, UpType.CONTENT, entity);
            return updated != null ? updated : entity;
        }

        // String ID path — use MERGE to avoid "id field found but specific record specified" error
        String surql = "UPDATE " + rid.toString() + " CONTENT $content RETURN *, <string>id AS id";
        Map<String, Object> contentMap = MAPPER.convertValue(entity, Map.class);
        contentMap.remove("id");
        Map<String, Object> bindings = Map.of("content", contentMap);

        Value result = surreal.query(surql, bindings).take(0);
        if (result.isArray() && result.getArray().len() > 0) {
            return result.getArray().get(0).get(info.type());
        }
        return result.get(info.type());
    }

    /**
     * Run a parameterised SurQL query and map the first statement's result
     * set to {@code type}.
     *
     * @param type     domain class
     * @param surql    SurQL query string
     * @param bindings parameter bindings
     * @param <T>      result type
     * @return list of results
     */
    public <T> List<T> query(Class<T> type, String surql, Map<String, Object> bindings) {
        Response response = surreal.query(surql, bindings);
        Value result = response.take(0);
        List<T> out = new ArrayList<>();
        if (result.isArray()) {
            for (Value element : result.getArray()) {
                T entity = element.get(type);
                if (entity != null) {
                    out.add(entity);
                }
            }
        }
        return out;
    }

    /**
     * Deletes a single record by id.
     *
     * @param type the domain class
     * @param id   the record identifier (String or RecordId)
     */
    public void deleteById(Class<?> type, Object id) {
        SurrealEntityInformation<?, Object> info = new SurrealEntityInformation<>(type);
        surreal.delete(recordId(info.table(), id));
    }

    /**
     * Deletes all records from the table mapped to {@code type}.
     *
     * @param type the domain class
     */
    public void deleteAll(Class<?> type) {
        SurrealEntityInformation<?, Object> info = new SurrealEntityInformation<>(type);
        surreal.delete(info.table());
    }

    /**
     * Creates a bare graph edge using record identifiers (String or RecordId).
     *
     * <p>If passing Strings, they must be in the "table:id" format.
     */
    public <R extends Relation> R relate(Class<R> edgeType, Object from, Object to) {
        return relate(edgeType, from, to, null);
    }

    /**
     * Creates a graph edge with metadata using record identifiers.
     */
    public <R extends Relation, C> R relate(
            Class<R> edgeType, Object from, Object to, C content) {
        String edgeTable = SurrealEntityInformation.resolveEdgeTable(edgeType);
        return (content == null)
                ? surreal.relate(edgeType, parseRecordId(from), edgeTable, parseRecordId(to))
                : surreal.relate(edgeType, parseRecordId(from), edgeTable, parseRecordId(to), content);
    }

    /**
     * Creates a bare graph edge with explicit table names.
     */
    public <R extends Relation> R relate(
            Class<R> edgeType,
            Object fromId, String fromTable,
            Object toId, String toTable) {
        return relate(edgeType, fromId, fromTable, toId, toTable, null);
    }

    /**
     * Creates a graph edge with metadata and explicit table names.
     */
    public <R extends Relation, C> R relate(
            Class<R> edgeType,
            Object fromId, String fromTable,
            Object toId, String toTable,
            C content) {
        String edgeTable = SurrealEntityInformation.resolveEdgeTable(edgeType);
        RecordId from = recordId(fromTable, fromId);
        RecordId to = recordId(toTable, toId);
        return (content == null)
                ? surreal.relate(edgeType, from, edgeTable, to)
                : surreal.relate(edgeType, from, edgeTable, to, content);
    }

    /**
     * INSERT RELATION-style edge creation.
     *
     * <p>Delegates to {@link Surreal#insertRelation(Class, String, InsertRelation)}.
     * Use this when you need an explicit {@code id} on the edge record itself,
     * or when you prefer the INSERT semantics over RELATE.
     *
     * @param edgeType edge class (must extend {@link InsertRelation})
     * @param content  the relation object (must have {@code in} and {@code out} set)
     * @param <T>      edge type
     * @return the inserted edge deserialised into {@code edgeType}
     */
    public <T extends InsertRelation> T insertRelation(Class<T> edgeType, T content) {
        String edgeTable = SurrealEntityInformation.resolveEdgeTable(edgeType);
        return surreal.insertRelation(edgeType, edgeTable, content);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private static RecordId recordId(String table, Object id) {
        if (id instanceof RecordId rid) {
            return rid;
        }
        String s = String.valueOf(id);
        if (s.contains(":")) {
            String[] parts = s.split(":", 2);
            // If the table matches the prefix, use the remainder as the id
            if (parts[0].equals(table)) {
                return new RecordId(table, parts[1]);
            }
        }
        if (id instanceof Long l) {
            return new RecordId(table, l);
        }
        if (id instanceof Integer i) {
            return new RecordId(table, i.longValue());
        }
        return new RecordId(table, s);
    }

    private static RecordId parseRecordId(Object id) {
        if (id instanceof RecordId rid) {
            return rid;
        }
        String s = String.valueOf(id);
        if (s.contains(":")) {
            String[] parts = s.split(":", 2);
            return new RecordId(parts[0], parts[1]);
        }
        throw new IllegalArgumentException("Cannot parse RecordId from: " + id + ". Use the (table, id) overload instead.");
    }
}
