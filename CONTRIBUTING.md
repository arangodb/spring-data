# Contributing to [Spring Data ArangoDB](README.md)

## Build

The project is built using [Maven](https://maven.apache.org) with JDK 17.

```
mvn clean compile
```

## Test

Tests are written using [JUnit 5](https://junit.org/junit5/) and [Hamcrest](https://hamcrest.org/JavaHamcrest/)
and run using `mvn test`.

To run tests integrated with a database server, the easiest way is to use docker to spin up a single server:

```
docker run -e ARANGO_NO_AUTH=1 -p 8529:8529 arangodb:latest arangod
```

More server versions and starter modes will be checked for a pull request. 

## Contribute

To contribute code:

1. create a fork of this repository
2. create a branch on the fork
3. commit and push your changes to your branch
4. make sure the tests run on your local machine
5. create a pull request from your branch to `master` of this repository
6. make sure all checks are green for your pull request (only with CLA signed) 

Do not change the project version, this will be done after merge on master.

To accept external contributions, we need you to fill out and sign our
[Individual Contributor License Agreement](https://www.arangodb.com/documents/cla.pdf) (CLA). We use an Apache 2 CLA
for ArangoDB. You can scan and email the CLA PDF file to [cla@arangodb.com](mailto:cla@arangodb.com) or send it via fax
to +49-221-2722999-88.

