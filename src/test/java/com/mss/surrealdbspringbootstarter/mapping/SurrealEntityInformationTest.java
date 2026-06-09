package com.mss.surrealdbspringbootstarter.mapping;

import com.surrealdb.RecordId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SurrealEntityInformation")
class SurrealEntityInformationTest {

    // -------------------------------------------------------------------------
    // Table resolution (pre-existing behaviour)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("table name resolution")
    class TableNameResolution {

        @Test
        @DisplayName("defaults to lowercased simple class name when @Table is absent")
        void defaultsToLowercasedSimpleClassName() {
            SurrealEntityInformation<Plain, String> info = new SurrealEntityInformation<>(Plain.class);
            assertThat(info.table()).isEqualTo("plain");
        }

        @Test
        @DisplayName("honours explicit @Table value")
        void honoursExplicitTableAnnotation() {
            SurrealEntityInformation<Person, String> info = new SurrealEntityInformation<>(Person.class);
            assertThat(info.table()).isEqualTo("person");
        }
    }

    // -------------------------------------------------------------------------
    // @Id field resolution (pre-existing behaviour)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("@Id field resolution")
    class IdFieldResolution {

        @Test
        @DisplayName("detects @Id field present on the class")
        void detectsIdField() {
            SurrealEntityInformation<Plain, String> info = new SurrealEntityInformation<>(Plain.class);
            assertThat(info.hasIdField()).isTrue();
        }

        @Test
        @DisplayName("finds @Id declared on a superclass")
        void findsIdOnInheritedField() {
            SurrealEntityInformation<Child, String> info = new SurrealEntityInformation<>(Child.class);
            assertThat(info.hasIdField()).isTrue();
            Child c = new Child();
            info.setId(c, "abc");
            assertThat(info.getId(c)).isEqualTo("abc");
        }
    }

    // -------------------------------------------------------------------------
    // fetchFields() — @Link / @LinkList
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("fetchFields() — FETCH clause field collection")
    class FetchFields {

        @Test
        @DisplayName("returns empty list when entity has no @Link or @LinkList fields")
        void emptyWhenNoLinkAnnotations() {
            SurrealEntityInformation<Plain, String> info = new SurrealEntityInformation<>(Plain.class);
            assertThat(info.fetchFields()).isEmpty();
        }

        @Test
        @DisplayName("excludes @Link fields where fetch = false (default)")
        void excludesLinkWithFetchFalse() {
            SurrealEntityInformation<PersonWithLazyLink, String> info =
                    new SurrealEntityInformation<>(PersonWithLazyLink.class);
            assertThat(info.fetchFields()).doesNotContain("company");
        }

        @Test
        @DisplayName("includes @Link field when fetch = true")
        void includesLinkFieldWhenFetchTrue() {
            SurrealEntityInformation<PersonWithEagerLink, String> info =
                    new SurrealEntityInformation<>(PersonWithEagerLink.class);
            assertThat(info.fetchFields()).containsExactly("company");
        }

        @Test
        @DisplayName("excludes @LinkList fields where fetch = false (default)")
        void excludesLinkListWithFetchFalse() {
            SurrealEntityInformation<PersonWithLazyLinkList, String> info =
                    new SurrealEntityInformation<>(PersonWithLazyLinkList.class);
            assertThat(info.fetchFields()).doesNotContain("friends");
        }

        @Test
        @DisplayName("includes @LinkList field when fetch = true")
        void includesLinkListFieldWhenFetchTrue() {
            SurrealEntityInformation<PersonWithEagerLinkList, String> info =
                    new SurrealEntityInformation<>(PersonWithEagerLinkList.class);
            assertThat(info.fetchFields()).containsExactly("friends");
        }

        @Test
        @DisplayName("collects multiple fetch fields in declaration order")
        void collectsMultipleFetchFieldsInOrder() {
            SurrealEntityInformation<PersonWithMixedLinks, String> info =
                    new SurrealEntityInformation<>(PersonWithMixedLinks.class);
            // 'company' fetch=true, 'manager' fetch=false, 'friends' fetch=true
            assertThat(info.fetchFields()).containsExactly("company", "friends");
        }

