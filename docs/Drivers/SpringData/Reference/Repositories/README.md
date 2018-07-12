# Repositories

## Introduction

Spring Data Commons provides a composable repository infrastructure which Spring Data ArangoDB is built on. These allow for interface-based composition of repositories consisting of provided default implementations for certain interfaces (like `CrudRepository`) and custom implementations for other methods.

## Instantiating

Instances of a Repository are created in Spring beans through the auto-wired mechanism of Spring.

```java
public class MySpringBean {

  @Autowired
  private MyRepository rep;

}
```
