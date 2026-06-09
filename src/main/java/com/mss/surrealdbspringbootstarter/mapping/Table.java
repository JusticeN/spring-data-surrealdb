package com.mss.surrealdbspringbootstarter.mapping;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a domain class as mapped to a SurrealDB table.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Table {

    /** SurrealDB table name. Defaults to the simple class name lowercased. */
    String value() default "";
}
