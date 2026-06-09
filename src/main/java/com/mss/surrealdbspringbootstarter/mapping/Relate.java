package com.mss.surrealdbspringbootstarter.mapping;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a domain class as a SurrealDB graph-edge (relation) table.
 *
 * <p>The annotated class should extend {@link com.surrealdb.Relation} so that
 * the SDK can automatically deserialise the {@code id}, {@code in}, and
 * {@code out} fields returned by the server.  Additional fields defined on the
 * class are treated as edge metadata (e.g. {@code since}, {@code weight}).
 *
 * <p>Usage:
 * <pre>
 * {@literal @}Relate("wrote")
 * public class Wrote extends Relation {
 *     public Instant since;
 * }
 * </pre>
 *
 * <p>If {@link #value()} is left empty the edge table name defaults to the
 * simple class name converted to lower-case, e.g. class {@code Wrote} maps to
 * table {@code wrote}.
 *
 * @see com.mss.surrealdbspringbootstarter.core.SurrealTemplate#relate(Class, Object, String, Object, String)
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Relate {

    /**
     * SurrealDB edge-table name.
     * Defaults to the annotated class's simple name lowercased.
     */
    String value() default "";
}