        @Test
        @DisplayName("collects fetch fields declared on a superclass")
        void collectsFetchFieldsFromSuperclass() {
            SurrealEntityInformation<ChildWithLink, String> info =
                    new SurrealEntityInformation<>(ChildWithLink.class);
            assertThat(info.fetchFields()).contains("company");
        }
    }

    // -------------------------------------------------------------------------
    // lazyLinkFields() / lazyLinkListFields()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("lazyLinkFields() / lazyLinkListFields() — projection detection")
    class LazyLinkFields {

        @Test
        @DisplayName("excludes fields where type is RecordId even if fetch = false")
        void excludesRecordIdFields() {
            SurrealEntityInformation<PersonWithRawLink, String> info =
                    new SurrealEntityInformation<>(PersonWithRawLink.class);
            assertThat(info.lazyLinkFields()).isEmpty();
        }

        @Test
        @DisplayName("includes @Link field where fetch = false and type is a domain object")
        void includesLazyObjectLink() {
            SurrealEntityInformation<PersonWithLazyLink, String> info =
                    new SurrealEntityInformation<>(PersonWithLazyLink.class);
            assertThat(info.lazyLinkFields()).containsExactly("company");
        }

        @Test
        @DisplayName("includes @LinkList field where fetch = false and element type is a domain object")
        void includesLazyObjectLinkList() {
            SurrealEntityInformation<PersonWithLazyLinkList, String> info =
                    new SurrealEntityInformation<>(PersonWithLazyLinkList.class);
            assertThat(info.lazyLinkListFields()).containsExactly("friends");
        }
    }

    // -------------------------------------------------------------------------
    // resolveEdgeTable() — @Relate
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("resolveEdgeTable() — edge table name resolution")
    class ResolveEdgeTable {

        @Test
        @DisplayName("returns @Relate value when explicitly set")
        void returnsExplicitRelateValue() {
            assertThat(SurrealEntityInformation.resolveEdgeTable(WroteEdge.class))
                    .isEqualTo("wrote");
        }

        @Test
        @DisplayName("falls back to lowercased class name when @Relate value is empty")
        void fallsBackToLowercasedClassName() {
            assertThat(SurrealEntityInformation.resolveEdgeTable(FollowsEdge.class))
                    .isEqualTo("followsedge");
        }

        @Test
        @DisplayName("falls back to lowercased class name when @Relate is absent")
        void fallsBackWhenNoRelateAnnotation() {
            assertThat(SurrealEntityInformation.resolveEdgeTable(Plain.class))
                    .isEqualTo("plain");
        }
    }

    // =========================================================================
    // Test fixtures
    // =========================================================================

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

    // --- @Link fixtures ---

    @Table("person")
    static class PersonWithRawLink {
        @Id String id;
        @Link
        RecordId company;
    }

    @Table("person")
    static class PersonWithLazyLink {
        @Id String id;
        @Link           // fetch defaults to false, type is object → lazy projection
        CompanyFixture company;
    }

    @Table("person")
    static class PersonWithEagerLink {
        @Id String id;
        @Link(fetch = true)
        CompanyFixture company;
    }

    @Table("person")
    static class PersonWithLazyLinkList {
        @Id String id;
        @LinkList  // fetch defaults to false, type is list → lazy projection
        List<Person> friends;
    }

    @Table("person")
    static class PersonWithEagerLinkList {
        @Id String id;
        @LinkList(fetch = true)
        List<Person> friends;
    }

    @Table("person")
    static class PersonWithMixedLinks {
        @Id String id;
        @Link(fetch = true)
        CompanyFixture company;
        @Link        // fetch = false → excluded from fetchFields but in lazyLinkFields
        Person manager;
        @LinkList(fetch = true)
        List<Person> friends;
    }

    @Table("person")
    static class ParentWithLink {
        @Id String id;
        @Link(fetch = true)
        CompanyFixture company;
    }

    static class ChildWithLink extends ParentWithLink {
    }

    // --- @Relate fixtures ---

    @Relate("wrote")
    static class WroteEdge extends com.surrealdb.Relation {
    }

    @Relate   // value() is empty → falls back to class name
    static class FollowsEdge extends com.surrealdb.Relation {
    }

    // --- Stub target type for @Link ---
    static class CompanyFixture {
        @Id String id;
    }
}
