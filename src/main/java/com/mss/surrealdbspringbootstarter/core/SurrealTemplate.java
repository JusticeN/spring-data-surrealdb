package com.mss.surrealdbspringbootstarter.core;

import com.mss.surrealdbspringbootstarter.mapping.SurrealEntityInformation;
import com.surrealdb.RecordId;
import com.surrealdb.Response;
import com.surrealdb.Surreal;
import com.surrealdb.UpType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Thin Spring Data–style facade over the {@link Surreal} client.
 *
 * <p>Provides table-name and id resolution via {@link SurrealEntityInformation}
 * so callers can work with annotated domain types instead of raw
 * {@link RecordId}s and table names.
 */
public class SurrealTemplate {

    private final Surreal surreal;

    public SurrealTemplate(Surreal surreal) {
        this.surreal = surreal;
    }

    /** @return the underlying {@link Surreal} client. */
    public Surreal getSurreal() {
        return surreal;
    }

    /**
     * Insert {@code entity} into its mapped table.
     * The id, if pre-assigned, is honoured by the server-side.
     */
    @SuppressWarnings("unchecked")
    public <T> T insert(T entity) {
        SurrealEntityInformation<T, Object> info =
                new SurrealEntityInformation<>((Class<T>) entity.getClass());
        List<T> created = surreal.create(info.type(), info.table(), entity);
        return created.isEmpty() ? entity : created.get(0);
    }

    /** Select a single record by id, returning empty if absent. */
    public <T, ID> Optional<T> findById(Class<T> type, ID id) {
        SurrealEntityInformation<T, ID> info = new SurrealEntityInformation<>(type);
        return surreal.select(type, recordId(info.table(), id));
    }

    /** Select every record from the table mapped to {@code type}. */
    public <T> List<T> findAll(Class<T> type) {
        SurrealEntityInformation<T, Object> info = new SurrealEntityInformation<>(type);
        List<T> out = new ArrayList<>();
        Iterator<T> it = surreal.select(type, info.table());
        while (it.hasNext()) {
            out.add(it.next());
        }
        return out;
    }

    /** Delete a record by id. */
    public <T, ID> void deleteById(Class<T> type, ID id) {
        SurrealEntityInformation<T, ID> info = new SurrealEntityInformation<>(type);
        surreal.delete(recordId(info.table(), id));
    }

    /** Delete every record from the table mapped to {@code type}. */
    public <T> void deleteAll(Class<T> type) {
        SurrealEntityInformation<T, Object> info = new SurrealEntityInformation<>(type);
        surreal.delete(info.table());
    }

    /**
     * Update an entity using SurrealDB's CONTENT semantics; the entity's id
     * field must be populated.
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
        return surreal.update(info.type(), recordId(info.table(), id), UpType.CONTENT, entity);
    }

    /**
     * Run a parameterised SurQL query and map the first statement's result
     * set to {@code type}.
     */
    public <T> T query(Class<T> type, String surql, Map<String, ?> bindings) {
        Response response = (bindings == null || bindings.isEmpty())
                ? surreal.query(surql)
                : surreal.query(surql, bindings);
        return response.take(type, 0);
    }

    private static RecordId recordId(String table, Object id) {
        if (id instanceof Long l) {
            return new RecordId(table, l);
        }
        if (id instanceof Integer i) {
            return new RecordId(table, i.longValue());
        }
        return new RecordId(table, String.valueOf(id));
    }
}
