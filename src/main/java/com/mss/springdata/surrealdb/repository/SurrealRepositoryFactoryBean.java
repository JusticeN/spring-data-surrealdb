package com.mss.springdata.surrealdb.repository;

import com.mss.springdata.surrealdb.core.SurrealTemplate;

import org.springframework.beans.factory.FactoryBean;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;

/**
 * {@link FactoryBean} that materialises a user-defined repository interface
 * extending {@link SurrealRepository} as a JDK dynamic proxy backed by a
 * {@link SimpleSurrealRepository}.
 *
 * <p>The repository's domain type is inferred from the
 * {@code SurrealRepository<T, ID>} type parameter of the interface.
 */
public class SurrealRepositoryFactoryBean<T> implements FactoryBean<T> {

    private final Class<T> repositoryInterface;
    private final SurrealTemplate template;

    public SurrealRepositoryFactoryBean(Class<T> repositoryInterface, SurrealTemplate template) {
        this.repositoryInterface = repositoryInterface;
        this.template = template;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public T getObject() {
        Class<?> domainType = resolveDomainType(repositoryInterface);
        SimpleSurrealRepository delegate = new SimpleSurrealRepository<>(template, domainType);
        InvocationHandler handler = (proxy, method, args) -> {
            try {
                return method.invoke(delegate, args);
            } catch (java.lang.reflect.InvocationTargetException ex) {
                throw ex.getTargetException();
            } catch (IllegalArgumentException ex) {
                throw new UnsupportedOperationException(
                        "Method " + method + " is not supported by SimpleSurrealRepository", ex);
            }
        };
        return (T) Proxy.newProxyInstance(
                repositoryInterface.getClassLoader(),
                new Class<?>[]{repositoryInterface},
                handler);
    }

    @Override
    public Class<?> getObjectType() {
        return repositoryInterface;
    }

    private static Class<?> resolveDomainType(Class<?> repositoryInterface) {
        for (Type t : repositoryInterface.getGenericInterfaces()) {
            if (t instanceof ParameterizedType pt
                    && SurrealRepository.class.isAssignableFrom(asClass(pt.getRawType()))) {
                Type[] args = pt.getActualTypeArguments();
                if (args.length >= 1 && args[0] instanceof Class<?> c) {
                    return c;
                }
            }
        }
        // Walk up super-interfaces.
        for (Class<?> sup : repositoryInterface.getInterfaces()) {
            Class<?> resolved = resolveDomainType(sup);
            if (resolved != null) {
                return resolved;
            }
        }
        throw new IllegalStateException(
                "Could not resolve domain type for " + repositoryInterface.getName());
    }

    private static Class<?> asClass(Type t) {
        return (t instanceof Class<?> c) ? c : Object.class;
    }

    // Suppress unused-method warning on Method import.
    @SuppressWarnings("unused")
    private static void unused(Method m) {
    }
}
