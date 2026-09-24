# Spring Data ArangoDB: agent guidance

This repository implements synchronous Spring Data integration over the ArangoDB
Java driver. The root Maven project builds the library; `integration-tests/` and
`tutorial/` are separate consumer projects, not reactor modules.

## Task entry points

| Task | Read |
| --- | --- |
| Implement, fix, refactor, or update dependencies | [Development skill](.agents/skills/spring-data-development/SKILL.md) |
| Write tests, run validation, or diagnose CI | [Testing skill](.agents/skills/spring-data-testing/SKILL.md) |
| Locate an implementation boundary | [Architecture](.agents/skills/spring-data-development/references/architecture.md) |

Read only the references relevant to the change. These are ordinary Markdown
files; agents without skill discovery can follow the same links.

## Repository constraints

- Keep library code and root tests compatible with Java 17, including JDK APIs.
  Running on a newer CI JDK does not raise the baseline.
- Do not introduce production dependencies on `..internal..` packages.
  [InternalsTest](src/test/java/arch/InternalsTest.java) enforces this; internal
  imports in test fixtures are not production precedents.
- Preserve public API and persisted-document compatibility except where the task
  intentionally changes the contract. Follow nearby code and `formatter.xml`;
  avoid unrelated reformatting. Use the existing Apache header for new Java files.
- Follow [CONTRIBUTING.md](CONTRIBUTING.md); do not bump the project release version.
  Edit source files, not `target/` or `.flattened-pom.xml`.
- Most root tests run through Surefire at `mvn test` and need an authenticated
  ArangoDB deployment; the testing skill describes the CI-equivalent setup.
- Use disposable databases. Tests truncate collections and drop databases; do not
  run suites concurrently against the same deployment. Publishing is not validation.

For build versions and execution details, the POMs, `.circleci/config.yml`, and
fixtures take precedence over summaries here. Correct stale guidance when changing
those sources; do not mistake existing buggy behavior for an intended contract.

In the handoff, identify behavior/compatibility changes, checks actually executed
and their results, and relevant checks not run. Separate environment limitations
from test failures; compilation or skipped tests are not behavioral validation.
