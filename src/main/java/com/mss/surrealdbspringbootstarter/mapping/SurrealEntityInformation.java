package com.mss.surrealdbspringbootstarter.mapping;

import org.springframework.data.annotation.Id;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

/**
 * Lightweight metadata holder for a SurrealDB-mapped domain type.
 *
 * <p>Resolves the target table name (from {@link Table} or the simple class
 * name lowercased) and the identifier field (annotated with
 * {@link org.springframework.data.annotation.Id}).
 *
 * @param <T>  the domain type
 * @param <ID> the identifier type
 */
public final class SurrealEntityInformation<T, ID> {

    private final Class<T> type;
    private final String table;
    private final Field idField;

    public SurrealEntityInformation(Class<T> type) {
        this.type = type;
        Table t = type.getAnnotation(Table.class);
        this.table = (t != null && !t.value().isEmpty())
                ? t.value()
                : type.getSimpleName().toLowerCase();
        this.idField = findIdField(type);
        if (this.idField != null) {
            ReflectionUtils.makeAccessible(this.idField);
        }
    }

    public String table() {
        return table;
    }

    public Class<T> type() {
        return type;
    }

    public boolean hasIdField() {
        return idField != null;
    }

    @SuppressWarnings("unchecked")
    public ID getId(T entity) {
        if (idField == null || entity == null) {
            return null;
        }
        return (ID) ReflectionUtils.getField(idField, entity);
    }

    public void setId(T entity, ID id) {
        if (idField != null && entity != null) {
            ReflectionUtils.setField(idField, entity, id);
        }
    }

    private static Field findIdField(Class<?> type) {
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (f.isAnnotationPresent(Id.class)) {
                    return f;
                }
            }
        }
        return null;
    }
}
