package com.mss.surrealdbspringbootstarter;

import com.surrealdb.Surreal;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SurrealDbAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SurrealDbAutoConfiguration.class));

    @Test
    void autoConfigurationIsDisabledWithoutUrl() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(Surreal.class));
    }

    @Test
    void propertiesAreBound() {
        contextRunner
                .withPropertyValues(
                        "surrealdb.url=memory://",
                        "surrealdb.username=root",
                        "surrealdb.password=root",
                        "surrealdb.namespace=test",
                        "surrealdb.database=test")
                .withUserConfiguration(StubSurrealConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(SurrealDbProperties.class);
                    SurrealDbProperties props = context.getBean(SurrealDbProperties.class);
                    assertThat(props.getUrl()).isEqualTo("memory://");
                    assertThat(props.getUsername()).isEqualTo("root");
                    assertThat(props.getPassword()).isEqualTo("root");
                    assertThat(props.getNamespace()).isEqualTo("test");
                    assertThat(props.getDatabase()).isEqualTo("test");
                    // User-supplied Surreal bean wins over the auto-configured one.
                    assertThat(context).hasSingleBean(Surreal.class);
                });
    }

    /**
     * Provides a mock {@link Surreal} bean so the context can load without
     * triggering native Rust library loading from the real SDK during tests.
     */
    @Configuration
    static class StubSurrealConfig {
        @Bean
        Surreal surreal() {
            return mock(Surreal.class);
        }
    }
}
