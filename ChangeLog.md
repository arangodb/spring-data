# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/) and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- added support for streaming AQL cursors
- added support for satellite collections
  - added `@Document#satellite()`
  - added `@Edge#satellite()`

## [1.1.8] - 2018-06-26

### Fixed

- fixed derived query with `containing` on `String` (issue #84)

## [1.1.7] - 2018-06-25

### Changed

- upgraded dependency arangodb-java-driver 4.5.2
  - fixed `ArangoDB#aquireHostList(true)` with authentication
  - added support for custom serializer

## [1.1.6] - 2018-06-14

### Changed

- allow override of CRUD methods of `ArangoRepository` with `@Query`
- upgraded dependency arangodb-java-driver 4.5.0

### Fixed

- fixed lazy use of `@Relations`/`@From`/`@To` when using a `Set<>`

## [1.1.5] - 2018-06-07

### Fixed

- fixed relation cycle (issue #43)

## [1.1.4] - 2018-06-07

### Changed

- upgraded arangodb-java-driver to 4.4.1

### Fixed

- fixed relation cycle (issue #43)

## [1.1.3] - 2018-06-04

### Added

- added paging and sorting support for native queries

### Fixed

- fixed support for `ArangoCusor` as query-method return type

## [1.1.2] - 2018-05-04

### Added

- added support for named queries

### Deprecated

- deprecated `@Param` annotation, there is already such an annotation from Spring Data

### Fixed

- fixed floating point numbers in derived queries
- fixed distance calculation in derived geo queries

## [1.1.1] - 2018-04-23

### Fixed

- fixed serialization of enums (issue #39)

## [1.1.0] - 2018-04-20

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

## [1.0.1] - 2018-01-26

### Fixed

- fixed missing WITH information in AQL when resolving annotation `@Relations` (Issue #8)
