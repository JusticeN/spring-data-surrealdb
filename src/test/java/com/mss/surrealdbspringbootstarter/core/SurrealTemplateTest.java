package com.mss.surrealdbspringbootstarter.core;

import com.mss.surrealdbspringbootstarter.mapping.Table;
import com.surrealdb.RecordId;
import com.surrealdb.Surreal;
import com.surrealdb.UpType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.annotation.Id;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SurrealTemplateTest {

    private final Surreal surreal = mock(Surreal.class);
    private final SurrealTemplate template = new SurrealTemplate(surreal);

    @Test
    void insertDelegatesToCreateWithResolvedTable() {
        Person p = new Person();
        p.name = "Ada";
        when(surreal.create(eq(Person.class), eq("person"), any(Person.class)))
                .thenReturn(List.of(p));

        Person saved = template.insert(p);

        assertThat(saved).isSameAs(p);
        verify(surreal).create(eq(Person.class), eq("person"), any(Person.class));
    }

    @Test
    void findByIdUsesRecordIdWithMappedTable() {
        Person p = new Person();
        when(surreal.select(eq(Person.class), any(RecordId.class))).thenReturn(Optional.of(p));

        Optional<Person> found = template.findById(Person.class, "ada");

        assertThat(found).containsSame(p);
        ArgumentCaptor<RecordId> captor = ArgumentCaptor.forClass(RecordId.class);
        verify(surreal).select(eq(Person.class), captor.capture());
        assertThat(captor.getValue().getTable()).isEqualTo("person");
    }

    @Test
    void deleteByIdDelegatesToSurrealDelete() {
        template.deleteById(Person.class, "ada");
        verify(surreal).delete(any(RecordId.class));
    }

    @Test
    void updateRequiresIdAndUsesContent() {
        Person p = new Person();
        p.id = "ada";
        when(surreal.update(eq(Person.class), any(RecordId.class), eq(UpType.CONTENT), any()))
                .thenReturn(p);

        Person updated = template.update(p);
        assertThat(updated).isSameAs(p);
    }

    @Test
    void deleteAllDelegatesToTableLevelDelete() {
        template.deleteAll(Person.class);
        verify(surreal).delete(eq("person"));
    }

    @Table("person")
    static class Person {
        @Id String id;
        String name;
    }
}
