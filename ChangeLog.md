# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/) and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unreleased]

- added support to Spring Data `3.5` (DE-1027, #329)
- updated Java Driver to version `7.20.0`

## [4.5.0] - 2024-12-12

- added support to Spring Data `3.4`
- updated Java Driver to version `7.15.0`
- updated Jackson Dataformat Velocypack to version `4.5.0`

## [4.4.2] - 2024-11-08

- updated Java Driver to version `7.12.0`


## [4.4.1] - 2024-10-23

- updated Java Driver to version `7.10.0`


## [4.4.0] - 2024-09-24

- updated Java Driver to version `7.9.0`
- added MD-Index support (#320, DE-789)
- added allowRetry AQL query option (DE-589)
- fixed arangoTemplate bean factory (#319)


## [4.3.0] - 2024-09-05

- fixed delete with revision in ArangoRepository (#317, DE-843)
- fixed mapping of entities with non-string ids (#316, DE-842)
- added `METHOD` and `ANNOTATION_TYPE` to property annotations targets (#314, DE-780)
- fixed concurrency bug in date formatting and parsing (#313, DE-840) 
- updated `arangodb-java-driver` to version `7.8.0`


## [4.2.0] - 2024-06-21

- added support to Spring Data `3.3` (DE-816, #306)
- added `LazyLoadingProxy.isResolved()` to check whether a lazy proxy has been already resolved or not (DE-805, #271)
- added support conputed values in data definitions (DE-779, [docs-hugo#477](https://github.com/arangodb/docs-hugo/pull/477))
- added support mapping of computed values (DE-604, [docs-hugo#477](https://github.com/arangodb/docs-hugo/pull/477))
- added configuration for changing `ArangoRepository#save()` behavior returning new entity instances instead of original ones (DE-539, #295, [docs-hugo#476](https://github.com/arangodb/docs-hugo/pull/476))
- added support for Java records, Kotlin data classes and immutable entities (DE-539, #295, [docs-hugo#476](https://github.com/arangodb/docs-hugo/pull/476))
- updated `arangodb-java-driver` to version `7.7.1`
- fixed unit of measure of GeoResults distance (DE-803)
- fixed concurrency in annotation cache (#302)
- dropped support for Spring Data `3.1` (DE-816, #306)
- moved CI to CircleCI (DE-823, #308)


## [4.1.0] - 2024-01-24

- updated `arangodb-java-driver` to version `7.5.1`
- updated to Spring Data 3.2 (DE-767)
- fixed `VPACK` support (DE-707, #297)
- fixed type information equality (#288)


## [4.0.0] - 2023-09-18

- upgraded dependency Spring Framework 6 and Spring Data 3 (#250)
- `CrudRepository.deleteById()` silently ignores an unknown id (#283)
- exceptions during `ArangoOperations.query()` are now translated (#281)
- improved exception translation `OptimisticLockingFailureException` is now thrown in case of `_rev` conflict (#282)
- raised required minimum Java version to JDK 17
- deprecated Fulltext Index support 
- changed `deduplicate` default value to `true` in `@PersistentIndex` and `@PersistentIndexed` annotations 
- underlying Java driver (accessible via `com.arangodb.springframework.core.ArangoOperations#driver()`) uses
  now `ArangoConverter` bean to serialize and deserialize user data (#284)
- renamed `ArangoOperations` methods operating on multiple documents with `All` suffix (e.g. `insert(Iterable)` has been
  renamed to `insertAll(Iterable)` (#284)
- `ArangoOperations` methods for single document manipulation have now specific return
  types (, `DocumentDeleteEntity<T>`, `DocumentUpdateEntity<T>`, `DocumentCreateEntity<T>`) (#284)
- `ArangoOperations` methods for multiple documents manipulation have now specific return types as for single documents,
  wrapped by `MultiDocumentEntity<>`  (#284)
- `ArangoOperations` methods for documents manipulation accepting options `returnNew(boolean)` or `returnOld(boolean)`
  return now the deserialized entity in the response (accessible via `getNew()` or `getOld()`) (#284)
- changed the arguments order of some `ArangoOperations` methods for better API coherence (#284)
- changed the arguments type of some `ArangoOperations` methods to be covariant (#284)
- return updated entity from `ArangoOperations.repsert()` (#285)
- removed deprecated `AbstractArangoConfiguration` in favor of `ArangoConfiguration`
- removed support for Joda-Time

## [3.10.0] - 2023-05-17

- fixed merge AQL options from `@QueryOptions` annotation and `AqlQueryOptions` parameter (#276)
- upgraded dependency `com.arangodb:arangodb-java-driver:6.24.0` (DE-541)

## [3.9.0] - 2023-04-18

- upgraded dependency `com.arangodb:arangodb-java-driver:6.22.0` (DE-541)

## [3.8.0] - 2023-02-08

- improved resolution of already fetched embedded entities (#270)
- added `deduplicate` in persistent indexes options  (#242)
- dependencies update
- fixed support for `Pageable.unpaged()` in `PagingAndSortingRepository` (#255)
- fixed concurrency in SPEL evaluation of string based queries (#266)

## [3.7.1] - 2022-08-19

- fixed counterclockwise box generation for compatibility with geo json polygons in ArangoDB 3.10 (#231)
- fixed query generation with custom field names (#240)
- fixed compatibility with Lombok classes (#241)
- fixed WITH clause generation for compatibility with ArangoDB 3.10 (#247)


## [3.7.0] - 2022-02-08

- deprecated hash and skiplist indexes
- fixed support for collection names containing `-` symbol (#230)
- upgraded dependency `com.arangodb:arangodb-java-driver:6.16.0`
- added mapping support to `LocalTime` (#227)

**NOTE:** new changes in Spring Core named parameters discovery could result in runtime exceptions when using positional
query parameters. Using named query parameters is therefore recommended.

## [3.6.0] - 2021-09-20

- updated support of spring-data to `2.5.x`
- upgraded dependency `com.arangodb:arangodb-java-driver:6.13.0`
- upgraded dependency `com.arangodb:velocypack:2.5.4`
- fixed binding Point parameter in query derivation (#223)
- added support to geoJSON types and Spring Data geo types (#222)
- added SPEL support to custom AQL query on repository methods (#221)
- fixed missing `META-INF/spring.factories` (#155)
- fixed query derivation for not persistent nested attribute (#216)

## [3.5.0] - 2021-04-23

- added annotations to ttl indexes (`@TtlIndex` and `@TtlIndexed`)
- removed dependencies on `velocypack-module-jdk8` and `velocypack-module-joda`
- upgraded dependency `com.arangodb:arangodb-java-driver:6.11.1`
- upgraded dependency `com.arangodb:velocypack:2.5.3`

## [3.4.1] - 2021-01-26

- upgraded dependency `com.arangodb:arangodb-java-driver:6.8.2`

## [3.4.0] - 2020-12-21

### Added
- support for array search by example (#202)

### Changed
- updated support of spring-data to `2.4.x`
- upgraded dependency `com.arangodb:velocypack:2.5.1`
- upgraded dependency `com.arangodb:arangodb-java-driver:6.8.0`

### Fixed
- fixed swallowed exception in query result conversion (#213)
- fixed escaping of array brackets in user queries (#208)

## [3.3.0] - 2020-08-14

### Added

- find by example using regex string matcher 
- find by example matching any object in nested array

### Changed

- reimplemented `ArangoOperations.repsert()` using AQL UPSERT (also used by `ArangoRepository.save()`)
- dependencies update

### Fixed
- fixed `null` serialization when writing Maps, Arrays and Collections
- fixed automatical collection creation on `ArangoRepository.count()` and `ArangoRepository.findAll()`
- fixed serialization of `@Ref` fields with custom names 
- fixed collection like mapping, allowing duplicate values
- fixed lazy behavior of `ArangoOperations.findAll()`

## [3.2.5] - 2020-05-14

- set `org.springframework.data.build:spring-data-parent` as parent project
- added `org.springframework.boot:spring-boot-dependencies` to dependency management

## [3.2.4] - 2020-05-07

- dependencies update

## [3.2.3] - 2019-09-16

- upgraded dependency arangodb-java-driver 6.3.0

## [3.2.2] - 2019-09-04

- upgraded dependency arangodb-java-driver 6.1.0

## [3.2.1] - 2018-12-17

### Fixed

- fixed preventing the use of multiple edge entities in a query inside `@Relations`
- fixed `ArangoRepository.save()` to perform _repsert_ instead of _upsert_ (for ArangoDB < 3.4)
- fixed deserialization of nested `Map`s
- fixed deserialization of type `Object` 

## [3.2.0] - 2018-11-09

### Added

- added interface `ArangoConfiguration` to replace `AbstractArangoConfiguration`

  `ArangoConfiguration` provides default methods and can be implemented directly without the need of the class `AbstractArangoConfiguration`.

### Changed

- updated support of spring to `5.1.x`
- updated support of spring-data to `2.1.x`

### Deprecated

- deprecated `AbstractArangoConfiguration`

### Removed

- removed use of javax.xml.bind to better support Java 9 and above

### Fixed

- fixed NPE when loading non-existing data into fields of primitive type (issue #127)

## [3.1.1] - 2018-09-25

### Fixed

- upgraded dependency arangodb-java-driver 5.0.1
  - fixed dirty read
  - fixed connection stickiness

## [3.1.0] - 2018-09-18

### Added

- added dirty read support ([reading from followers](https://www.arangodb.com/docs/stable/administration-active-failover.html#reading-from-follower)) for AQL queries
  - added `QueryOptions#allowDirtRead`

### Changed

- use AQL `LIKE` instead of `REGEX_TEST` for query by example in `ArangoRepository`
- upgraded dependency arangodb-java-driver 5.0.0

## [3.0.0] - 2018-09-04

### Added

- added support for non-String `@Id`s (issue #79)
- added annotation `@ArangoId` as representation for field `_id` (instead of `@Id`)
- added support for saving entities lazy loaded

  Entities loaded over `@Ref`/`@From`/`@To`/`@Relations` with `lazy` == `true` can now be saved back into the database.

- added logging of query warnings when executed through `ArangoRepository` (issue #56)
- added convenience method `ArangoOperations#query(String, Class)`
- added convenience method `ArangoOperations#query(String, Map<String, Object>, Class)`
- added convenience method `AbstractArangoConfiguration#customConverters()` to add custom converters
- added SpEL expression parsing for database names

  SpEL expressions can now be used within `AbstractArangoConfiguration#database()`. This allows Multi-tenancy on database level.

- added mapping events (`BeforeDeleteEvent`, `AfterDeleteEvent`, `AfterLoadEvent`, `BeforeSaveEvent`, `AfterSaveEvent`)
- added support for non-collection fields annotated with `@Relations`/`@From`/`@To` in domain objects annotated with `@Document` (issue #104)
- added support for placeholder `#collection` in `@Query` methods
- added auditing support through annotations `@CreatedDate`/`@CreatedBy`/`@LastModifiedDate`/`@LastModifiedBy`

### Changed

- save `@Id` values as `_key` instead of `_id` (issue #78)
- changed SpEL expression parsing for collection names

  SpEL expressions in `@Document#value`/`@Edge#value` are now parsed whenever the domain entity is accessed. This allows Multi-tenancy on collection level.

- upgraded dependency arangodb-java-driver 5.0.0

### Removed

- removed `com.arangodb.springframework.annotation.Key`
- removed `com.arangodb.springframework.annotation.Param`
- removed `com.arangodb.springframework.core.convert.DBEntity`
- removed `com.arangodb.springframework.core.convert.DBCollectionEntity`

### Fixed

- fixed repository methods with `Example` using `StringMatcher.CONTAINING` (issue #113)
- added `toString()`, `equals()` and `hashCode()` to proxy

## [2.3.1] - 2018-08-13

### Fixed

- fixed a bug in derived queries when using two times `@Relations` in one entity

## [2.3.0] - 2018-07-18

### Deprecated

- deprecated `com.arangodb.springframework.annotation.Key`
- deprecated `com.arangodb.springframework.core.convert.DBEntity`
- deprecated `com.arangodb.springframework.core.convert.DBCollectionEntity`

## [2.2.2] - 2018-07-09

### Fixed

- fixed `ArangoOperations#getVersion()` use configured database instead of \_system

## [2.2.1] - 2018-07-03

### Fixed

- fixed `ArangoOperations#upsert(T, UpsertStrategy)` (issue #92)
  - Check `Persistable#isNew`
- fixed `ArangoOperations#upsert(Iterable<T>, UpsertStrategy)` (issue #92)
  - Check `Persistable#isNew`

## [2.2.0] - 2018-07-02

### Added

- added `ArangoOperations#repsert(T)`
- added `ArangoOperations#repsert(Iterable<T>, Class<T>)`
- added support for streaming AQL cursors
  - added `QueryOptions#stream()`
- added `QueryOptions#memoryLimit()`
- added support for satellite collections
  - added `@Document#satellite()`
  - added `@Edge#satellite()`

## Changed

- upgraded dependency arangodb-java-driver 4.6.0
- changed `SimpleArangoRepository#save()` to use `ArangoOperations#repsert()` when ArangoDB version >= 3.4.0
- changed `SimpleArangoRepository#saveAll()` to use `ArangoOperations#repsert()` when ArangoDB version >= 3.4.0
- changed `ArangoOperations#upsert(T, UpsertStrategy)` to work with `@Id` in addition to `@Key`
- changed `ArangoOperations#upsert(Iterable<T>, UpsertStrategy)` to work with `@Id` in addition to `@Key`

### Deprecated

- deprecated `ArangoOperations#upsert(T, UpsertStrategy)`
- deprecated `ArangoOperations#upsert(Iterable<T>, UpsertStrategy)`

## [2.1.9] - 2018-06-26

### Fixed

- fixed derived query with `containing` on `String` (issue #84)

## [2.1.8] - 2018-06-25

### Changed

- upgraded dependency arangodb-java-driver 4.5.2
  - fixed `ArangoDB#aquireHostList(true)` with authentication
  - added support for custom serializer

## [2.1.7] - 2018-06-14

### Changed

- allow override of CRUD methods of `ArangoRepository` with `@Query`
- upgraded dependency arangodb-java-driver 4.5.0

### Fixed

- fixed lazy use of `@Relations`/`@From`/`@To` when using a Set<>

## [2.1.6] - 2018-06-07

### Fixed

- fixed relation cycle (issue #43)

## [2.1.5] - 2018-06-07

### Changed

- upgraded arangodb-java-driver to 4.4.1

### Fixed

- fixed relation cycle (issue #43)

## [2.1.4] - 2018-06-04

### Added

- added `java.time.*` to `ArangoSimpleTypes`
- added paging and sorting support for native queries

### Changed

- annotated `TimeStringConverters` for compatibility with Spring Data 2.0.7-RELEASE

### Fixed

- fixed support for `ArangoCusor` as query-method return type (compatibility with Spring Data 2.0.7-RELEASE)

## [2.1.3] - 2018-05-04

### Added

- added support for named queries

### Deprecated

- deprecated `@Param` annotation, there is already such an annotation from Spring Data

### Fixed

- fixed floating point numbers in derived queries
- fixed distance calculation in derived geo queries

## [2.1.2] - 2018-04-23

### Fixed

- fixed serialization of enums (issue #39)

## [2.1.1] - 2018-04-20

### Fixed

- fixed `org.joda.time.DateTime` parsing

## [2.1.0] - 2018-04-20

### Added

- added `DataIntegrityViolationException` to `ExceptionTranslator`
- added type mapper implementation & custom conversions extension (issue #33)

### Fixed

- fixed race conditions in `ArangoTemplate` when creating database and collections (issue #35)
- fixed missing deserialization of return types of `@Query` methods(issue #21)
- fixed handling of `java.time` in converters (issue #36, #24, #25)
- fixed handling of `org.joda.time` in converters (issue #36)

## [2.0.3] - 2018-03-23

### Fixed

- fixed missing WITH information in derived query

## [2.0.2] - 2018-01-26

### Fixed

- fixed missing WITH information in AQL when resolving annotation @Relations (issue #9)

[unreleased]: https://github.com/arangodb/spring-data/compare/3.2.1...HEAD
[3.2.1]: https://github.com/arangodb/spring-data/compare/3.2.0...3.2.1
[3.2.0]: https://github.com/arangodb/spring-data/compare/3.1.0...3.2.0
[3.1.0]: https://github.com/arangodb/spring-data/compare/3.0.0...3.1.0
[3.0.0]: https://github.com/arangodb/spring-data/compare/2.3.1...3.0.0
[2.3.1]: https://github.com/arangodb/spring-data/compare/2.3.0...2.3.1
[2.3.0]: https://github.com/arangodb/spring-data/compare/2.2.2...2.3.0
[2.2.2]: https://github.com/arangodb/spring-data/compare/2.2.1...2.2.2
[2.2.1]: https://github.com/arangodb/spring-data/compare/2.2.0...2.2.1
[2.2.0]: https://github.com/arangodb/spring-data/compare/2.1.9...2.2.0
[2.1.9]: https://github.com/arangodb/spring-data/compare/2.1.8...2.1.9
[2.1.8]: https://github.com/arangodb/spring-data/compare/2.1.7...2.1.8
[2.1.7]: https://github.com/arangodb/spring-data/compare/2.1.6...2.1.7
[2.1.6]: https://github.com/arangodb/spring-data/compare/2.1.5...2.1.6
[2.1.5]: https://github.com/arangodb/spring-data/compare/2.1.4...2.1.5
[2.1.4]: https://github.com/arangodb/spring-data/compare/2.1.3...2.1.4
[2.1.3]: https://github.com/arangodb/spring-data/compare/2.1.2...2.1.3
[2.1.2]: https://github.com/arangodb/spring-data/compare/2.1.1...2.1.2
[2.1.1]: https://github.com/arangodb/spring-data/compare/2.1.0...2.1.1
[2.1.0]: https://github.com/arangodb/spring-data/compare/2.0.3...2.1.0
[2.0.3]: https://github.com/arangodb/spring-data/compare/2.0.2...2.0.3
