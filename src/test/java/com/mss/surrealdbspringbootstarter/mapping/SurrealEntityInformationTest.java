package com.mss.surrealdbspringbootstarter.mapping;

import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;

import static org.assertj.core.api.Assertions.assertThat;

class SurrealEntityInformationTest {

    @Test
    void defaultsToLowercasedSimpleClassName() {
        SurrealEntityInformation<Plain, String> info = new SurrealEntityInformation<>(Plain.class);
        assertThat(info.table()).isEqualTo("plain");
        assertThat(info.hasIdField()).isTrue();
    }

    @Test
    void honoursExplicitTableAnnotation() {
        SurrealEntityInformation<Person, String> info = new SurrealEntityInformation<>(Person.class);
        assertThat(info.table()).isEqualTo("person");
    }

    @Test
    void findsIdOnInheritedField() {
        SurrealEntityInformation<Child, String> info = new SurrealEntityInformation<>(Child.class);
        assertThat(info.hasIdField()).isTrue();
        Child c = new Child();
        info.setId(c, "abc");
        assertThat(info.getId(c)).isEqualTo("abc");
    }

    static class Plain {
        @Id String id;
    }

    @Table("person")
    static class Person {
        @Id String id;
    }

    static class Parent {
        @Id String id;
    }

    static class Child extends Parent {
    }
}
