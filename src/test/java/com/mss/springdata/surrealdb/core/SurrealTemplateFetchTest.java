package com.mss.springdata.surrealdb.core;

import com.mss.springdata.surrealdb.mapping.Link;
import com.mss.springdata.surrealdb.mapping.LinkList;
import com.mss.springdata.surrealdb.mapping.Table;
import com.surrealdb.Array;
import com.surrealdb.RecordId;
import com.surrealdb.Response;
import com.surrealdb.Surreal;
import com.surrealdb.Value;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.annotation.Id;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the FETCH-aware {@link SurrealTemplate#findById} and
 * {@link SurrealTemplate#findAll} paths introduced by the
 * {@link Link @Link} / {@link LinkList @LinkList} record-link feature.
 */
@DisplayName("SurrealTemplate — record-link FETCH clause generation")
class SurrealTemplateFetchTest {

    private final Surreal surreal = mock(Surreal.class);
    private final SurrealTemplate template = new SurrealTemplate(surreal);

    // -------------------------------------------------------------------------
    // findById
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("findById() — fetch path selection")
    class FindById {

        @Test
        @DisplayName("uses Surreal.select(RecordId) when entity has RecordId @Id and no fetch fields")
        void usesSelectWhenRecordIdAndNoFetchFields() {
            Person p = new Person();
            when(surreal.select(eq(Person.class), any(RecordId.class)))
                    .thenReturn(Optional.of(p));

            template.findById(Person.class, "alice");

            verify(surreal).select(eq(Person.class), any(RecordId.class));
            verify(surreal, never()).query(anyString());
        }

        @Test
        @DisplayName("issues a projection query when entity has String @Id")
        void issuesProjectionQueryForStringId() {
            Value emptyArray = mockArrayValue();
            Response mockResponse = mock(Response.class);
            when(mockResponse.take(0)).thenReturn(emptyArray);
            when(surreal.query(anyString())).thenReturn(mockResponse);

            template.findById(PersonWithStringId.class, "alice");

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(surreal).query(sqlCaptor.capture());
            assertThat(sqlCaptor.getValue())
                    .contains("<string>id AS id")
                    .contains("FROM person:alice");
        }

        @Test
        @DisplayName("issues a SELECT…FETCH query when entity has @Link(fetch=true)")
        void issuesFetchQueryWhenLinkFetchTrue() {
            // Arrange mock response: empty array (no record found)
            Value emptyArray = mockArrayValue();
            Response mockResponse = mock(Response.class);
            when(mockResponse.take(0)).thenReturn(emptyArray);
            when(surreal.query(anyString())).thenReturn(mockResponse);

            template.findById(PersonWithEagerLink.class, "alice");

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(surreal).query(sqlCaptor.capture());
            assertThat(sqlCaptor.getValue())
                    .containsIgnoringCase("FETCH")
                    .contains("company");
        }

        @Test
        @DisplayName("includes all @Link(fetch=true) field names in FETCH clause")
        void includesAllFetchFieldNamesInQuery() {
            Value emptyArray = mockArrayValue();
            Response mockResponse = mock(Response.class);
            when(mockResponse.take(0)).thenReturn(emptyArray);
            when(surreal.query(anyString())).thenReturn(mockResponse);

            template.findById(PersonWithMultipleLinks.class, "alice");

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(surreal).query(sqlCaptor.capture());
            String sql = sqlCaptor.getValue();
            assertThat(sql)
                    .contains("FETCH")
                    .contains("company")
                    .contains("friends");
        }

        @Test
        @DisplayName("returns empty Optional when FETCH query result array is empty")
        void returnsEmptyOptionalWhenNoResults() {
            Value emptyArray = mockArrayValue(); // zero-length array
            Response mockResponse = mock(Response.class);
            when(mockResponse.take(0)).thenReturn(emptyArray);
            when(surreal.query(anyString())).thenReturn(mockResponse);
            when(surreal.query(anyString(), anyMap())).thenReturn(mockResponse);

            Optional<PersonWithEagerLink> result =
                    template.findById(PersonWithEagerLink.class, "missing");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("excludes @Link(fetch=false) fields from the FETCH clause")
        void excludesLazyLinkFields() {
            Value emptyArray = mockArrayValue();
            Response mockResponse = mock(Response.class);
            when(mockResponse.take(0)).thenReturn(emptyArray);
            when(surreal.query(anyString())).thenReturn(mockResponse);
            when(surreal.query(anyString(), anyMap())).thenReturn(mockResponse);

            template.findById(PersonWithMixedLinks.class, "alice");

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(surreal).query(sqlCaptor.capture());
            String sql = sqlCaptor.getValue();
            assertThat(sql).contains("FETCH company");
            assertThat(sql.substring(sql.indexOf("FETCH"))).doesNotContain("manager");
        }
    }

    // -------------------------------------------------------------------------
    // findAll
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("findAll() — fetch path selection")
    class FindAll {

        @Test
        @DisplayName("uses Surreal.select(String table) when no @Link(fetch=true) fields exist")
        void usesSelectWhenNoFetchFields() {
            @SuppressWarnings("unchecked")
            Iterator<Person> emptyIter = mock(Iterator.class);
            when(emptyIter.hasNext()).thenReturn(false);
            when(surreal.select(eq(Person.class), eq("person"))).thenReturn(emptyIter);

            template.findAll(Person.class);

            verify(surreal).select(eq(Person.class), eq("person"));
            verify(surreal, never()).query(anyString());
        }

        @Test
        @DisplayName("issues a SELECT…FETCH query when entity has @LinkList(fetch=true)")
        void issuesFetchQueryWhenLinkListFetchTrue() {
            Value emptyArray = mockArrayValue();
            Response mockResponse = mock(Response.class);
            when(mockResponse.take(0)).thenReturn(emptyArray);
            when(surreal.query(anyString())).thenReturn(mockResponse);

            template.findAll(PersonWithEagerLinkList.class);

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(surreal).query(sqlCaptor.capture());
            assertThat(sqlCaptor.getValue())
                    .containsIgnoringCase("FETCH")
                    .contains("friends");
        }

        @Test
        @DisplayName("returns empty list when FETCH query result array is empty")
        void returnsEmptyListWhenNoResults() {
            Value emptyArray = mockArrayValue();
            Response mockResponse = mock(Response.class);
            when(mockResponse.take(0)).thenReturn(emptyArray);
            when(surreal.query(contains("FETCH"))).thenReturn(mockResponse);

            List<PersonWithEagerLinkList> result =
                    template.findAll(PersonWithEagerLinkList.class);

            assertThat(result).isEmpty();
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Returns a mock {@link Value} that reports {@code isArray() = true} and
     * returns an empty iterator, simulating a zero-result SurrealDB response.
     */
    private static Value mockArrayValue() {
        Value v = mock(Value.class);
        Array emptyArr = mock(Array.class);
        when(v.isNone()).thenReturn(false);
        when(v.isNull()).thenReturn(false);
        when(v.isArray()).thenReturn(true);
        when(v.getArray()).thenReturn(emptyArr);
        when(emptyArr.iterator()).thenReturn(List.<Value>of().iterator());
        return v;
    }

    // =========================================================================
    // Test fixtures
    // =========================================================================

    @Table("person")
    static class Person {
        @Id RecordId id;
    }

    @Table("person")
    static class PersonWithStringId {
        @Id String id;
    }

    static class Company {
        @Id String id;
    }

    @Table("person")
    static class PersonWithEagerLink {
        @Id RecordId id;
        @Link(fetch = true)
        Company company;
    }

    @Table("person")
    static class PersonWithLazyLink {
        @Id RecordId id;
        @Link
        Company company;
    }

    @Table("person")
    static class PersonWithMixedLinks {
        @Id RecordId id;
        @Link(fetch = true)
        Company company;
        @Link   // fetch = false
        PersonWithMixedLinks manager;
    }

    @Table("person")
    static class PersonWithMultipleLinks {
        @Id RecordId id;
        @Link(fetch = true)
        Company company;
        @LinkList(fetch = true)
        List<PersonWithMultipleLinks> friends;
    }

    @Table("person")
    static class PersonWithEagerLinkList {
        @Id RecordId id;
        @LinkList(fetch = true)
        List<PersonWithEagerLinkList> friends;
    }
}
