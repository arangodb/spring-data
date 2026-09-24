# CircleCI test recipes

Use the checked-out [CI configuration](../../../../.circleci/config.yml) and POMs
as the source of truth. These are local equivalents of its validation jobs, not a
second CI implementation. Commands start at the repository root unless shown
inside a subshell. Select the required JDK through the environment and confirm it
with `mvn --version`.

- [Matrix](#matrix)
- [Database setup](#database-setup)
- [Root test job](#root-test-job)
- [Boot integration-test job](#boot-integration-test-job)
- [Tutorial job](#tutorial-job)
- [Reports and release boundary](#reports-and-release-boundary)

## Matrix

The `test` job defaults are JDK 21 (`j21`),
`docker.io/arangodb/enterprise:latest`, `single`, and `HTTP2_JSON`.
With an empty pipeline `docker-img` parameter, CI schedules these independent
matrices; it does not take their combined Cartesian product:

| Workflow | Job | Values differing from the `test` defaults |
| --- | --- | --- |
| `test-adb-version` | `test` | `docker.io/arangodb/enterprise:3.12` and `docker.io/arangodb/core-preview:4-nightly`, each with `single` and `cluster` |
| `test-jdk-versions` | `test` | JDK 17, 21, 25 (`j17`, `j21`, `j25`) |
| `test-protocol` | `test` | `HTTP_VPACK`, `HTTP_JSON`, `HTTP2_VPACK`, `HTTP2_JSON` |
| `test-spring-version` | `integration-test` | JDK 17, Boot parent `4.0.2`; default image, single server, `HTTP2_JSON` |
| `tutorial` | `tutorial` | JDK 21, default image, single server; run the application, not Surefire |

With a nonempty pipeline `docker-img`, `test-adb-topology` runs `test` against that
image on `single` and `cluster`, retaining JDK 21 and `HTTP2_JSON`. The four
conditional version/JDK/protocol/Spring workflows above do not run. The tutorial
workflow still runs and still uses its job's default image: the pipeline override
is not forwarded to it. VST, SSL, and compression are not matrix dimensions here.

## Database setup

Use Bash, a Docker daemon, `curl`, and a disposable deployment reachable from the
Maven process. [docker/start_db.sh](../../../../docker/start_db.sh) provisions the
same authenticated server as CI. For the default job setup, run it once:

```sh
DOCKER_IMAGE=docker.io/arangodb/enterprise:latest \
STARTER_MODE=single ./docker/start_db.sh
```

Substitute only the image/topology required by the matrix row being reproduced.
Supply `ARANGO_LICENSE_KEY` through the environment when required for Enterprise;
do not commit credentials. A different available image may be useful diagnostically
but is not proof that the requested CI image passed.

The script uses network `arangodb` (`172.28.0.0/16`), containers `adb` and
`arangodb-data`, a Docker socket mount, and Starter-created child containers.
It is not an idempotent test launcher. Use a fresh disposable Docker environment
for a new deployment, or remove only resources verified to belong to your prior
run. Do not use global Docker prune/stop commands or remove someone else's data.

The default fixture connects as `root` with password `test` to `172.28.0.1:8529`.
Cluster coordinator ports are 8529, 8539, and 8549. Linux bridge routing is assumed
by the script; a Docker Desktop or remote daemon address is not automatically
reachable from the Maven host. For an already provisioned compatible test server,
append `-Darango.endpoints=host:port` to the Maven test command. The property is
`arango.endpoints`, while the protocol property is `arangodb.protocol`.

Tests use `spring-test-db` and additional multi-tenancy fixtures; the tutorial drops
`spring-demo`. Do not point either at a valuable server. Leave `SSL` and
`COMPRESSION` unset for these CI equivalents: script support is not test-matrix
coverage, and the fixture does not automatically configure TLS from those flags.

## Root test job

After database setup, use the chosen row's JDK and protocol:

```sh
mvn --version
mvn dependency:tree
mvn -Darangodb.protocol=HTTP2_JSON test
```

This runs the root suite, including database tests, through Surefire at `test`.
Use Surefire's `-Dtest=...` for focused diagnosis, not Failsafe's `-Dit.test`; CI
selects no extra test profile. Remove the selector to reproduce the whole job.
For the protocol matrix, run these four protocol values sequentially on the default
single-server deployment; JDK or server changes need the matching
runtime/deployment, not just another Maven property.

## Boot integration-test job

Use JDK 17 and the default single-server database. CI changes the Boot parent in
`integration-tests/pom.xml`, installs the root library, then tests the consumer:

```sh
boot_version=4.0.2
(
  cd integration-tests
  sed -i "0,/<version>.*<\/version>/s//<version>${boot_version}<\/version>/" pom.xml
)
mvn install -Dmaven.test.skip=true -Dgpg.skip=true -Dmaven.javadoc.skip=true
(
  cd integration-tests
  mvn --version
  mvn dependency:tree
  mvn -Darangodb.protocol=HTTP2_JSON test
)
```

The version substitution is the GNU `sed` command used by CI; it edits the first
version element, the Boot parent, not the library version or root Spring Data
parent. Use the checked-out matrix value; do not commit temporary matrix overrides.
Reinstall after library changes so the consumer does not test a stale local or
published artifact. The install step skips even test compilation and is setup,
not validation. Both GPG and Javadoc skip flags are intentional CI install flags.
Both consumers pin the library version literally (the integration project's own
`<version>`, the tutorial's dependency); it must equal the root version installed.

Keep `HTTP2_JSON` for this job: the consumer lacks the root's VelocyPack test
dependencies, so VPACK protocol failures there are not library regressions.

The consumer depends on root tests via filesystem symlinks. Preserve those links
when copying/checking out the project; do not replace them with duplicate tests.
Do not use `-pl integration-tests -am`: this is not a root reactor module.

## Tutorial job

Use JDK 21 and the default single-server deployment, then:

```sh
mvn install -Dmaven.test.skip=true -Dgpg.skip=true -Dmaven.javadoc.skip=true
(
  cd tutorial
  mvn spring-boot:run
)
```

`CrudRunner` exercises repository operations and assertions; `DemoApplication`
exits after running it. Successful compilation alone does not reproduce this job.
[AdbConfig](../../../../tutorial/src/main/java/com/arangodb/spring/demo/AdbConfig.java) hard-codes
`172.28.0.1:8529`, `root` / `test`; it does not read the tests' `arango.endpoints`
override. Keep routing consistent with CI or explicitly report the local deviation.

## Reports and release boundary

The CI `report` command is `mvn -e surefire-report:report-only`; it renders existing
results without rerunning tests. It runs at the root even in `integration-test`,
so inspect `integration-tests/target/surefire-reports/` for the actual Boot results
rather than treating a root site report as consumer test evidence.

The separate JDK 17 `deploy` job uses release credentials, skips tests, and is
filtered to tags beginning with `deploy-5.`. Do not invoke publishing or import
release keys to validate a code change. CI cache and cancellation steps are infrastructure, not
additional local test requirements.
