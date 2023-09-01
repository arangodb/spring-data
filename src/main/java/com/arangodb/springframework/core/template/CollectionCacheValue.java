package com.arangodb.springframework.core.template;

import java.util.ArrayList;
import java.util.Collection;

import com.arangodb.ArangoCollection;

class CollectionCacheValue {

	private final ArangoCollection collection;
	private final Collection<Class<?>> entities;

	public CollectionCacheValue(final ArangoCollection collection) {
		super();
		this.collection = collection;
		this.entities = new ArrayList<>();
	}

	public ArangoCollection getCollection() {
		return collection;
	}

	public Collection<Class<?>> getEntities() {
		return entities;
	}

	public void addEntityClass(final Class<?> entityClass) {
		entities.add(entityClass);
	}

}