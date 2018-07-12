# Query methods

Queries using [ArangoDB Query Language (AQL)](https://docs.arangodb.com/current/AQL/index.html) can be supplied with the `@Query` annotation on methods. `AqlQueryOptions` can also be passed to the driver, as an argument anywhere in the method signature.

There are three ways of passing bind parameters to the query in the query annotation.

Using number matching, arguments will be substituted into the query in the order they are passed to the query method.

```java
public interface MyRepository extends Repository<Customer, String>{

  @Query("FOR c IN customers FILTER c.name == @0 AND c.surname == @2 RETURN c")
  ArangoCursor<Customer> query(String name, AqlQueryOptions options, String surname);

}
```

With the `@Param` annotation, the argument will be placed in the query at the place corresponding to the value passed to the `@Param` annotation.

```java
public interface MyRepository extends Repository<Customer, String>{

  @Query("FOR c IN customers FILTER c.name == @name AND c.surname == @surname RETURN c")
  ArangoCursor<Customer> query(@Param("name") String name, @Param("surname") String surname);

}
```

In addition you can use a parameter of type `Map<String, Object>` annotated with `@BindVars` as your bind parameters. You can then fill the map with any parameter used in the query. (see [here](https://docs.arangodb.com/3.1/AQL/Fundamentals/BindParameters.html#bind-parameters) for more Information about Bind Parameters).

```java
public interface MyRepository extends Repository<Customer, String>{

  @Query("FOR c IN customers FILTER c.name == @name AND c.surname = @surname RETURN c")
  ArangoCursor<Customer> query(@BindVars Map<String, Object> bindVars);

}
```

A mixture of any of these methods can be used. Parameters with the same name from an `@Param` annotation will override those in the `bindVars`.

```java
public interface MyRepository extends Repository<Customer, String>{

  @Query("FOR c IN customers FILTER c.name == @name AND c.surname = @surname RETURN c")
  ArangoCursor<Customer> query(@BindVars Map<String, Object> bindVars, @Param("name") String name);

}
```
