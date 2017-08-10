package com.arangodb.springframework.core.repository;

import com.arangodb.ArangoCursor;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.convert.DBDocumentEntity;
import com.arangodb.springframework.core.convert.DBEntity;
import com.arangodb.springframework.core.mapping.ArangoMappingContext;
import com.arangodb.springframework.core.repository.query.derived.DerivedQueryCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * Created by F625633 on 06/07/2017.
 */
@Repository
public class SimpleArangoRepository<T> implements ArangoRepository<T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleArangoRepository.class);

	private ArangoOperations arangoOperations;
	private ArangoExampleConverter exampleConverter;
	private Class<T> domainClass;


	public SimpleArangoRepository(ArangoOperations arangoOperations, Class<T> domainClass) {
		super();
		this.arangoOperations = arangoOperations;
		this.domainClass = domainClass;
		this.exampleConverter = new ArangoExampleConverter((ArangoMappingContext) arangoOperations.getConverter().getMappingContext());
	}

	@Override public <S extends T> S save(S entity) {
		try { arangoOperations.insertDocument(entity); }
		catch (Exception e) {
			DBEntity dbEntity = new DBDocumentEntity();
			arangoOperations.getConverter().write(entity, dbEntity);
			arangoOperations.updateDocument((String) dbEntity.get("_id"), entity);
		}
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
		return arangoOperations.getDocuments(strings, domainClass);
	}

	@Override public long count() {
		return arangoOperations.collection(domainClass).count();
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
		arangoOperations.collection(domainClass).truncate();
	}

	@Override
	public Iterable<T> findAll(Sort sort) {
		String sortString = DerivedQueryCreator.buildSortString(sort);
		return execute("", sortString, "", new HashMap<>()).asListRemaining();
	}

	@Override
	public Page<T> findAll(Pageable pageable) {
		if (pageable == null) { LOGGER.debug("Pageable in findAll(Pageable) is null"); }
		String sort = DerivedQueryCreator.buildSortString(pageable.getSort());
		String limit = String.format(" LIMIT %d, %d", pageable.getOffset(), pageable.getPageSize());
		ArangoCursor<T> result = execute("", sort, limit, new HashMap<>());
		List<T> content = result.asListRemaining();
		return new PageImpl<T>(content, pageable, result.getStats().getFullCount());
	}

	private String getCollectionName() {
		return arangoOperations.getConverter().getMappingContext().getPersistentEntity(domainClass).getCollection();
	}

	@Override
	public <S extends T> S findOne(Example<S> example) {
		Map<String, Object> bindVars = new HashMap<>();
		String predicate = exampleConverter.convertExampleToPredicate(example, bindVars);
		String filter = predicate.length() == 0 ? "" : " FILTER " + predicate;
		ArangoCursor cursor = execute(filter, "", "", bindVars);
		return cursor.hasNext() ? (S) cursor.next() : null;
	}

	@Override
	public <S extends T> Iterable<S> findAll(Example<S> example) {
		Map<String, Object> bindVars = new HashMap<>();
		String predicate = exampleConverter.convertExampleToPredicate(example, bindVars);
		String filter = predicate.length() == 0 ? "" : " FILTER " + predicate;
		ArangoCursor cursor = execute(filter, "", "", bindVars);
		return cursor.asListRemaining();
	}

	@Override
	public <S extends T> Iterable<S> findAll(Example<S> example, Sort sort) {
		Map<String, Object> bindVars = new HashMap<>();
		String predicate = exampleConverter.convertExampleToPredicate(example, bindVars);
		String filter = predicate.length() == 0 ? "" : " FILTER " + predicate;
		String sortString = DerivedQueryCreator.buildSortString(sort);
		ArangoCursor cursor = execute(filter, sortString, "", bindVars);
		return cursor.asListRemaining();
	}

	@Override
	public <S extends T> Page<S> findAll(Example<S> example, Pageable pageable) {
		Map<String, Object> bindVars = new HashMap<>();
		String predicate = exampleConverter.convertExampleToPredicate(example, bindVars);
		String filter = predicate.length() == 0 ? "" : " FILTER " + predicate;
		String sortString = DerivedQueryCreator.buildSortString(pageable.getSort());
		String limit = String.format(" LIMIT %d, %d", pageable.getOffset(), pageable.getPageSize());
		ArangoCursor cursor = execute(filter, sortString, limit, bindVars);
		List<T> content = cursor.asListRemaining();
		return new PageImpl<S>((List<S>) content, pageable, cursor.getStats().getFullCount());
	}

	@Override
	public <S extends T> long count(Example<S> example) {
		Map<String, Object> bindVars = new HashMap<>();
		String predicate = exampleConverter.convertExampleToPredicate(example, bindVars);
		String filter = predicate.length() == 0 ? "" : " FILTER " + predicate;
		String query = String.format("FOR e IN %s%s COLLECT WITH COUNT INTO length RETURN length", getCollectionName(), filter);
		ArangoCursor<Long> cursor = arangoOperations.query(query, bindVars, null, Long.class);
		return cursor.next();
	}

	@Override
	public <S extends T> boolean exists(Example<S> example) {
		return count(example) > 0;
	}

	private ArangoCursor<T> execute(String filter, String sort, String limit, Map<String, Object> bindVars) {
		String query = String.format("FOR e IN %s%s%s%s RETURN e", getCollectionName(), filter, sort, limit);
		return arangoOperations.query(query, bindVars, limit.length() == 0 ? null : new AqlQueryOptions().fullCount(true), domainClass);
	}
}
