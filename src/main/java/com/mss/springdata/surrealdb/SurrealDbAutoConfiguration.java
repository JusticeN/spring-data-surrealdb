package com.mss.springdata.surrealdb;

import com.mss.springdata.surrealdb.core.SurrealTemplate;
import com.surrealdb.Surreal;
import com.surrealdb.signin.RootCredential;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

/**
 * Auto-configuration for SurrealDB.
 *
 * <p>Activates when the {@link Surreal} client is on the classpath and a
 * {@code surrealdb.url} property is provided. Creates a single, connected
 * {@link Surreal} bean. If credentials are supplied, the connection is signed
 * in as root. If a namespace and/or database are supplied, they are selected.
 */
@AutoConfiguration
@ConditionalOnClass(Surreal.class)
@EnableConfigurationProperties(SurrealDbProperties.class)
public class SurrealDbAutoConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "surrealdb", name = "url")
    public Surreal surreal(SurrealDbProperties properties) {
        Surreal surreal = new Surreal();
        surreal.connect(properties.getUrl());

        if (StringUtils.hasText(properties.getUsername())
                && StringUtils.hasText(properties.getPassword())) {
            surreal.signin(new RootCredential(properties.getUsername(), properties.getPassword()));
        }

        if (StringUtils.hasText(properties.getNamespace())) {
            surreal.useNs(properties.getNamespace());
        }

        if (StringUtils.hasText(properties.getDatabase())) {
            surreal.useDb(properties.getDatabase());
        }

        return surreal;
    }

    /**
     * Spring Data–style template wrapper. Registered whenever a {@link Surreal}
     * bean is available (auto-configured or user-supplied).
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(Surreal.class)
    public SurrealTemplate surrealTemplate(Surreal surreal) {
        return new SurrealTemplate(surreal);
    }
}
