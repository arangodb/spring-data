# Change guide

Read only the affected sections. Java paths are relative to
`src/main/java/com/arangodb/springframework/`; test names refer to `src/test/java/`.

## Mapping and persisted data

Keep identity roles distinct: Spring Data `@Id` maps to `_key`, `@ArangoId` to the
full `_id` handle, and `@Rev` to `_rev`. `@From` / `@To` map to `_from` / `_to` on
edges; document-side relationships use resolver behavior instead. Use
`DefaultArangoPersistentProperty`, converter ID conversion, and `MetadataUtils`
rather than assuming every ID is a String or every property name is its stored name.

Preserve the converter's distinction between entity fields, map entries, missing
values, and explicit nulls. Entity writes omit null properties; that is not a rule
to strip nulls from every representation. Retain custom-conversion precedence,
constructor/record materialization, and configured type-discriminator behavior.
Changes to `_class`, `@Field`, or naming strategies can affect existing stored data
and query paths as well as newly written entities.

Keep objects passing through `ArangoConverter` and the configured serde. Do not
replace this with an application's ObjectMapper or direct driver POJO binding.
Exercise JSON and VelocyPack when changing conversion or serde behavior.
For `@Ref`, `@Relations`, and graph endpoints, preserve the resolver's stored-ID
versus traversal semantics and lazy/eager behavior; avoid introducing eager reads
merely to inspect an unresolved proxy.

Use the relevant `core/mapping/*MappingTest`, `LazyLoadingProxyTest`, and
`IdType*TemplateTest` / `IdType*RepositoryTest` families for these boundaries.

## Repositories and queries

Trace query changes through method/parameter metadata, the appropriate declared,
derived, or query-by-example builder, template execution, and result adaptation.
Do not assume fixing one query construction path fixes the others.

Keep runtime values in AQL bind variables. Preserve collection binding (`@@name`
in AQL, `@name` in the map), mapped property paths, and existing structural
placeholder/SpEL handling. Reuse `AqlUtils` and the parameter-binding machinery
instead of concatenating user values into query text.

When adding an option, inspect `annotation/QueryOptions`, `ArangoQueryMethod`, and
`AbstractArangoQuery.mergeQueryOptions`: non-null dynamic options override static
ones, and explicit false/zero values must not be treated as absent. Check declared
query precedence and parameter naming when modifying lookup or Spring integration.

Test the affected result contracts, including projections, empty/single results,
count/exists, sort/pagination and full-count requirements. Cursors returned to a
caller must remain consumable; do not eagerly materialize or close them as a
side effect of an unrelated refactor. Use `ArangoAqlQueryTest`,
`DerivedQueryCreatorTest`, and the applicable `ArangoRepositoryTest` cases.

## Template behavior

Keep `save`'s replacement semantics distinct from `update`. For write changes,
check single and bulk operations, revision checks, returned versus input objects,
and metadata/computed-value propagation. Cover both default mutable-entity behavior
and `returnOriginalEntities=false` for immutable entities/records where affected.
Do not introduce a read-before-write check that weakens atomic revision handling.

Preserve before/after event timing and auditing along the changed path. Translate
driver errors through the existing exception translator, retaining their causes;
check document API and AQL failures separately. Missing documents, revision
conflicts, duplicate keys, and per-item bulk failures are not interchangeable.
Use the template/repository tests and `core/mapping/event/` tests for these contracts.

For collection, index, or computed-value changes, trace annotation -> persistent
metadata -> driver options -> template/collection operation. Check the relevant
server versions, single-server/cluster behavior, and existing version assumptions
in `ArangoIndexTest`. For naming or cache changes, exercise
`MultiTenancyCollectionLevelMappingTest`, `MultiTenancyDBLevelMappingTest`, and
`MultiTenancyDBLevelRepositoryTest`; retain database-qualified collection caches
and per-access expression evaluation rather than capturing the first tenant.

## Configuration and dependencies

Preserve `ArangoConfiguration`'s bean customization points and repository factory
wiring. Protocol selection and `contentType()` must agree (JSON versus VelocyPack);
consult `ArangoTestConfiguration` for the existing protocol-aware fixture, but do
not copy its driver-internal imports into production.

Read all three POMs for upgrades. The root Spring Data parent version is also the
imported Spring Boot dependency BOM version, so bumping one bumps the other;
`arangodb.version` manages the driver dependencies. The integration and tutorial
projects have independent Boot parents and consume the installed library, so a
root-only build is insufficient for a dependency compatibility change. Preserve
dependency convergence and explicit scopes enforced by Maven instead of disabling
the enforcer to resolve conflicts.

Verify the Java 17 baseline with JDK 17: the root compiler uses `source`/`target`,
which alone do not prevent calls to newer JDK APIs. Include root `InternalsTest`,
Boot compatibility, and tutorial validation as relevant; use the testing skill for
the current CI matrix and install sequence.
