## Return types

The method return type for single results can be a primitive type, a domain class, `Map<String, Object>`, `BaseDocument`, `BaseEdgeDocument`, `Optional<Type>`, `GeoResult<Type>`.
The method return type for multiple results can additionally be `ArangoCursor<Type>`, `Iterable<Type>`, `Collection<Type>`, `List<Type>`, `Set<Type>`, `Page<Type>`, `Slice<Type>`, `GeoPage<Type>`, `GeoResults<Type>` where Type can be everything a single result can be.
