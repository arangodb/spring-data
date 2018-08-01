# Converter

## Registering a Spring Converter

The `AbstractArangoConfiguration` provides a convenient way to register Spring `Converter` by overriding the method `customConverters()`.

**Examples**

```Java
@Configuration
public class MyConfiguration extends AbstractArangoConfiguration {

  @Override
  protected Collection<Converter<?, ?>> customConverters() {
    Collection<Converter<?, ?>> converters = new ArrayList<>();
    converters.add(new MyConverter());
    return converters;
  }

}
```

## Implementing a Spring Converter

A `Converter` is used for reading or writing if one of the types (source, target) is of type `DBDocumentEntity` or `VPackSlice`.

**Examples**

```Java
public class MyConverter implements Converter<MyObject, DBDocumentEntity> {

  @Override
  public DBDocumentEntity convert(final MyObject source) {
    DBDocumentEntity entity = new DBDocumentEntity();
    // convert MyObject to DBDocumentEntity
    return entity;
  }

}
```
