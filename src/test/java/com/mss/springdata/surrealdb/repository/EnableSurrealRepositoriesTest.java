package com.mss.springdata.surrealdb.repository;

import com.mss.springdata.surrealdb.core.SurrealTemplate;
import com.mss.springdata.surrealdb.mapping.Table;
import com.surrealdb.Surreal;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class EnableSurrealRepositoriesTest {

    @Test
    void registersProxyForUserRepositoryInterface() {
        try (AnnotationConfigApplicationContext ctx =
                     new AnnotationConfigApplicationContext(Config.class)) {

            PersonRepository repo = ctx.getBean(PersonRepository.class);
            assertThat(repo).isNotNull();
            assertThat(java.lang.reflect.Proxy.isProxyClass(repo.getClass())).isTrue();
        }
    }

    @Configuration
    @EnableSurrealRepositories(basePackages = "com.mss.surrealdbspringbootstarter.repository")
    static class Config {

        @Bean
        Surreal surreal() {
            return mock(Surreal.class);
        }

        @Bean
        SurrealTemplate surrealTemplate(Surreal surreal) {
            return new SurrealTemplate(surreal);
        }
    }

    @Table("person")
    public static class Person {
        @Id public String id;
    }

    public interface PersonRepository extends SurrealRepository<Person, String> {
    }
}
