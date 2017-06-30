![ArangoDB-Logo](https://docs.arangodb.com/assets/arangodb_logo_2016_inverted.png)

# Spring Data ArangoDB

## Table of Contents

* [Getting Started](#getting-started)
* [Template](#template)
  * [Introduction](#introduction)
  * [Configuration](#configuration)
  * [Java-Configuration](#java-configuration)
  * [XML-Configuration](#xml-configuration)
* [Repositories](#repositories)
  * [Introduction](#introduction)
  * [Instantiating](#instantiating)
  * [Return types](#return-types)
  * [Query Methods](#query-methods)
  * [Finder Methods](#finder-methods)
  * [Property expression](#property-expression)
  * [Special parameter handling](#special-parameter-handling)
    * [Bind parameters](#bind-parameters)
    * [AQL query options](#aql-query-options)
* [Mapping](#mapping)
  * [Introduction](#introduction)
  * [Conventions](#conventions)
  * [Type conventions](#type-conventions)
  * [Annotations](#annotations)
    * [Annotation overview](#annotation-overview)
    * [Document Edge](#document-edge)
    * [From To](#from-to)
    * [Reference](#reference)
    * [Indexed annotations](#indexed-annotations)

# Getting Started

Spring Data ArangoDB requires ArangoDB 3.1 or higher - which you can download [here](https://www.arangodb.com/download/) - and Java 6 or higher. To use Spring Data ArangoDB in your project, your build automation tool needs to be configured to include and use the Spring Data ArangoDB dependency. Example with Maven:

``` xml
<dependency>
  <groupId>com.arangodb</groupId>
  <artifactId>spring-data</artifactId>
  <version>{version}</version>
</dependency>
```

# Template

## Introduction

With `ArangoTemplate` Spring Data ArangoDB offers a central support for interactions with the database over a rich feature set. It mostly offers the features from the ArangoDB Java driver with additional exception translation from the drivers exceptions to the Spring Data access exceptions inheriting the `DataAccessException` class.
The `ArangoTemplate` class is the default implementation of the operations interface `ArangoOperations` which developers of Spring Data are encouraged to code against.

## Configuration

### Java-Configuration

You can use Java to instantiate and configure an instance of `ArangoTemplate` as show below. Setup the underlying driver (`ArangoDB.Builder`) with default configuration automatically loads a properties file arangodb.properties, if exists in the classpath.

``` java
@Configuration
public class MyConfiguration extends AbstractArangoConfiguration {

  @Override
  public ArangoDB.Builder arango() {
    return new ArangoDB.Builder();
  }

}
```

The driver is configured with some default values:

property-key | description | default value
-------------|-------------|--------------
arangodb.host | ArangoDB host | 127.0.0.1
arangodb.port | ArangoDB port | 8529
arangodb.timeout | socket connect timeout(millisecond) | 0
arangodb.user | Basic Authentication User |
arangodb.password | Basic Authentication Password |
arangodb.useSsl | use SSL connection | false

To customize the configuration, the parameters can be changed in the Java code.

``` java
ArangoDB.Builder arangoBuilder = new ArangoDB.Builder()
  .host("127.0.0.1")
  .port(8429)
  .user("root)";
return new ArangoTemplate(arangoBuilder);
```

In addition you can use the *arangodb.properties* or a custom properties file to supply credentials to the driver.

*Properties file*
```
arangodb.host=127.0.0.1
arangodb.port=8529
arangodb.user=root
arangodb.password=
```

*Custom properties file*
``` java
InputStream in = MyClass.class.getResourceAsStream("my.properties");
ArangoDB.Builder arangoBuilder = new ArangoDB.Builder().loadProperties(in);
return new ArangoTemplate(arangoBuilder);
```

### XML-Configuration

You can use Spring’s XML bean schema to configure `ArangoTemplate` as show below.

``` xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
  xmlns:arango="http://www.arangodb.com/spring/schema/arangodb"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
    http://www.arangodb.com/spring/schema/arangodb http://www.arangodb.com/spring/schema/arangodb/spring-arangodb.xsd">

  <arango:arango host="127.0.0.1" port="8529" username="root" password="" />

  <bean id="arangoTemplate" class="com.arangodb.springframework.core.template.ArangoTemplate">
    <constructor-arg ref="arango" />
  </bean>

</beans>
```

# Repositories

## Introduction

Spring Data Commons provide a composable repository infrastructure which Spring Data ArangoDB is built on. These allow for interface-based composition of repositories consisting of provided default implementations for certain interfaces (like `CrudRepository`) and custom implementations for other methods.

## Instantiating

Instances of a Repository are created in Spring beans through the auto-wired mechanism of Spring.

``` java
public class MySpringBean {

  @Autowired
  private MyRepository rep;

}
```

## Return types

The method return type for single results can be a primitive type, a POJO/domain class, `Map<String, Object>`, `BaseDocument`, `BaseEdgeDocument` or `VPackSlice`.
The method return type for multiple results can additionally be `ArangoCursor<Type>`, `Iterable<Type>`, `Collection<Type>`, `List<Type>` where Type can be everything a single result can be.

## Query Methods

Queries using ArangoDB Query Language (AQL) can be supplied with the @Query annotation on methods.

``` java
public interface MyRepository extends Repository<Customer, String>{

  @Query("FOR c IN customers FILTER c.name == @name RETURN c")
  ArangoCursor<Customer> query(String name);

}
```

The Bind Parameters will be substituted by the actual method parameter. In addition you can use a parameter bindVars from type Map<String, Object> as your Bind Parameters. You can then fill the map with any parameter used in the query. (see [here](https://docs.arangodb.com/3.1/AQL/Fundamentals/BindParameters.html#bind-parameters) for more Information about Bind Parameters)

``` java
public interface MyRepository extends Repository<Customer, String>{

  @Query("FOR c IN customers FILTER c.name == @name RETURN c")
  ArangoCursor<Customer> query(Map<String, Object> bindVars);

}
```

## Finder Methods

Spring Data ArangoDB supports Queries derived from methods names by splitting it into its semantic parts and converting into AQL. The mechanism strips the prefixes `find..By`, `get..By`, `query..By`, `read..By`, `count..By` from the method and parse the rest. The By acts as a separator to indicate the start of the criteria for the query to be build. You can define conditions on entity properties and concatenate them with `And` and `Or`.

``` java
public interface MyRepository extends Repository<Customer, String> {

  // FOR c IN customers FILTER c.name == @name RETURN c
  ArangoCursor<Customer> findByName(String name);
  ArangoCursor<Customer> getByName(String name);

  // FOR c IN customers
  // FILTER c.name == @name && c.age == @age
  // RETURN c
  ArangoCursor<Customer> findByNameAndAge(String name, int age);

  // FOR c IN customers
  // FILTER c.name == @name || c.age == @age
  // RETURN c
  ArangoCursor<Customer> findByNameOrAge(String name, int age);
}
```

You can apply sorting for one or multiple sort criteria by appending `SortBy` or `OrderBy` to the method and `Asc` or `Desc` for the directions.

``` java
public interface MyRepository extends Repository<Customer, String> {

  // FOR c IN customers
  // FITLER c.name == @name
  // SORT c.age DESC RETURN c
  ArangoCursor<Customer> findByNameSortByAgeDesc(String name);
  ArangoCursor<Customer> getByNameOrderByAgeDesc(String name);

  // FOR c IN customers
  // FILTER c.name = @name
  // SORT c.name ASC, c.age DESC RETURN c
  ArangoCursor<Customer> findByNameSortByNameAscAndAgeDesc(String name);

}
```

## Property expression

Property expressions can refer only to direct and nested properties of the managed domain class. The algorithm checks the domain class for the entire expression as the property. If the check fails, the algorithm splits up the expression at the camel case parts from the right and tries to find the corresponding property.

``` java
@Document(name="customers")
public class Customer {
  private Address address;
}

public class Address {
  private ZipCode zipCode;
}

public interface MyRepository extends Repository<Customer, String> {

  // 1. step: search domain class for a property "addressZipCode"
  // 2. step: search domain class for "addressZip.code"
  // 3. step: search domain class for "address.zipCode"
  ArangoCursor<Customer> findByAddressZipCode(ZipCode zipCode);
}
```

It is possible for the algorithm to select the wrong property if the domain class has also an property which match the first split of the expression. To resolve this ambiguity you can use _ as a separator inside your method-name to define traversal points.

``` java
@Document(name="customers")
public class Customer {
  private Address address;
  private AddressZip addressZip;
}

public class Address {
  private ZipCode zipCode;
}

public class AddressZip {
  private String code;
}

public interface MyRepository extends Repository<Customer, String> {

  // 1. step: search domain class for a property "addressZipCode"
  // 2. step: search domain class for "addressZip.code"
  // creates query with "x.addressZip.code"
  ArangoCursor<Customer> findByAddressZipCode(ZipCode zipCode);

  // 1. step: search domain class for a property "addressZipCode"
  // 2. step: search domain class for "addressZip.code"
  // 3. step: search domain class for "address.zipCode"
  // creates query with "x.address.zipCode"
  ArangoCursor<Customer> findByAddress_ZipCode(ZipCode zipCode);

}
```

## Special parameter handling

### Bind parameters

AQL supports the usage of [bind parameters](https://docs.arangodb.com/3.1/AQL/Fundamentals/BindParameters.html) which you can define with a method parameter named `bindVars` of type `Map<String, Object>`.

``` java
public interface MyRepository extends Repository<Customer, String> {

  @Query("FOR c IN customers FILTER c[@field] == @value RETURN c")
  ArangoCursor<Customer> query(Map<String, Object> bindVars);

}

Map<String, Object> bindVars = new HashMap<String, Object>();
bindVars.put("field", "name");
bindVars.put("value", "john";

// will execute query "FOR c IN customers FILTER c.name == "john" RETURN c"
ArangoCursor<Customer> cursor = myRepo.query(bindVars);
```

### AQL query options

You can set additional options for the query and the created cursor over the class `AqlQueryOptions` which you can simply define as a method parameter without a specific name. The `AqlQueryOptions` allows you to set the cursor time-to-life, batch-size, caching flag and several other settings.
This special parameter works with both query-methods and finder-methods. Keep in mind that some options - like time-to-life - are only effective if the method return type is `ArangoCursor<T>` or `Iterable<T>`.

``` java
public interface MyRepository extends Repository<Customer, String> {

  @Query("FOR c IN customers FILTER c.name == @name RETURN c")
  Iterable<Customer> query(String name, AqlQueryOptions options);

  Iterable<Customer> findByName(String name, AqlQueryOptions options);

  ArangoCursor<Customer> findByAddressZipCode(ZipCode zipCode, AqlQueryOptions options);

  @Query("FOR c IN customers FILTER c[@field] == @value RETURN c")
  ArangoCursor<Customer> query(Map<String, Object> bindVars, AqlQueryOptions options);

}
```

# Mapping

## Introduction

In this section we will describe the features and conventions for mapping Java objects to documents and how to override those conventions with annotation based mapping metadata.

## Conventions

* The Java class name is mapped to the collection name
* The non-static fields of a Java object are used as fields in the stored document
* The Java field name is mapped to the stored document field name
* All nested Java object are stored as nested objects in the stored document
* The Java class needs a non parameterized constructor

## Type conventions

ArangoDB use [VelocyPack](https://github.com/arangodb/velocypack) as internal storage format which supports a big number of data types. In addition Spring Data ArangoDB offers - with the underlying Java driver - built-in converters to add additional types to the mapping. These converters are separated for each mapping direction (Java <-> VelocyPack) with the classes `VPackSerializer` and `VPackDeserializer`.

Java type | VelocyPack type
----------|----------------
java.lang.String | string
java.lang.Boolean | bool
java.lang.Integer | signed int 4 bytes, smallint
java.lang.Long | signed int 8 bytes, smallint
java.lang.Short | signed int 2 bytes, smallint
java.lang.Double | double
java.lang.Float | double
java.math.BigInteger | signed int 8 bytes, unsigned int 8 bytes
java.math.BigDecimal | double
java.lang.Number | double
java.lang.Character | string
java.util.Date | string (date-format ISO 8601)
java.sql.Date | string (date-format ISO 8601)
java.sql.Timestamp | string (date-format ISO 8601)
java.util.UUID | string
java.lang.byte[] | string (Base64)

## Annotations

### Annotation overview

annotation | level | description
-----------|-------|------------
@Document | class | marked this class as a candidate for mapping
@Edge | class | marked this class as a candidate for mapping
@Graph | class | marked this class as part of a named graph
@Id | field | stored the field as the system field _id
@Key | field | stored the field as the system field _key
@Rev | field | stored the field as the system field _rev
@Field("alt-name") | field | stored the field with an alternative name
@Ref, @Reference | field | stored the _id of the referenced document and not the nested document
@From | field | stored the _id of the referenced document as the system field _from
@To | field | stored the _id of the referenced document as the system field _to
@Relation | field | vertices which are connected over edges
@HashIndexed | field | described how to index the field
@SkiplistIndexed | field | described how to index the field
@PersistentIndexed | field | described how to index the field
@GeoIndexed | field | described how to index the field
@FulltextIndexed | field | described how to index the field
@Transient, @Expose | field | excludes the field from serialisation/deserialisation

### Document Edge

The annotations `@Document` and `@Edge` applied to a class marked this class as a candidate for mapping to the database. The most relevant parameters are name to specify the collection name in the database. The annotation `@Document` specify the collection type to `DOCUMENT`. The annotation `@Edge` specify the collection type to `EDGE`.

``` java
@Document(name="persons")
public class Person {
  ...
}
@Edge(name="relations")
public class Relation {
  ...
}
```

### From To

With the annotations `@From` and `@To` applied on a field in a class annotated with @Document(type=`EDGE`) the nested object isn’t stored as a nested object in the document. The _id field of the nested object is stored in the edge document and the nested object is stored as a separate document in another collection described in the `@Document` annotation of the nested object class.

``` java
@Edge(name="relations")
public class Relation {
  @From
  private Person c1;
  @To
  private Person c2;
}

@Document(name="customers")
public class Customer {
  …
}
```

Will result in the following stored edge-document:

```
{
  "_key" : "123",
  "_id" : "relations/123",
  "_from" : "persons/456",
  "_to" : "persons/789"
}
```

and the following stored documents:

```
{
  "_key" : "456",
  "_id" : "persons/456",
}
{
  "_key" : "789",
  "_id" : "persons/789",
}
```

### Reference

With the annotation `@Ref` applied on a field the nested object isn’t stored as a nested object in the document. The `_id` field of the nested object is stored in the document and the nested object is stored as a separate document in another collection described in the `@Document` annotation of the nested object class.

``` java
@Document(name="persons")
public class Person {
  @Ref
  private Address address;
}

@Document(name="addresses")
public class Address {
  private String country;
  private String street;
  ...
}
```

Will result in the following stored documents:

```
{
  "_key" : "123",
  "_id" : "persons/123",
  "address" : "addresses/456"
}
{
  "_key" : "456",
  "_id" : "addresses/456",
  "country" : "...",
  "street" : "..."
}
```

Without the annotation `Ref` at the field `address`, the stored document would look:

```
{
  "_key" : "123",
  "_id" : "persons/123",
  "address" : {
    "country" : "...",
     "street" : "..."
  }
}
```

### Indexed annotations

With the `@<IndexType>Indexed` annotations user defined indexes can be created on collection level. If the index should include multiple fields the annotation can be applied to the required fields with the parameter group set to the same id. The type of the indexes with the same group has to be the same.

``` java
public class Person {
  @HashIndexed(group=0)
  private String name;

  @HashIndexed(group=0)
  private int age;

  @HashIndexed(group=1)
  private Gender gender;
}
```

Creates a Hash-Index on the fields `name` and `age` and a separate Hash-Index on the field `gender`.

``` java
public class Person {
  @FulltextIndexed(group=0)
  private String name;

  @HashIndexed(group=0)
  private int age;
}
```

Throws an exception because both indexes are from different types but have the same group-id.
