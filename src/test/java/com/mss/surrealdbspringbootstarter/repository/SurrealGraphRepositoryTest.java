package com.mss.surrealdbspringbootstarter.repository;

import com.mss.surrealdbspringbootstarter.core.SurrealTemplate;
import com.mss.surrealdbspringbootstarter.mapping.Relate;
import com.mss.surrealdbspringbootstarter.mapping.Table;
import com.surrealdb.RecordId;
import com.surrealdb.Relation;
import com.surrealdb.Surreal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.annotation.Id;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link SurrealGraphRepository} graph-edge operations
 * as implemented by {@link SimpleSurrealRepository}.
 */
@DisplayName("SurrealGraphRepository — graph edge operations via SimpleSurrealRepository")
class SurrealGraphRepositoryTest {

    private final Surreal surreal = mock(Surreal.class);
    private final SurrealTemplate template = new SurrealTemplate(surreal);
    private final SimpleSurrealRepository<Person, String> repo =
            new SimpleSurrealRepository<>(template, Person.class);

    @Nested
    @DisplayName("relate(edgeType, fromId, toTable, toId) — bare edge")
    class BareEdge {

        @Test
        @DisplayName("resolves the 'from' table from the repository's own domain type (@Table)")
        void resolvesFromTableFromDomainType() {
            Wrote expectedEdge = new Wrote();
            when(surreal.relate(eq(Wrote.class), any(RecordId.class), eq("wrote"), any(RecordId.class)))
                    .thenReturn(expectedEdge);

            repo.relate(Wrote.class, "alice", "article", "surreal");

            ArgumentCaptor<RecordId> fromCaptor = ArgumentCaptor.forClass(RecordId.class);
            verify(surreal).relate(
                    eq(Wrote.class),
                    fromCaptor.capture(),
                    eq("wrote"),
                    any(RecordId.class));
            assertThat(fromCaptor.getValue().getTable()).isEqualTo("person");
        }

        @Test
        @DisplayName("uses the caller-supplied toTable for the 'to' RecordId")
        void usesSuppliedToTable() {
            Wrote expectedEdge = new Wrote();
            when(surreal.relate(eq(Wrote.class), any(RecordId.class), eq("wrote"), any(RecordId.class)))
                    .thenReturn(expectedEdge);

            repo.relate(Wrote.class, "alice", "article", "surreal");

            ArgumentCaptor<RecordId> toCaptor = ArgumentCaptor.forClass(RecordId.class);
            verify(surreal).relate(
                    eq(Wrote.class),
                    any(RecordId.class),
                    eq("wrote"),
                    toCaptor.capture());
            assertThat(toCaptor.getValue().getTable()).isEqualTo("article");
        }

        @Test
        @DisplayName("resolves 'to' table from the provided target class")
        void resolvesToTableFromTargetClass() {
            Wrote expectedEdge = new Wrote();
            when(surreal.relate(eq(Wrote.class), any(RecordId.class), eq("wrote"), any(RecordId.class)))
                    .thenReturn(expectedEdge);

            // Article domain class maps to "article" table
            repo.relate(Wrote.class, "alice", Article.class, "surreal");

            ArgumentCaptor<RecordId> toCaptor = ArgumentCaptor.forClass(RecordId.class);
            verify(surreal).relate(
                    eq(Wrote.class),
                    any(RecordId.class),
                    eq("wrote"),
                    toCaptor.capture());
            assertThat(toCaptor.getValue().getTable()).isEqualTo("article");
        }

        @Test
        @DisplayName("returns the edge instance produced by SurrealTemplate")
        void returnsEdgeFromTemplate() {
            Wrote expectedEdge = new Wrote();
            when(surreal.relate(eq(Wrote.class), any(RecordId.class), eq("wrote"), any(RecordId.class)))
                    .thenReturn(expectedEdge);

            Wrote result = repo.relate(Wrote.class, "alice", "article", "surreal");

            assertThat(result).isSameAs(expectedEdge);
        }
    }

    @Nested
    @DisplayName("relate(edgeType, fromId, toTable, toId, content) — edge with metadata")
    class EdgeWithMetadata {

        @Test
        @DisplayName("passes content through to SurrealTemplate and returns the created edge")
        void passesContentThroughToTemplate() {
            Wrote meta = new Wrote();
            meta.since = "2025-01-01";
            Wrote expectedEdge = new Wrote();
            when(surreal.relate(eq(Wrote.class), any(RecordId.class), eq("wrote"), any(RecordId.class), eq(meta)))
                    .thenReturn(expectedEdge);

            Wrote result = repo.relate(Wrote.class, "alice", "article", "surreal", meta);

            assertThat(result).isSameAs(expectedEdge);
            verify(surreal).relate(
                    eq(Wrote.class),
                    any(RecordId.class),
                    eq("wrote"),
                    any(RecordId.class),
                    eq(meta));
        }

        @Test
        @DisplayName("resolves 'from' table from domain type even when content is supplied")
        void resolvesFromTableWithContent() {
            Wrote meta = new Wrote();
            Wrote expectedEdge = new Wrote();
            when(surreal.relate(eq(Wrote.class), any(RecordId.class), eq("wrote"), any(RecordId.class), any()))
                    .thenReturn(expectedEdge);

            repo.relate(Wrote.class, "alice", "article", "surreal", meta);

            ArgumentCaptor<RecordId> fromCaptor = ArgumentCaptor.forClass(RecordId.class);
            verify(surreal).relate(
                    eq(Wrote.class),
                    fromCaptor.capture(),
                    eq("wrote"),
                    any(RecordId.class),
                    any());
            assertThat(fromCaptor.getValue().getTable()).isEqualTo("person");
        }
    }

    @Nested
    @DisplayName("SurrealGraphRepository is a SurrealRepository")
    class HierarchyCheck {

        @Test
        @DisplayName("SimpleSurrealRepository implements SurrealGraphRepository")
        void simpleSurrealRepositoryImplementsGraphRepository() {
            assertThat(repo).isInstanceOf(SurrealGraphRepository.class);
        }

        @Test
        @DisplayName("SurrealGraphRepository extends SurrealRepository")
        void surrealGraphRepositoryExtendsSurrealRepository() {
            assertThat(SurrealRepository.class)
                    .isAssignableFrom(SurrealGraphRepository.class);
        }

        @Test
        @DisplayName("standard CRUD operations still work on a SurrealGraphRepository")
        void crudOperationsStillWorkOnGraphRepo() {
            Person p = new Person();
            p.id = "person:alice";
            com.surrealdb.Response mockResponse = mock(com.surrealdb.Response.class);
            com.surrealdb.Value mockValue = mock(com.surrealdb.Value.class);
            when(surreal.query(any(String.class))).thenReturn(mockResponse);
            when(mockResponse.take(0)).thenReturn(mockValue);
            when(mockValue.isNone()).thenReturn(false);
            when(mockValue.isNull()).thenReturn(false);
            when(mockValue.get(Person.class)).thenReturn(p);

            Optional<Person> found = repo.findById("alice");
            assertThat(found).containsSame(p);
            verify(surreal).query(contains("SELECT *"));
        }
    }

    // =========================================================================
    // Test fixtures
    // =========================================================================

    @Table("person")
    static class Person {
        @Id String id;
    }

    @Table("article")
    static class Article {
        @Id String id;
    }

    @Relate("wrote")
    static class Wrote extends Relation {
        String since;
    }
}
