package com.arangodb.springframework.core.template;

import java.util.ArrayList;
import java.util.Collection;

import com.arangodb.ArangoCollection;
import com.arangodb.entity.IndexEntity;

class CollectionCacheValue {

	private final ArangoCollection collection;
	private final Collection<Class<?>> entities;
	private final Collection<IndexEntity> indexes;

	public CollectionCacheValue(final ArangoCollection collection, Collection<IndexEntity> indexes) {
		super();
		this.collection = collection;
		this.entities = new ArrayList<>();
		this.indexes = indexes;
	}

	public ArangoCollection getCollection() {
		return collection;
	}

	public Collection<Class<?>> getEntities() {
		return entities;
	}

	public Collection<IndexEntity> getIndexes() {
		return indexes;
	}

	public boolean addEntityClass(final Class<?> entityClass) {
		return entities.add(entityClass);
	}

}