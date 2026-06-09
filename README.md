# SurrealDB Spring Boot Starter

A Spring Boot starter that provides seamless integration with [SurrealDB](https://surrealdb.com/). This library brings Spring Data-like repository abstractions (`SurrealRepository`), custom mapping annotations (`@Table`, `@Link`), and a core `SurrealTemplate` to your Spring Boot application, making it easy to interact with SurrealDB.

## 📦 Installation

### Maven

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.mss</groupId>
    <artifactId>spring-data-surrealdbr</artifactId>
    <version>1.0.0-SNAPSHOT</version> <!-- Replace with current version -->
</dependency>
```

### Gradle

Add the dependency to your `build.gradle`:

```gradle
dependencies {
    implementation 'com.mss:surrealdb-spring-boot-starter:1.0.0-SNAPSHOT' // Replace with current version
}
```

## ⚙️ Configuration

Configure your SurrealDB connection properties in your `application.yml` or `application.properties`:

```yaml
surrealdb:
  url: ws://localhost:8000
  username: root
  password: root
  namespace: test
  database: test
```

## 🚀 Getting Started

Here is a quick teaser showing how to set up an application using this starter.

### 1. Enable Repositories

Annotate your main Spring Boot application class with `@EnableSurrealRepositories` to activate the custom repository scanning.

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import repository.com.mss.springdata.surrealdb.EnableSurrealRepositories;

@SpringBootApplication
@EnableSurrealRepositories
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

### 2. Create an Entity

Use the `@Table` annotation to map your domain class to a SurrealDB table. Note that the primary key field can be mapped directly.

```java
import mapping.com.mss.springdata.surrealdb.Table;
import org.springframework.data.annotation.Id;

@Table("person")
public class Person {
    
    @Id
    private String id;
    private String name;
    private int age;

    // Constructors, Getters, and Setters
    public Person() {}

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }
    
    // ...
}
```

### 3. Create a Repository

Create an interface extending `SurrealRepository` to get standard CRUD operations right out of the box.

```java
import repository.com.mss.springdata.surrealdb.SurrealRepository;

public interface PersonRepository extends SurrealRepository<Person, String> {
}
```

### 4. Use the Repository

You can now auto-wire your repository and use it in your services or command-line runners!

```java
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AppRunner implements CommandLineRunner {

    private final PersonRepository repository;

    public AppRunner(PersonRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Create
        Person john = repository.save(new Person("John Doe", 30));
        System.out.println("Saved: " + john.getName());

        // Read
        repository.findById(john.getId()).ifPresent(p -> {
            System.out.println("Found: " + p.getName());
        });

        // Delete
        repository.delete(john);
    }
}
```

## 🔗 Modeling Relationships

SurrealDB allows you to store direct pointers to other records using record links. You can use `@Link` for single relationships and `@LinkList` for collections.

### Single Link (`@Link`)

Use `@Link` to point to a single related entity.

```java
import mapping.com.mss.springdata.surrealdb.Link;
import mapping.com.mss.springdata.surrealdb.Table;
import org.springframework.data.annotation.Id;

@Table("person")
public class Person {
    @Id
    private String id;
    private String name;

    @Link(fetch = true) // Automatically fetch the linked company document
    private Company company;
    
    // ...
}

@Table("company")
public class Company {
    @Id
    private String id;
    private String name;
    
    // ...
}
```

### Link List (`@LinkList`)

Use `@LinkList` to manage a collection of record links.

```java
import mapping.com.mss.springdata.surrealdb.LinkList;
import mapping.com.mss.springdata.surrealdb.Table;
import java.util.List;

@Table("person")
public class Person {
    @Id
    private String id;
    private String name;

    @LinkList(fetch = true) // Automatically fetch all friend documents
    private List<Person> friends;

    // ...
}
```
