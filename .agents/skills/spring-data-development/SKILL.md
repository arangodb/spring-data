---
name: spring-data-development
description: >-
  Maintain and extend the arangodb/spring-data repository. Use for Spring Data
  ArangoDB bug fixes, features, refactoring, mapping and serialization changes,
  repository or AQL behavior, configuration, and dependency upgrades. This is
  codebase development guidance, not an application integration tutorial.
---

# Spring Data ArangoDB development

Apply [AGENTS.md](../../../AGENTS.md). Trace the affected operation through one
neighboring implementation before selecting the layer to change. Establish the
intended contract from the task, API documentation, and tests, not assumptions
about how another Spring Data store works.

## Load by concern

| Concern | Reference |
| --- | --- |
| Ownership, call flow, or extension points | [Architecture](references/architecture.md) |
| Entity fields, IDs, conversions, or relationships | [Mapping](references/changes.md#mapping-and-persisted-data) |
| Repository methods, AQL, or result adaptation | [Repositories and queries](references/changes.md#repositories-and-queries) |
| Writes, events, revisions, indexes, or tenancy | [Template behavior](references/changes.md#template-behavior) |
| Spring wiring, driver, or dependency changes | [Configuration and dependencies](references/changes.md#configuration-and-dependencies) |
| Regression coverage or validation commands | [Testing skill](../spring-data-testing/SKILL.md) |

## Change and verify

For a bug, add a regression at the failing boundary and demonstrate failure before
the fix when feasible. For a feature, check that the configured driver and target
server versions support the operation. Keep transport implementation in the driver,
not this adapter. For a refactor, explicitly retain the affected API, document
representation, and query/result semantics.

Inspect related overloads and both template and repository entry points when they
share the changed behavior. Add coverage in the owning test area rather than
copying the suite into a consumer project. Use the testing skill to select a
focused run and the additional CI variants justified by the change.

Update affected public Javadocs and local examples. Record user-visible changes
under `ChangeLog.md`'s `Unreleased` section; leave released entries intact. The main
reference manual is linked from `README.md`, not maintained in this source tree;
identify any required external documentation follow-up rather than inventing a
local documentation module.
