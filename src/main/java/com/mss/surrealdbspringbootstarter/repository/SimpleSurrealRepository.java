package com.mss.surrealdbspringbootstarter.repository;

import com.mss.surrealdbspringbootstarter.core.SurrealTemplate;
import com.mss.surrealdbspringbootstarter.mapping.SurrealEntityInformation;
import com.surrealdb.Relation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Default, generic {@link SurrealGraphRepository} implementation that delegates
 * all CRUD and graph operations to a {@link SurrealTemplate}.
 *
 * <p>One instance is created per repository interface by the
 * {@code SurrealRepositoryFactoryBean}.
 */
public class SimpleSurrealRepository<T, ID> implements SurrealGraphRepository<T, ID> {

    private final SurrealTemplate template;
    private final Class<T> domainType;
    private final SurrealEntityInformation<T, ID> entityInformation;

    public SimpleSurrealRepository(SurrealTemplate template, Class<T> domainType) {
        this.template = template;
        this.domainType = domainType;
        this.entityInformation = new SurrealEntityInformation<>(domainType);
    }

    // -------------------------------------------------------------------------
    // CrudRepository
    // -------------------------------------------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public <S extends T> S save(S entity) {
        ID id = entityInformation.getId((T) entity);
        if (id != null && template.findById(domainType, id).isPresent()) {
            return (S) template.update((T) entity);
        }
        return (S) template.insert((T) entity);
    }

    @Override
    public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {
        List<S> out = new ArrayList<>();
        for (S e : entities) {
            out.add(save(e));
        }
        return out;
    }

    @Override
    public Optional<T> findById(ID id) {
        return template.findById(domainType, id);
    }

    @Override
    public boolean existsById(ID id) {
        return findById(id).isPresent();
    }

    @Override
    public Iterable<T> findAll() {
        return template.findAll(domainType);
    }

    @Override
    public Iterable<T> findAllById(Iterable<ID> ids) {
        List<T> out = new ArrayList<>();
        for (ID id : ids) {
            findById(id).ifPresent(out::add);
        }
        return out;
    }

    @Override
    public long count() {
        return template.findAll(domainType).size();
    }

    @Override
    public void deleteById(ID id) {
        template.deleteById(domainType, id);
    }

    @Override
    public void delete(T entity) {
        ID id = entityInformation.getId(entity);
        if (id != null) {
            deleteById(id);
        }
    }

    @Override
    public void deleteAllById(Iterable<? extends ID> ids) {
        for (ID id : ids) {
            deleteById(id);
        }
    }

    @Override
    public void deleteAll(Iterable<? extends T> entities) {
        for (T e : entities) {
            delete(e);
        }
    }

    @Override
    public void deleteAll() {
        template.deleteAll(domainType);
    }

    // -------------------------------------------------------------------------
    // SurrealGraphRepository — graph edges
    // -------------------------------------------------------------------------

    @Override
    public <R extends Relation> R relate(
            Class<R> edgeType, ID fromId, String toTable, Object toId) {
        return template.relate(edgeType,
                fromId, entityInformation.table(),
                toId,   toTable);
    }

    @Override
    public <R extends Relation, C> R relate(
            Class<R> edgeType, ID fromId, String toTable, Object toId, C content) {
        return template.relate(edgeType,
                fromId, entityInformation.table(),
                toId,   toTable,
                content);
    }
}
