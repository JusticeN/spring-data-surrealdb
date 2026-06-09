package com.mss.springdata.surrealdb.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Marker base interface for SurrealDB-backed repositories.
 *
 * @param <T>  the domain type the repository manages
 * @param <ID> the type of the domain's identifier
 */
@NoRepositoryBean
public interface SurrealRepository<T, ID> extends CrudRepository<T, ID> {
}
