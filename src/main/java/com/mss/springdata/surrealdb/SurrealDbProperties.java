package com.mss.springdata.surrealdb;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the SurrealDB Spring Boot starter.
 *
 * <p>Bound under the {@code surrealdb} prefix, e.g.:
 * <pre>
 * surrealdb:
 *   url: ws://localhost:8000
 *   username: root
 *   password: root
 *   namespace: test
 *   database: test
 * </pre>
 */
@ConfigurationProperties(prefix = "surrealdb")
public class SurrealDbProperties {

    /**
     * Connection URL. Supported schemes: {@code ws://}, {@code wss://},
     * {@code http://}, {@code https://}, {@code memory://}, {@code surrealkv://}.
     */
    private String url;

    /** Username for root-level sign-in. Optional. */
    private String username;

    /** Password for root-level sign-in. Optional. */
    private String password;

    /** Namespace to switch to after connecting. Optional. */
    private String namespace;

    /** Database to switch to after connecting. Optional. */
    private String database;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }
}
