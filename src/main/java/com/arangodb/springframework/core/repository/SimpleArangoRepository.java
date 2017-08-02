package com.arangodb.springframework.core.repository;

import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.repository.query.derived.DerivedQueryCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

import java.util.*;

/**
 * Created by F625633 on 06/07/2017.
 */
@Repository
public class SimpleArangoRepository<T> implements ArangoRepository<T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleArangoRepository.class);

	private ArangoOperations arangoOperations;
	private Class<T> domainClass;


	public SimpleArangoRepository(ArangoOperations arangoOperations, Class<T> domainClass) {
		super();
		this.arangoOperations = arangoOperations;
		this.domainClass = domainClass;
	}

	@Override public <S extends T> S save(S entity) {
		arangoOperations.insertDocument(entity);
		return entity;
	}

	@Override public <S extends T> Iterable<S> save(Iterable<S> entities) {
		Collection<Object> entityCollection = new ArrayList<>();
		entities.forEach(entityCollection::add);
		arangoOperations.insertDocuments(entityCollection, this.domainClass);
		return entities;
	}

	@Override public T findOne(String id) {
		return arangoOperations.getDocument(id, domainClass);
	}

	@Override public boolean exists(String s) {
		return arangoOperations.exists(s, domainClass);
	}

	@Override public Iterable<T> findAll() {
		return arangoOperations.getDocuments(domainClass);
	}

	@Override public Iterable<T> findAll(Iterable<String> strings) {
		return arangoOperations.getDocuments(domainClass, strings);
	}

	@Override public long count() {
		return arangoOperations.count(domainClass);
	}

	@Override public void delete(String s) {
		arangoOperations.deleteDocument(s, domainClass);
	}

	@Override public void delete(T entity) {
		String id = null;
		try { id = (String) arangoOperations.getConverter().getMappingContext().getPersistentEntity(domainClass)
					.getIdProperty().getField().get(entity); }
		catch (IllegalAccessException e) { e.printStackTrace(); }
		arangoOperations.deleteDocument(id, domainClass);
	}

	@Override public void delete(Iterable<? extends T> entities) {
		entities.forEach(this::delete);
	}

	@Override public void deleteAll() {
		arangoOperations.deleteDocuments(domainClass);
	}

	@Override
	public Iterable<T> findAll(Sort sort) {
		String query = String.format("FOR e IN %s%s RETURN e", getCollectionName(), DerivedQueryCreator.buildSortString(sort));
		return arangoOperations.query(query, new HashMap<>(), null, domainClass).asListRemaining();
	}

	@Override
	public Page<T> findAll(Pageable pageable) {
		if (pageable == null) LOGGER.debug("Pageable in findAll(Pageable) is null");
		String query = String.format("FOR e IN %s%s LIMIT %d, %d RETURN e",
				getCollectionName(), DerivedQueryCreator.buildSortString(pageable.getSort()), pageable.getOffset(), pageable.getPageSize());
		List<T> content = (List<T>) arangoOperations.query(query, new HashMap<>(), null, domainClass).asListRemaining();
		return new PageImpl<T>(content, pageable, count());
	}

	private String getCollectionName() {
		return arangoOperations.getConverter().getMappingContext().getPersistentEntity(domainClass).getCollection();
	}
}
