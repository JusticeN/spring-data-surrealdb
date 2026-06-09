package com.mss.surrealdbspringbootstarter.mapping;

import com.surrealdb.RecordId;
import org.springframework.data.annotation.Id;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lightweight metadata holder for a SurrealDB-mapped domain type.
 *
 * <p>Resolves the target table name (from {@link Table} or the simple class
 * name lowercased) and the identifier field (annotated with
 * {@link org.springframework.data.annotation.Id}).
 *
 * <p>Also exposes:
 * <ul>
 *   <li>{@link #fetchFields()} — names of fields annotated with
 *       {@link Link @Link(fetch=true)} or {@link LinkList @LinkList(fetch=true)},
 *       used to build an automatic {@code FETCH} clause.</li>
 *   <li>{@link #resolveEdgeTable(Class)} — static utility that reads an
 *       {@link Relate @Relate} annotation to determine the edge-table name.</li>
 * </ul>
 *
 * @param <T>  the domain type
 * @param <ID> the identifier type
 */
public final class SurrealEntityInformation<T, ID> {

    private final Class<T> type;
    private final String table;
    private final Field idField;
    private final List<String> fetchFields;
    private final List<String> lazyLinkFields;
    private final List<String> lazyLinkListFields;
    private final boolean isStringId;

    public SurrealEntityInformation(Class<T> type) {
        this.type = type;
        Table t = type.getAnnotation(Table.class);
        this.table = (t != null && !t.value().isEmpty())
                ? t.value()
                : type.getSimpleName().toLowerCase();
        this.idField = findIdField(type);
        if (this.idField != null) {
            ReflectionUtils.makeAccessible(this.idField);
            this.isStringId = this.idField.getType().equals(String.class);
        } else {
            this.isStringId = false;
        }
        this.fetchFields = Collections.unmodifiableList(resolveFetchFields(type));
        this.lazyLinkFields = Collections.unmodifiableList(resolveLazyLinkFields(type));
        this.lazyLinkListFields = Collections.unmodifiableList(resolveLazyLinkListFields(type));
    }

    public boolean isStringId() {
        return isStringId;
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

    /**
     * Returns the names of fields that should appear in an automatic
     * {@code FETCH} clause when querying this entity type.
     *
     * <p>A field is included when it carries {@code @Link(fetch = true)} or
     * {@code @LinkList(fetch = true)}.  Returns an empty list when no such
     * fields are present, indicating that the standard {@code select()} path
     * should be used (no FETCH clause is added).
     *
     * @return immutable, ordered list of field names; never {@code null}
     */
    public List<String> fetchFields() {
        return fetchFields;
    }

    /**
     * Returns the names of single-value link fields where {@code fetch = false}
     * but the field type is a domain object (not {@code RecordId}).
     * These require a SurQL projection to return a partial object.
     */
    public List<String> lazyLinkFields() {
        return lazyLinkFields;
    }

    /**
     * Returns the names of collection link fields where {@code fetch = false}
     * but the element type is a domain object (not {@code RecordId}).
     */
    public List<String> lazyLinkListFields() {
        return lazyLinkListFields;
    }

    /**
     * Resolves the edge-table name for the given edge class.
     *
     * <p>Reads the {@link Relate @Relate} annotation value; when absent or
     * empty falls back to the simple class name lowercased — the same
     * convention used by {@link Table @Table}.
     *
     * @param edgeClass the edge domain class (typically extends
     *                  {@link com.surrealdb.Relation})
     * @return the resolved edge-table name; never {@code null}
     */
    public static String resolveEdgeTable(Class<?> edgeClass) {
        Relate r = edgeClass.getAnnotation(Relate.class);
        if (r != null && !r.value().isEmpty()) {
            return r.value();
        }
        return edgeClass.getSimpleName().toLowerCase();
    }

    // -------------------------------------------------------------------------
    // private helpers
    // -------------------------------------------------------------------------

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

    private static List<String> resolveFetchFields(Class<?> type) {
        List<String> names = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                Link link = f.getAnnotation(Link.class);
                if (link != null && link.fetch()) {
                    names.add(f.getName());
                    continue;
                }
                LinkList linkList = f.getAnnotation(LinkList.class);
                if (linkList != null && linkList.fetch()) {
                    names.add(f.getName());
                }
            }
        }
        return names;
    }

    private static List<String> resolveLazyLinkFields(Class<?> type) {
        List<String> names = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                Link link = f.getAnnotation(Link.class);
                if (link != null && !link.fetch() && !f.getType().equals(RecordId.class)) {
                    names.add(f.getName());
                }
            }
        }
        return names;
    }

    private static List<String> resolveLazyLinkListFields(Class<?> type) {
        List<String> names = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                LinkList linkList = f.getAnnotation(LinkList.class);
                if (linkList != null && !linkList.fetch() && !f.getType().equals(RecordId.class)) {
                    names.add(f.getName());
                }
            }
        }
        return names;
    }
}
