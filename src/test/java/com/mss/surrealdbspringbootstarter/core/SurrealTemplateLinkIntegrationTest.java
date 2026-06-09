package com.mss.surrealdbspringbootstarter.core;

import com.mss.surrealdbspringbootstarter.mapping.Link;
import com.mss.surrealdbspringbootstarter.mapping.LinkList;
import com.mss.surrealdbspringbootstarter.mapping.Table;
import com.surrealdb.RecordId;
import com.surrealdb.Surreal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Integration — Record links with fetch=false")
class SurrealTemplateLinkIntegrationTest {

    private Surreal surreal;
    private SurrealTemplate template;

    @BeforeEach
    void setUp() {
        surreal = new Surreal();
        surreal.connect("mem://");
        surreal.useNs("test").useDb("test");
        template = new SurrealTemplate(surreal);
    }

    @AfterEach
    void tearDown() {
        surreal.close();
    }

    @Test
    @DisplayName("findById() with @Link(fetch=false) returns an object with only ID populated")
    void findByIdWithLazyLink() {
        Company acme = new Company();
        acme.name = "Acme Corp";
        Company savedCompany = template.insert(acme);

        Employee emp = new Employee();
        emp.name = "Alice";
        emp.company = savedCompany;
        Employee savedEmp = template.insert(emp);

        Optional<Employee> found = template.findById(Employee.class, savedEmp.id);

        assertThat(found).isPresent();
        Employee loaded = found.get();
        assertThat(loaded.name).isEqualTo("Alice");
        
        // The company link should be present but only its ID should be populated
        assertThat(loaded.company).isNotNull();
        assertThat(loaded.company.id).isEqualTo(savedCompany.id);
        assertThat(loaded.company.name).isNull(); // Not fetched
    }

    @Test
    @DisplayName("findAll() with @LinkList(fetch=false) returns a list of objects with only IDs populated")
    void findAllWithLazyLinkList() {
        Project p1 = new Project(); p1.name = "Project Alpha";
        Project p2 = new Project(); p2.name = "Project Beta";
        Project savedP1 = template.insert(p1);
        Project savedP2 = template.insert(p2);

        Manager manager = new Manager();
        manager.name = "Bob";
        manager.projects = List.of(savedP1, savedP2);
        template.insert(manager);

        List<Manager> managers = template.findAll(Manager.class);

        assertThat(managers).hasSize(1);
        Manager loaded = managers.get(0);
        assertThat(loaded.name).isEqualTo("Bob");
        
        assertThat(loaded.projects).hasSize(2);
        assertThat(loaded.projects).extracting(p -> p.id)
                .containsExactlyInAnyOrder(savedP1.id, savedP2.id);
        assertThat(loaded.projects).extracting(p -> p.name)
                .containsOnlyNulls(); // Not fetched
    }

    // =========================================================================
    // Test fixtures
    // =========================================================================

    @Table("company")
    public static class Company {
        @Id public String id;
        public String name;
    }

    @Table("employee")
    public static class Employee {
        @Id public String id;
        public String name;

        @Link(fetch = false) // explicit false for clarity
        public Company company;
    }

    @Table("project")
    public static class Project {
        @Id public String id;
        public String name;
    }

    @Table("manager")
    public static class Manager {
        @Id public String id;
        public String name;

        @LinkList(fetch = false)
        public List<Project> projects;
    }
}
