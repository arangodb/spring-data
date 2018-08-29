package com.arangodb.springframework.core.template;

import java.util.ArrayList;
import java.util.Collection;

import com.arangodb.ArangoSearch;

class ArangoSearchCacheValue {

	private final ArangoSearch view;
	private final Collection<Class<?>> entities;

	public ArangoSearchCacheValue(final ArangoSearch view) {
		super();
		this.view = view;
		this.entities = new ArrayList<>();
	}

	public ArangoSearch getView() {
		return view;
	}

	public Collection<Class<?>> getEntities() {
		return entities;
	}

	public void addEntityClass(final Class<?> entityClass) {
		entities.add(entityClass);
	}

}