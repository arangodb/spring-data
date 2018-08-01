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

A `Converter` is used for reading or writing if one of the types (source, target) is of type `VPackSlice` or `DBDocumentEntity`.

**Examples**

```Java
public class MyConverter implements Converter<MyObject, VPackSlice> {

  @Override
  public VPackSlice convert(final MyObject source) {
    VPackBuilder builder = new VPackBuilder();
    // map fields of MyObject to builder
    return builder.slice();
  }

}
```

For performance reasons `VPackSlice` should always be used within a converter. If your object is too complexe, you can also use `DBDocumentEntity` to simplify the mapping.
