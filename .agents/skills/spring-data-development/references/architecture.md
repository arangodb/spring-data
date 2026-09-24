# Architecture and ownership

Java paths below are relative to
[`src/main/java/com/arangodb/springframework/`](../../../../src/main/java/com/arangodb/springframework).

## Code map

| Area | Responsibility and entry points |
| --- | --- |
| `annotation/`, `config/` | Entity/repository annotations; `ArangoConfiguration` wires the driver, template, mapping context, converter, type mapper, and resolvers. |
| `core/` | Public `ArangoOperations`, `CollectionOperations`, and `UserOperations` contracts. |
| `core/template/` | `ArangoTemplate` implements database/document operations; collection/user adapters delegate to the driver. `ArangoExtCursor` adds load events. |
| `core/mapping/` | `ArangoMappingContext`, `DefaultArangoPersistentEntity`, and `DefaultArangoPersistentProperty` interpret entity metadata, names, collection options, indexes, and computed values. |
| `core/convert/` | `DefaultArangoConverter` maps objects to/from Jackson trees using Spring mapping metadata, conversions, and type information. `DefaultArangoTypeMapper` handles type discriminators. |
| `core/convert/resolver/` | `ResolverFactory`, reference/relation resolvers, and lazy-loading proxies connect mapped relationships to database reads. |
| `core/mapping/event/` | Save/delete/load events and `AuditingEventListener`; auditing hooks into before-save processing. |
| `repository/` | Registration/factory infrastructure, `SimpleArangoRepository` for CRUD/paging/sorting, and `ArangoExampleConverter` for query by example. |
| `repository/query/` | Query metadata, parameters, declared/derived AQL execution, and `ArangoResultConverter`; `derived/` builds criteria and bind parameters. |
| `core/util/`, `core/geo/` | AQL/name/ID helpers, exception translation, and GeoJSON value types. |

## Configuration and mapping flow

An application implements `ArangoConfiguration`, providing `arango()` and
`database()`, and enables repository scanning with `@EnableArangoRepositories`.
The default bean methods connect the driver to this adapter's mapping layer:

```text
application entity <-> DefaultArangoConverter <-> Jackson JsonNode
                   <-> ArangoConfiguration.serde() <-> driver user-data bytes
```

The serde uses the Jackson mapper for `contentType()`. Its format must agree with
the configured driver protocol. Spring metadata and custom conversions, not direct
Jackson POJO binding, determine the persisted entity representation. Driver-owned
transport and protocol handling remain outside this repository.

`ArangoMappingContext` creates persistent entity/property metadata. The converter
uses it for constructors and properties, custom read/write conversions, references,
and polymorphic types (default discriminator `_class`). Relationship resolvers
can call back into the template; lazy values are not ordinary embedded documents.

## Repository execution paths

```text
@EnableArangoRepositories
  -> ArangoRepositoriesRegistrar / ArangoRepositoryConfigurationExtension
  -> ArangoRepositoryFactoryBean / ArangoRepositoryFactory
       -> SimpleArangoRepository -> ArangoTemplate -> Java driver
       -> StringBasedArangoQuery or DerivedArangoQuery
            -> AbstractArangoQuery -> ArangoOperations.query()
            -> ArangoExtCursor -> ArangoResultConverter -> projection processing
```

The implemented lookup strategy is `CREATE_IF_NOT_FOUND`: named queries precede
`@Query`, then method-name derivation. `StringBasedArangoQuery` resolves bind
parameters, SpEL, and `#collection` / `#sort` / `#pageable` placeholders.
`DerivedArangoQuery` uses `DerivedQueryCreator`, `Criteria`, and
`BindParameterBinding`. Query by example has its own `ArangoExampleConverter` path.
`AbstractArangoQuery` merges options and selects the result type; result adaptation
and Spring projection processing are separate from AQL construction.

Repository `save` / `saveAll` delegate to template `repsert` / `repsertAll`: AQL
UPSERT with REPLACE, not a partial update or a generic Spring `isNew()` decision.
The template returns saved objects and updates writable metadata on inputs.
`ArangoConfiguration.returnOriginalEntities()` controls which objects the repository
returns; immutable entities and records use `false`.

## Template lifecycle and extension boundaries

`ArangoTemplate` resolves the database expression per access and caches driver
handles by resolved name. Collection metadata can also contain SpEL; collection
cache keys include both database and collection. Database/collection creation and
annotation-driven index setup happen lazily through the template's access paths.

The template publishes lifecycle events, applies write metadata, and delegates
error classification to `core/util/ArangoExceptionTranslator`. `ArangoExtCursor`
publishes load events as results are consumed. Collection operations maintain
collection-cache state, so bypassing the adapters can change lifecycle behavior.

## Build and consumer boundaries

[`pom.xml`](../../../../pom.xml) owns the library's dependencies and root tests.
[`integration-tests/pom.xml`](../../../../integration-tests/pom.xml) tests the
installed library under a Spring Boot parent. Its `src/test/java/com` and test
resources are symlinks to root sources; `src/test/java/arch` is not shared.
[`tutorial/`](../../../../tutorial) is a separate runnable Boot consumer with
assertions, not a library module or a replacement for the test suite. The separate
Spring Boot starter repository is linked from `README.md`; it is not implemented here.
