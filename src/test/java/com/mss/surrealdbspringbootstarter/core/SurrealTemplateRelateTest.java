package com.mss.surrealdbspringbootstarter.core;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SurrealTemplate#relate} overloads.
 *
 * <p>All tests use a Mockito mock for {@link Surreal} to verify that the
 * correct native SDK methods are called with the right arguments.
 */
@DisplayName("SurrealTemplate — graph edge operations (RELATE)")
class SurrealTemplateRelateTest {

    private final Surreal surreal = mock(Surreal.class);
    private final SurrealTemplate template = new SurrealTemplate(surreal);

    @Nested
    @DisplayName("relate(edgeType, fromId, fromTable, toId, toTable) — bare edge")
    class BareEdgeWithTableNames {

        @Test
        @DisplayName("resolves the edge table from the @Relate annotation")
        void resolvesEdgeTableFromAnnotation() {
            Wrote expectedEdge = new Wrote();
            when(surreal.relate(eq(Wrote.class), any(RecordId.class), eq("wrote"), any(RecordId.class)))
                    .thenReturn(expectedEdge);

            Wrote result = template.relate(Wrote.class, "alice", "person", "surreal", "article");

            assertThat(result).isSameAs(expectedEdge);
            verify(surreal).relate(eq(Wrote.class), any(RecordId.class), eq("wrote"), any(RecordId.class));
        }

        @Test
        @DisplayName("builds the 'from' RecordId with the supplied fromTable")
        void buildsFromRecordIdWithCorrectTable() {
            when(surreal.relate(eq(Wrote.class), any(RecordId.class), any(), any(RecordId.class)))
                    .thenReturn(new Wrote());

            template.relate(Wrote.class, "alice", "person", "surreal", "article");

            ArgumentCaptor<RecordId> fromCaptor = ArgumentCaptor.forClass(RecordId.class);
            // from is first RecordId argument
            verify(surreal).relate(eq(Wrote.class), fromCaptor.capture(), any(), any(RecordId.class));
            assertThat(fromCaptor.getValue().getTable()).isEqualTo("person");
        }

        @Test
        @DisplayName("builds the 'to' RecordId with the supplied toTable")
        void buildsToRecordIdWithCorrectTable() {
            when(surreal.relate(eq(Wrote.class), any(RecordId.class), any(), any(RecordId.class)))
                    .thenReturn(new Wrote());

            template.relate(Wrote.class, "alice", "person", "surreal", "article");

            ArgumentCaptor<RecordId> toCaptor = ArgumentCaptor.forClass(RecordId.class);
            // to is the last RecordId argument
            verify(surreal).relate(eq(Wrote.class), any(RecordId.class), any(), toCaptor.capture());
            assertThat(toCaptor.getValue().getTable()).isEqualTo("article");
        }

        @Test
        @DisplayName("falls back to lowercased class name when @Relate value is empty")
        void fallsBackToClassNameWhenRelateValueEmpty() {
            Follows expectedEdge = new Follows();
            when(surreal.relate(eq(Follows.class), any(RecordId.class), eq("follows"), any(RecordId.class)))
                    .thenReturn(expectedEdge);

            Follows result = template.relate(Follows.class, "alice", "person", "bob", "person");

            assertThat(result).isSameAs(expectedEdge);
            verify(surreal).relate(eq(Follows.class), any(RecordId.class), eq("follows"), any(RecordId.class));
        }
    }

    @Nested
    @DisplayName("relate(edgeType, fromId, fromTable, toId, toTable, content) — edge with metadata")
    class EdgeWithMetadataAndTableNames {

        @Test
        @DisplayName("passes content to the underlying Surreal.relate(Class,RecordId,String,RecordId,T)")
        void passesContentToUnderlyingRelate() {
            Wrote meta = new Wrote();
            meta.since = "2025-01-01";
            Wrote expectedEdge = new Wrote();
            when(surreal.relate(eq(Wrote.class), any(RecordId.class), eq("wrote"), any(RecordId.class), eq(meta)))
                    .thenReturn(expectedEdge);

            Wrote result = template.relate(Wrote.class, "alice", "person", "surreal", "article", meta);

            assertThat(result).isSameAs(expectedEdge);
            verify(surreal).relate(
                    eq(Wrote.class),
                    any(RecordId.class),
                    eq("wrote"),
                    any(RecordId.class),
                    eq(meta));
        }
    }

    @Nested
    @DisplayName("relate(edgeType, RecordId from, RecordId to) — pre-built RecordIds, bare edge")
    class BareEdgeWithPrebuiltRecordIds {

        @Test
        @DisplayName("delegates directly to Surreal.relate(Class,RecordId,String,RecordId)")
        void delegatesToSurrealRelate() {
            RecordId from = new RecordId("person", "alice");
            RecordId to   = new RecordId("article", "surreal");
            Wrote expectedEdge = new Wrote();
            when(surreal.relate(eq(Wrote.class), eq(from), eq("wrote"), eq(to)))
                    .thenReturn(expectedEdge);

            Wrote result = template.relate(Wrote.class, from, to);

            assertThat(result).isSameAs(expectedEdge);
        }
    }

    @Nested
    @DisplayName("relate(edgeType, RecordId from, RecordId to, content) — pre-built RecordIds with metadata")
    class EdgeWithMetadataAndPrebuiltRecordIds {

        @Test
        @DisplayName("delegates to Surreal.relate(Class,RecordId,String,RecordId,T) with the content")
        void delegatesToSurrealRelateWithContent() {
            RecordId from = new RecordId("person", "alice");
            RecordId to   = new RecordId("article", "surreal");
            Wrote meta = new Wrote();
            meta.since = "2025-06-01";
            Wrote expectedEdge = new Wrote();
            when(surreal.relate(eq(Wrote.class), eq(from), eq("wrote"), eq(to), eq(meta)))
                    .thenReturn(expectedEdge);

            Wrote result = template.relate(Wrote.class, from, to, meta);

            assertThat(result).isSameAs(expectedEdge);
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

    @Relate("follows")
    static class Follows extends Relation {
    }
}
