package com.mss.springdata.surrealdb.repository;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables classpath scanning for interfaces that extend
 * {@link SurrealRepository} and registers a dynamic proxy bean for each.
 *
 * <p>If {@link #basePackages()} is empty, the package of the annotated
 * configuration class is used.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(SurrealRepositoriesRegistrar.class)
public @interface EnableSurrealRepositories {

    /** Base packages to scan. Defaults to the package of the annotated class. */
    String[] basePackages() default {};
}
