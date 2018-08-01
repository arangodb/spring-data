# Migrating Spring Data ArangoDB 2.x to 3.0

## Annotations @Key

The annotation `@Key` is removed. Use `@Id` instead.

## Annotations @Id

The annotation `@Id` in now saved in the database as field `_key` instead of `_id`. All operations in `ArangoOperations` and `ArangoRepository` still work with `@Id` and also now supports non-String fields.

If you - for some reason - need the value of `_id` within your application, you can use the annotatioon `@ArangoId` on a `String` field instead of `@Id`.

## ArangoRepository

`ArangoRepository` now requires a second generic type. This type `ID` represents the type of your domain object field annotated with `@Id`.

**Examples**

```Java
public class Customer {
  @Id private String id;
}

public interface CustomerRepository extends ArangoRepository<Customer, String> {

}
```

## Annotation @Param

The annotation `com.arangodb.springframework.annotation.Param` is removed. Use `org.springframework.data.repository.query.Param` instead.

## DBEntity / DBCollectionEntity

Both `DBEntity` and `DBCollectionEntity` are removed. Use `DBDocumentEntity` instead.
