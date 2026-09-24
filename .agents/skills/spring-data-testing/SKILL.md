---
name: spring-data-testing
description: >-
  Write and run tests for arangodb/spring-data, select regression coverage, and
  reproduce CircleCI failures. Use for Maven validation, ArangoDB server/topology
  or protocol variants, JDK compatibility, Spring Boot consumer tests, and the
  tutorial smoke test. Distinguishes local focused checks from complete CI jobs.
---

# Spring Data ArangoDB testing

Apply [AGENTS.md](../../../AGENTS.md). Use
[CircleCI recipes](references/circleci.md) for job defaults, database setup, and
commands. For a CI failure, resolve the workflow's matrix values and the job's
remaining defaults before running it; changing one axis is not the full matrix.

## Select coverage

Start at the affected boundary, then expand to the relevant CI jobs. Mapping/serde
changes need JSON and VelocyPack coverage; database/index/graph behavior can need
both server versions and topologies; dependency or Spring changes need consumer
and JDK checks. Documentation-only changes need link/command verification, not an
unrelated database matrix.

Put shared library regression tests in root `src/test/java/`, following the existing
JUnit Jupiter fixtures and assertions. Root Surefire includes `*Test.java` and
`*Example.java`. Edit the original test sources/resources, not the symlink paths
under `integration-tests/`.
That project reuses the `com` tests but not `arch/InternalsTest` and inherits its
own Boot test configuration; it does not inherit the root POM's test dependencies
(including the VelocyPack ones) or Surefire includes, so root `*Example.java`
classes do not run there. Check consumer test compilation when adding fixture
dependencies.

`AbstractArangoTest` truncates configured collections and drops the shared test
database. Reuse the existing setup/teardown and genuine server-version assumptions;
do not disable a regression or broaden an assumption to hide an infrastructure
failure. Even `DerivedQueryCreatorTest` uses a database-backed fixture.

## Focused local checks

Run from the repository root with Maven (`mvn`; there is no wrapper):

```sh
# Compile production and test sources; no tests or database calls run.
mvn test-compile

# Small database-free selection, not a substitute for the full suite.
mvn -Dtest=InternalsTest,AqlUtilsTest,MetadataUtilsTest,JavaTimeUtilTest test
```

With a disposable database provisioned as described in the recipes:

```sh
mvn -Darangodb.protocol=HTTP2_JSON -Dtest=ArangoTemplateTest test
```

Replace the selector with the affected class or a quoted `ClassName#method`.
Confirm the intended tests executed, including nested cases where applicable.
Add `clean` when switching JDKs or dependency versions so stale compiled classes
are not reused. Compilation, a successful run with zero selected tests, and the CI
install step with tests skipped are not equivalent to a test pass.

## Inspect results

Read `target/surefire-reports/` in the project actually tested; Boot consumer reports
are under `integration-tests/target/surefire-reports/`. For a failure, distinguish
startup/routing/authentication, dependency or test-compilation problems, assertion
failures, and deliberate version skips before changing code. Report the command,
JDK, server image/version/topology, protocol, consumer version where relevant, and
outcome.
Do not present unexecuted variants or skipped cases as covered.
