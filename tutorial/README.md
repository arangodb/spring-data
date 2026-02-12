![ArangoDB-Logo](https://www.arangodb.com/wp-content/uploads/2016/05/ArangoDB_logo_@2.png)

# Spring Data ArangoDB - Tutorial

This is a tutorial on how to configure [Spring Data ArangoDB](https://github.com/arangodb/spring-data), without using 
Spring Boot Starter ArangoDB.
A more extensive demo about the features of Spring Data ArangoDB can be found in the 
[Spring Boot Starter ArangoDB Demo](https://github.com/arangodb/spring-boot-starter/tree/main/demo).

## Get started

This tutorial is about how to configure [Spring Data ArangoDB](https://github.com/arangodb/spring-data)
without using Spring Boot Starter ArangoDB.

For a more extensive tutorial about the features of Spring Data ArangoDB and
Spring Boot support, see the [Spring Boot Starter](../spring-boot-arangodb.md)
documentation.

### Build a project with Maven

Set up a project and add every needed dependency. This demo uses Maven and
Spring Boot.

Create a Maven `pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <relativePath/>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.0.2</version>
    </parent>

    <groupId>com.arangodb</groupId>
    <artifactId>spring-data-arangodb-tutorial</artifactId>
    <version>1.0.0</version>

    <name>spring-data-arangodb-tutorial</name>
    <description>ArangoDB Spring Data Tutorial</description>

    <properties>
        <java.version>21</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>com.arangodb</groupId>
            <artifactId>arangodb-spring-data</artifactId>
            <version>5.0.0</version>
        </dependency>
    </dependencies>

</project>
```

Substitute the versions with the latest available versions that are compatible.
See the [Supported versions](#supported-versions) for details.

### Entity classes

For this tutorial we will model our entity with a Java record class:

```java
@Document("characters")
public record Character(
        @Id
        String id,
        String name,
        String surname
) {
}
```

### Create a repository

Now that we have our data model, we want to store data. For this, we create a repository interface which
extends `ArangoRepository`. This gives us access to CRUD operations, paging, and query by example mechanics.

```java
public interface CharacterRepository extends ArangoRepository<Character, String> {
}
```

### Create a Configuration class

We need a configuration class to set up everything to connect to our ArangoDB instance and to declare that all
needed Spring Beans are processed by the Spring container.

- `@EnableArangoRepositories`: Defines where Spring can find your repositories
- `arango()`: Method to configure the connection to the ArangoDB instance
- `database()`: Method to define the database name
- `returnOriginalEntities()`: Method to configures the behavior of repository save methods to either return the  
  original entities (updated where possible) or new ones. Set to `false` to use java records.

```java
@Configuration
@EnableArangoRepositories(basePackages = {"com.arangodb.spring.demo"})
public class AdbConfig implements ArangoConfiguration {

    @Override
    public ArangoDB.Builder arango() {
        return new ArangoDB.Builder()
                .host("localhost", 8529)
                .user("root")
                .password("test");
    }

    @Override
    public String database() {
        return "spring-demo";
    }

    @Override
    public boolean returnOriginalEntities() {
        return false;
    }
}
```

Note that, in case the driver is configured to use a protocol with `VPACK`
content type (i.e. `HTTP_VPACK` or `HTTP2_VPACK`), then the
`ArangoConfiguration#contentType()` method must be overridden to return
`ContentType.VPACK` as shown in the following example:

```java
@Override
public ArangoDB.Builder arango() {
  new ArangoDB.Builder()
      // ...    
      .protocol(Protocol.HTTP2_VPACK);
}

@Override
public ContentType contentType() {
  return ContentType.VPACK;
}
```

### Create a CommandLineRunner

To run our demo as command line application, we have to create a class implementing `CommandLineRunner`:

```java
@ComponentScan("com.arangodb.spring.demo")
public class CrudRunner implements CommandLineRunner {

    @Autowired
    private ArangoOperations operations;

    @Autowired
    private CharacterRepository repository;

    @Override
    public void run(String... args) {
        // first drop the database so that we can run this multiple times with the same dataset
        operations.dropDatabase();

        System.out.println("# CRUD operations");

        // save a single entity in the database
        // there is no need of creating the collection first. This happen automatically
        Character nedStark = new Character(null, "Ned", "Stark");
        Character saved = repository.save(nedStark);
        System.out.println("Ned Stark saved in the database: " + saved);
    }
}
```

### Run the application

Finally, we create a main class:

```java
@SpringBootApplication
public class DemoApplication {
  public static void main(final String... args) {
    System.exit(SpringApplication.exit(
            SpringApplication.run(CrudRunner.class, args)
    ));
  }
}
```

And run it with:

```shell
mvn spring-boot:run
```

This should produce a console output similar to:

```
Ned Stark saved in the database: Character[id=2029, name=Ned, surname=Stark]
```
