package com.mss.springdata.surrealdb.core;

import com.mss.springdata.surrealdb.mapping.Relate;
import com.mss.springdata.surrealdb.mapping.Table;
import com.surrealdb.RecordId;
import com.surrealdb.Relation;
import com.surrealdb.Surreal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("SurrealTemplate")
class SurrealTemplateTest {

    private final Surreal surreal = mock(Surreal.class);
    private final SurrealTemplate template = new SurrealTemplate(surreal);

    // -------------------------------------------------------------------------
    // CRUD (pre-existing behaviour)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("insert()")
    class Insert {

        @Test
        @DisplayName("delegates to Surreal.query() when entity has String @Id")
        void insertDelegatesToQueryForStringId() {
            Person p = new Person();
            p.name = "Ada";
            com.surrealdb.Response mockResponse = mock(com.surrealdb.Response.class);
            com.surrealdb.Value mockValue = mock(com.surrealdb.Value.class);
            when(surreal.query(any(String.class), any(java.util.Map.class))).thenReturn(mockResponse);
            when(mockResponse.take(0)).thenReturn(mockValue);
            when(mockValue.get(Person.class)).thenReturn(p);

            Person saved = template.insert(p);

            assertThat(saved).isSameAs(p);
            verify(surreal).query(contains("CREATE person CONTENT"), any(java.util.Map.class));
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("uses Surreal.query() when entity has String @Id")
        void findByIdUsesQueryForStringId() {
            Person p = new Person();
            com.surrealdb.Response mockResponse = mock(com.surrealdb.Response.class);
            com.surrealdb.Value mockValue = mock(com.surrealdb.Value.class);
            when(surreal.query(any(String.class))).thenReturn(mockResponse);
            when(mockResponse.take(0)).thenReturn(mockValue);
            when(mockValue.isNone()).thenReturn(false);
            when(mockValue.isNull()).thenReturn(false);
            when(mockValue.get(Person.class)).thenReturn(p);

            Optional<Person> found = template.findById(Person.class, "ada");

            assertThat(found).containsSame(p);
            verify(surreal).query(contains("SELECT *"));
            verify(surreal).query(contains("<string>id AS id"));
        }
    }

    @Nested
    @DisplayName("deleteById()")
    class DeleteById {

        @Test
        @DisplayName("delegates to Surreal.delete() with a RecordId")
        void deleteByIdDelegatesToSurrealDelete() {
            template.deleteById(Person.class, "ada");
            verify(surreal).delete(any(RecordId.class));
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("uses Surreal.query() when entity has String @Id")
        void updateUsesQueryForStringId() {
            Person p = new Person();
            p.id = "person:ada";
            com.surrealdb.Response mockResponse = mock(com.surrealdb.Response.class);
            com.surrealdb.Value mockValue = mock(com.surrealdb.Value.class);
            when(surreal.query(any(String.class), any(java.util.Map.class))).thenReturn(mockResponse);
            when(mockResponse.take(0)).thenReturn(mockValue);
            when(mockValue.get(Person.class)).thenReturn(p);

            Person updated = template.update(p);
            assertThat(updated).isSameAs(p);
            verify(surreal).query(contains("UPDATE person:ada CONTENT"), any(java.util.Map.class));
            verify(surreal).query(contains("<string>id AS id"), any(java.util.Map.class));
        }

        @Test
        @DisplayName("throws IllegalArgumentException when the @Id field is null")
        void updateThrowsWhenIdIsNull() {
            Person p = new Person(); // id is null
            assertThatThrownBy(() -> template.update(p))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("without an @Id value");
        }
    }

    @Nested
    @DisplayName("deleteAll()")
    class DeleteAll {

        @Test
        @DisplayName("delegates to table-level Surreal.delete(String)")
        void deleteAllDelegatesToTableLevelDelete() {
            template.deleteAll(Person.class);
            verify(surreal).delete(eq("person"));
        }
    }

    // =========================================================================
    // Test fixtures
    // =========================================================================

    @Table("person")
    static class Person {
        @Id String id;
        String name;
    }

    @Relate("wrote")
    static class Wrote extends Relation {
        String since;
    }
}
