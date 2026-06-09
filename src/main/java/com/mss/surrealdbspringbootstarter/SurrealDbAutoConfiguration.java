package com.mss.surrealdbspringbootstarter;

import com.surrealdb.Surreal;
import com.surrealdb.signin.RootCredential;
import org.springframework.boot.autoconfigure.AutoConfiguration;
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
@ConditionalOnProperty(prefix = "surrealdb", name = "url")
@EnableConfigurationProperties(SurrealDbProperties.class)
public class SurrealDbAutoConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
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
}
