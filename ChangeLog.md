# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/) and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- added support for non-String `@Id`s (issue #79)
- added annotation `@ArangoId` as representation for field `_id` (instead of `@Id`)
- added support for saving entities lazy loaded

  Entities loaded over `@Ref`/`@From`/`@To`/`@Relations` with `lazy` == `true` can now be saved back into the database.

- added logging of query warnings when executed through `ArangoRepository` (#issue 56)
- added convenience method `ArangoOperations#query(String, Class)`
- added convenience method `ArangoOperations#query(String, Map<String, Object>, Class)`
- added convenience method `AbstractArangoConfiguration#customConverters()` to add custom converters
- added SpEL expression parsing for database names

  SpEL expressions can now be used within `AbstractArangoConfiguration#database()`. This allows Multi-tenancy on database level.

- added mapping events (`BeforeDeleteEvent`, `AfterDeleteEvent`, `AfterLoadEvent`, `BeforeSaveEvent`, `AfterSaveEvent`)

### Changed

- save `@Id` values as `_key` instead of `_id` (issue #78)
- changed SpEL expression parsing for collection names

  SpEL expressions in `@Document#value`/`@Edge#value` are now parsed whenever the domain entity is accessed. This allows Multi-tenancy on collection level.

### Removed

- removed `com.arangodb.springframework.annotation.Key`
- removed `com.arangodb.springframework.annotation.Param`
- removed `com.arangodb.springframework.core.convert.DBEntity`
- removed `com.arangodb.springframework.core.convert.DBCollectionEntity`

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
