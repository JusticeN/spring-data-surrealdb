package com.mss.springdata.surrealdb;

import com.mss.springdata.surrealdb.core.SurrealTemplate;
import com.mss.springdata.surrealdb.mapping.Link;
import com.mss.springdata.surrealdb.mapping.LinkList;
import com.mss.springdata.surrealdb.mapping.Relate;
import com.mss.springdata.surrealdb.mapping.Table;
import com.mss.springdata.surrealdb.repository.SimpleSurrealRepository;
import com.mss.springdata.surrealdb.repository.SurrealGraphRepository;
import com.mss.springdata.surrealdb.repository.SurrealRepository;
import com.surrealdb.RecordId;
import com.surrealdb.Relation;
import com.surrealdb.Surreal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests that run against a real embedded SurrealDB instance using
 * the {@code mem://} connection scheme — no external server is required.
 *
 * <p>The embedded engine runs in-process via JNI and discards all data when
 * {@link Surreal#close()} is called, giving each test a completely clean slate.
 *
 * <p>Setup notes:
 * <ul>
 *   <li>Use {@code mem://} (not {@code memory://}) for the embedded engine in SDK 2.1.0.</li>
 *   <li>The embedded engine does not support {@code signin()} — just call
 *       {@code useNs()} and {@code useDb()} directly after connecting.</li>
 *   <li>Entity {@code @Id} fields must be declared as {@link RecordId} (not
 *       {@code String}) because the SDK deserialises server-assigned ids as
 *       {@link RecordId} objects.</li>
 * </ul>
 *
 * <p>Covers end-to-end behaviour of:
 * <ul>
 *   <li>Basic CRUD via {@link SurrealTemplate}</li>
 *   <li>Graph edges (RELATE) via {@link SurrealTemplate#relate}</li>
 *   <li>Record links with auto-FETCH via {@link Link} / {@link LinkList}</li>
 *   <li>{@link SurrealRepository} and {@link SurrealGraphRepository} delegates</li>
 * </ul>
 */
@DisplayName("Integration — embedded in-memory SurrealDB (mem://)")
class SurrealDbIntegrationTest {

    private Surreal surreal;
    private SurrealTemplate template;

    @BeforeEach
    void setUp() {
        surreal = new Surreal();
        surreal.connect("mem://");
        // The embedded engine does not support authentication —
        // calling useNs/useDb is sufficient to establish the session context.
        surreal.useNs("test").useDb("test");
        template = new SurrealTemplate(surreal);
    }

    @AfterEach
    void tearDown() {
        surreal.close();
    }

    // =========================================================================
    // CRUD
    // =========================================================================

    @Nested
    @DisplayName("CRUD operations via SurrealTemplate")
    class CrudOperations {

        @Test
        @DisplayName("insert() persists a record and returns it with a server-assigned RecordId")
        void insertPersistsRecord() {
            Person p = new Person();
            p.name = "Ada";

            Person saved = template.insert(p);

            assertThat(saved).isNotNull();
            assertThat(saved.name).isEqualTo("Ada");
            assertThat(saved.id).isNotNull();
            assertThat(saved.id).contains("person:");
        }

        @Test
        @DisplayName("findById() retrieves a previously inserted record by its RecordId key")
        void findByIdRetrieves() {
            Person p = new Person();
            p.name = "Alan";
            Person saved = template.insert(p);
            String id = saved.id;

            Optional<Person> found = template.findById(Person.class, id);

            assertThat(found).isPresent();
            assertThat(found.get().name).isEqualTo("Alan");
        }

        @Test
        @DisplayName("findById() returns an empty Optional when the id does not exist")
        void findByIdReturnsEmptyForMissingRecord() {
            Optional<Person> found = template.findById(Person.class, "nobody");

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("findAll() returns every record currently in the table")
        void findAllReturnsAllRecords() {
            Person p1 = new Person(); p1.name = "Grace";
            Person p2 = new Person(); p2.name = "Margaret";
            template.insert(p1);
            template.insert(p2);

            List<Person> all = template.findAll(Person.class);

            assertThat(all).hasSize(2)
                    .extracting(p -> p.name)
                    .containsExactlyInAnyOrder("Grace", "Margaret");
        }

        @Test
        @DisplayName("update() persists changed field values to the database")
        void updateChangesFields() {
            Person p = new Person();
            p.name = "Hedy";
            Person saved = template.insert(p);
            saved.name = "Hedy Lamarr";

            template.update(saved);

            Optional<Person> reloaded = template.findById(Person.class,
                    saved.id);
            assertThat(reloaded).isPresent()
                    .get()
                    .extracting(x -> x.name)
                    .isEqualTo("Hedy Lamarr");
        }

        @Test
        @DisplayName("update() throws IllegalArgumentException when the entity has no @Id value")
        void updateThrowsWithoutId() {
            Person p = new Person();
            p.name = "Nobody";

            assertThatThrownBy(() -> template.update(p))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("without an @Id value");
        }

        @Test
        @DisplayName("deleteById() removes only the targeted record, leaving others intact")
        void deleteByIdRemovesRecord() {
            Person alice = template.insert(personNamed("Alice"));
            template.insert(personNamed("Bob"));

            template.deleteById(Person.class, alice.id);

            List<Person> remaining = template.findAll(Person.class);
            assertThat(remaining).hasSize(1)
                    .extracting(p -> p.name)
                    .containsExactly("Bob");
        }

        @Test
        @DisplayName("deleteAll() removes every record from the table")
        void deleteAllClearsTable() {
            template.insert(personNamed("X"));
            template.insert(personNamed("Y"));

            template.deleteAll(Person.class);

            assertThat(template.findAll(Person.class)).isEmpty();
        }
    }

    // =========================================================================
    // Graph edges — RELATE
    // =========================================================================

    @Nested
    @DisplayName("Graph edges via SurrealTemplate.relate()")
    class GraphEdges {

        @Test
        @DisplayName("relate() creates an edge with the correct 'in' and 'out' table pointers")
        void relateCreatesEdgeWithInAndOut() {
            Person alice   = template.insert(personNamed("Alice"));
            Article article = template.insert(articleTitled("SurrealDB"));

            Wrote edge = template.relate(
                    Wrote.class,
                    alice.id, "person",
                    article.id, "article");

            assertThat(edge).isNotNull();
            assertThat(edge.in.getTable()).isEqualTo("person");
            assertThat(edge.out.getTable()).isEqualTo("article");
        }

        @Test
        @DisplayName("relate() stores the edge in the table named by the @Relate annotation")
        void relateResolvesEdgeTableFromAnnotation() {
            Person alice   = template.insert(personNamed("Alice"));
            Article article = template.insert(articleTitled("SurrealDB"));

            Wrote edge = template.relate(
                    Wrote.class,
                    alice.id, "person",
                    article.id, "article");

            assertThat(edge.id.getTable()).isEqualTo("wrote");
        }

        @Test
        @DisplayName("relate() with content persists extra metadata fields on the edge record")
        void relateWithContentStoresMetadata() {
            Person alice   = template.insert(personNamed("Alice"));
            Article article = template.insert(articleTitled("SurrealDB"));
            Wrote meta = new Wrote();
            meta.since = "2025-01-01";

            Wrote edge = template.relate(
                    Wrote.class,
                    alice.id, "person",
                    article.id, "article",
                    meta);

            assertThat(edge.since).isEqualTo("2025-01-01");
        }

        @Test
        @DisplayName("relate() with pre-built RecordIds produces the same result as the table+id overload")
        void relateWithPrebuiltRecordIds() {
            Person alice    = template.insert(personNamed("Alice"));
            Article article = template.insert(articleTitled("SurrealDB"));

            Wrote edge = template.relate(Wrote.class, alice.id, article.id);

            assertThat(edge).isNotNull();
            assertThat(edge.in.getTable()).isEqualTo("person");
            assertThat(edge.out.getTable()).isEqualTo("article");
        }

        @Test
        @DisplayName("multiple relate() calls produce independent edge records with distinct ids")
        void multipleRelateCallsCreateDistinctEdgeIds() {
            Person alice = template.insert(personNamed("Alice"));
            Article art1  = template.insert(articleTitled("Article One"));
            Article art2  = template.insert(articleTitled("Article Two"));

            Wrote e1 = template.relate(Wrote.class, alice.id, art1.id);
            Wrote e2 = template.relate(Wrote.class, alice.id, art2.id);

            assertThat(e1.id).isNotEqualTo(e2.id);
        }
    }

    // =========================================================================
    // Record links — @Link / @LinkList with auto-FETCH
    // =========================================================================

    @Nested
    @DisplayName("Record links — @Link and @LinkList with auto-FETCH")
    class RecordLinks {

        @Test
        @DisplayName("findById() with @Link(fetch=true) includes the linked table in the FETCH clause")
        void findByIdWithLinkFetchTrue() {
            Company acme = new Company();
            acme.name = "Acme Corp";
            Company savedCompany = template.insert(acme);

            Employee emp = new Employee();
            emp.name = "Alice";
            emp.company = savedCompany;
            Employee savedEmp = template.insert(emp);

            Optional<Employee> found = template.findById(
                    Employee.class, savedEmp.id);

            assertThat(found).isPresent();
            assertThat(found.get().name).isEqualTo("Alice");
            // After FETCH, the company link is hydrated
            assertThat(found.get().company).isNotNull();
            assertThat(found.get().company.name).isEqualTo("Acme Corp");
        }

        @Test
        @DisplayName("findAll() with @LinkList(fetch=true) hydrates the list link on every result")
        void findAllWithLinkListFetchTrue() {
            Article a1 = template.insert(articleTitled("Chapter One"));
            Article a2 = template.insert(articleTitled("Chapter Two"));

            Author author = new Author();
            author.name = "Grace";
            author.articles = List.of(a1, a2);
            template.insert(author);

            List<Author> authors = template.findAll(Author.class);

            assertThat(authors).hasSize(1);
            Author loaded = authors.get(0);
            assertThat(loaded.name).isEqualTo("Grace");
            assertThat(loaded.articles).hasSize(2);
            assertThat(loaded.articles.get(0).title).isEqualTo("Chapter One");
        }

        @Test
        @DisplayName("findAll() on a plain entity with no @Link fields uses the standard select() path")
        void findAllWithoutFetchFieldsUsesPlainSelect() {
            template.insert(personNamed("Alan"));
            template.insert(personNamed("Grace"));

            List<Person> people = template.findAll(Person.class);

            assertThat(people).hasSize(2)
                    .extracting(p -> p.name)
                    .containsExactlyInAnyOrder("Alan", "Grace");
        }
    }

    // =========================================================================
    // SurrealRepository delegate
    // =========================================================================

    @Nested
    @DisplayName("SurrealRepository — save / find / delete lifecycle")
    class RepositoryLifecycle {

        private SimpleSurrealRepository<Person, String> personRepo;

        @BeforeEach
        void buildRepo() {
            personRepo = new SimpleSurrealRepository<>(template, Person.class);
        }

        @Test
        @DisplayName("save() inserts a new entity when no matching id exists yet")
        void saveInsertsNewEntity() {
            Person p = new Person();
            p.name = "Katherine";

            Person saved = personRepo.save(p);

            assertThat(saved.id).isNotNull();
            assertThat(personRepo.count()).isEqualTo(1L);
        }

        @Test
        @DisplayName("save() updates an entity in-place when its id already exists in the database")
        void saveUpdatesExistingEntity() {
            Person p = new Person();
            p.name = "Rosalind";
            Person inserted = personRepo.save(p);

            inserted.name = "Rosalind Franklin";
            personRepo.save(inserted);

            assertThat(personRepo.count()).isEqualTo(1L);
            Optional<Person> reloaded = personRepo.findById(inserted.id);
            assertThat(reloaded).isPresent()
                    .get()
                    .extracting(x -> x.name)
                    .isEqualTo("Rosalind Franklin");
        }

        @Test
        @DisplayName("existsById() returns true for an existing entity and false for an unknown id")
        void existsById() {
            Person p = new Person();
            p.name = "Vera";
            Person saved = personRepo.save(p);

            assertThat(personRepo.existsById(saved.id)).isTrue();
            assertThat(personRepo.existsById("ghost")).isFalse();
        }

        @Test
        @DisplayName("saveAll() + findAll() round-trips a collection of entities")
        void saveAllAndFindAll() {
            List<Person> people = List.of(personNamed("Emmy"), personNamed("Lise"));
            personRepo.saveAll(people);

            List<String> names = template.findAll(Person.class)
                    .stream().map(pe -> pe.name).toList();
            assertThat(names).containsExactlyInAnyOrder("Emmy", "Lise");
        }

        @Test
        @DisplayName("deleteAll() via repository removes every entity from the table")
        void deleteAllClearsRepository() {
            personRepo.save(personNamed("A"));
            personRepo.save(personNamed("B"));

            personRepo.deleteAll();

            assertThat(personRepo.count()).isEqualTo(0L);
        }

        @Test
        @DisplayName("delete(entity) removes only the given entity")
        void deleteEntityRemovesOnlyThatEntity() {
            Person alice = personRepo.save(personNamed("Alice"));
            personRepo.save(personNamed("Bob"));

            personRepo.delete(alice);

            assertThat(personRepo.count()).isEqualTo(1L);
            assertThat(personRepo.existsById(alice.id)).isFalse();
        }
    }

    // =========================================================================
    // SurrealGraphRepository delegate
    // =========================================================================

    @Nested
    @DisplayName("SurrealGraphRepository — relate() via repository layer")
    class GraphRepositoryOps {

        private SimpleSurrealRepository<Person, String> personRepo;

        @BeforeEach
        void buildRepo() {
            personRepo = new SimpleSurrealRepository<>(template, Person.class);
        }

        @Test
        @DisplayName("relate() auto-resolves the 'from' table from the repository's own domain type")
        void relateResolvesFromTableFromDomainType() {
            Person alice    = personRepo.save(personNamed("Alice"));
            Article article = template.insert(articleTitled("SurrealDB"));

            Wrote edge = personRepo.relate(
                    Wrote.class,
                    alice.id,
                    "article",
                    article.id);

            assertThat(edge).isNotNull();
            assertThat(edge.in.getTable()).isEqualTo("person");
            assertThat(edge.out.getTable()).isEqualTo("article");
            assertThat(edge.id.getTable()).isEqualTo("wrote");
        }

        @Test
        @DisplayName("relate() with content stores edge metadata when called from the repository layer")
        void relateWithContentViaRepository() {
            Person alice    = personRepo.save(personNamed("Alice"));
            Article article = template.insert(articleTitled("SurrealDB"));
            Wrote meta = new Wrote();
            meta.since = "2026-01-01";

            Wrote edge = personRepo.relate(
                    Wrote.class,
                    alice.id,
                    "article",
                    article.id,
                    meta);

            assertThat(edge.since).isEqualTo("2026-01-01");
        }

        @Test
        @DisplayName("standard CRUD methods still work on a SurrealGraphRepository instance")
        void crudStillWorksOnGraphRepo() {
            personRepo.save(personNamed("Hypatia"));
            personRepo.save(personNamed("Emmy"));

            assertThat(personRepo.count()).isEqualTo(2L);
            assertThat(personRepo.findAll())
                    .extracting(p -> p.name)
                    .containsExactlyInAnyOrder("Hypatia", "Emmy");
        }

        @Test
        @DisplayName("SimpleSurrealRepository is an instance of SurrealGraphRepository")
        void simpleSurrealRepositoryImplementsGraphRepository() {
            assertThat(personRepo).isInstanceOf(SurrealGraphRepository.class);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static Person personNamed(String name) {
        Person p = new Person();
        p.name = name;
        return p;
    }

    private static Article articleTitled(String title) {
        Article a = new Article();
        a.title = title;
        return a;
    }

    // =========================================================================
    // Domain fixtures
    // =========================================================================

    @Table("person")
    public static class Person {
        @Id public String id;
        public String name;
    }

    @Table("article")
    public static class Article {
        @Id public String id;
        public String title;
    }

    @Table("company")
    public static class Company {
        @Id public String id;
        public String name;
    }

    /** Employee with a single eagerly-fetched company link. */
    @Table("employee")
    public static class Employee {
        @Id public String id;
        public String name;

        @Link(fetch = true)
        public Company company;
    }

    /** Author with an eagerly-fetched list of article links. */
    @Table("author")
    public static class Author {
        @Id public String id;
        public String name;

        @LinkList(fetch = true)
        public List<Article> articles;
    }

    /** Graph edge: person –[wrote]→ article */
    @Relate("wrote")
    public static class Wrote extends Relation {
        public String since;
    }
}
