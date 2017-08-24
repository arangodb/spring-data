package com.arangodb.springframework.core.repository;

import com.arangodb.ArangoCursor;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.ArangoOperations.UpsertStrategy;
import com.arangodb.springframework.core.mapping.ArangoMappingContext;
import com.arangodb.springframework.core.repository.query.derived.DerivedQueryCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * The implementation of all CRUD, paging and sorting functionality in ArangoRepository from the
 * Spring Data Commons CRUD repository and PagingAndSorting repository
 */
@Repository
public class SimpleArangoRepository<T> implements ArangoRepository<T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleArangoRepository.class);

	private ArangoOperations arangoOperations;
	private ArangoExampleConverter exampleConverter;
	private Class<T> domainClass;

	/**
	 *
	 * @param arangoOperations
	 * 		The template used to execute much of the functionality of this class
	 * @param domainClass
	 * 		the class type of this repository
	 */
	public SimpleArangoRepository(ArangoOperations arangoOperations, Class<T> domainClass) {
		super();
		this.arangoOperations = arangoOperations;
		this.domainClass = domainClass;
		this.exampleConverter = new ArangoExampleConverter((ArangoMappingContext) arangoOperations.getConverter().getMappingContext());
	}

	/**
	 * Saves the passed entity to the database using upsert from the template
	 *
	 * @param entity
	 * 		the entity to be saved to the database
	 * @return
	 * 		the updated entity with any id/key/rev saved
	 */
	// TODO refactor once template.upsert() is implemented
	@Override public <S extends T> S save(S entity) {
		arangoOperations.upsert(entity, UpsertStrategy.UPDATE);
		return entity;
	}

	/**
	 * Saves the given iterable of entities to the database
	 *
	 *  @param entities
	 *  	the iterable of entities to be saved to the database
	 * 	@return
	 * 		the iterable of updated entities with any id/key/rev saved in each entity
	 */
	// TODO refactor once template.upsert() is implemented
	@Override public <S extends T> Iterable<S> save(Iterable<S> entities) {
		arangoOperations.upsert(entities, UpsertStrategy.UPDATE);
		return entities;
	}

	/**
	 * Finds if a document with the given id exists in the database
	 *
	 * @param id
	 * 		the id of the document to search for
	 * @return
	 * 		the object representing the document if found
	 */
	@Override public T findOne(String id) {
		return arangoOperations.find(id, domainClass);
	}

	/**
	 * Checks if a document exists or not based on the given id or key
	 *
	 * @param s
	 * 		represents either the key or id of a document to check for
	 * @return
	 * 		returns true if the document is found, false otherwise
	 */
	@Override public boolean exists(String s) {
		return arangoOperations.exists(s, domainClass);
	}

	/**
	 * Gets all documents in the collection for the class type of this repository
	 *
	 * @return
	 * 		an iterable with all the documents in the collection
	 */
	@Override public Iterable<T> findAll() {
		return arangoOperations.findAll(domainClass);
	}

	/**
	 * Finds all documents with the an id or key in the argument
	 *
	 * @param strings
	 * 		an iterable with ids/keys of documents to get
	 * @return
	 * 		an iterable with documents in the collection which have a id/key in the argument
	 */
	@Override public Iterable<T> findAll(Iterable<String> strings) {
		return arangoOperations.find(strings, domainClass);
	}

	/**
	 * Counts the number of documents in the collection for the type of this repository
	 *
	 * @return
	 * 		long with number of documents
	 */
	@Override public long count() {
		return arangoOperations.collection(domainClass).count();
	}

	/**
	 * Deletes the document with the given id or key
	 *
	 * @param s
	 * 		id or key of document to be deleted
	 */
	@Override public void delete(String s) {
		arangoOperations.delete(s, domainClass);
	}

	/**
	 * Deletes document in the database representing the given object, by getting it's id
	 *
	 * @param entity
	 * 		the entity to be deleted from the database
	 */
	@Override public void delete(T entity) {
		String id = null;
		try { id = (String) arangoOperations.getConverter().getMappingContext().getPersistentEntity(domainClass)
					.getIdProperty().getField().get(entity); }
		catch (IllegalAccessException e) { e.printStackTrace(); }
		arangoOperations.delete(id, domainClass);
	}

	/**
	 *	Deletes all the given documents from the database
	 *
	 * @param entities
	 * 		iterable of entities to be deleted from the database
	 */
	//TODO refactor if this can be done through one database call
	@Override public void delete(Iterable<? extends T> entities) {
		entities.forEach(this::delete);
	}

	/**
	 * Deletes all documents in the collection for this repository
	 */
	@Override public void deleteAll() {
		arangoOperations.collection(domainClass).truncate();
	}

	/**
	 * Gets all documents in the collection for the class type of this repository, with the given sort applied
	 *
	 * @param sort
	 * 		the sort object to use for sorting
	 * @return
	 * 		an iterable with all the documents in the collection
	 */
	@Override
	public Iterable<T> findAll(Sort sort) {
		String sortString = DerivedQueryCreator.buildSortString(sort);
		return execute("", sortString, "", new HashMap<>()).asListRemaining();
	}

	/**
	 * Gets all documents in the collection for the class type of this repository, with pagination
	 *
	 * @param pageable
	 * 		the pageable object to use for pagination of the results
	 * @return
	 * 		an iterable with all the documents in the collection
	 */
	@Override
	public Page<T> findAll(Pageable pageable) {
		if (pageable == null) { LOGGER.debug("Pageable in findAll(Pageable) is null"); }
		String sort = DerivedQueryCreator.buildSortString(pageable.getSort());
		String limit = String.format(" LIMIT %d, %d", pageable.getOffset(), pageable.getPageSize());
		ArangoCursor<T> result = execute("", sort, limit, new HashMap<>());
		List<T> content = result.asListRemaining();
		return new PageImpl<T>(content, pageable, result.getStats().getFullCount());
	}

	/**
	 * Gets the name of the collection for this repository
	 * @return
	 * 		the name of the collection
	 */
	private String getCollectionName() {
		return arangoOperations.getConverter().getMappingContext().getPersistentEntity(domainClass).getCollection();
	}

	/**
	 * 	Finds one document which matches the given example object
	 *
	 * @param example
	 * 		example object to construct query with
	 * @param <S>
	 * @return
	 * 		An object representing the example if it exists, else null
	 */
	@Override
	public <S extends T> S findOne(Example<S> example) {
		Map<String, Object> bindVars = new HashMap<>();
		String predicate = exampleConverter.convertExampleToPredicate(example, bindVars);
		String filter = predicate.length() == 0 ? "" : " FILTER " + predicate;
		ArangoCursor cursor = execute(filter, "", "", bindVars);
		return cursor.hasNext() ? (S) cursor.next() : null;
	}

	/**
	 * Finds all documents which match with the given example
	 *
	 * @param example
	 * 		example object to construct query with
	 * @param <S>
	 * @return
	 * 		iterable of all matching documents
	 */
	@Override
	public <S extends T> Iterable<S> findAll(Example<S> example) {
		Map<String, Object> bindVars = new HashMap<>();
		String predicate = exampleConverter.convertExampleToPredicate(example, bindVars);
		String filter = predicate.length() == 0 ? "" : " FILTER " + predicate;
		ArangoCursor cursor = execute(filter, "", "", bindVars);
		return cursor.asListRemaining();
	}

	/**
	 * Finds all documents which match with the given example, then apply the given sort to results
	 *
	 * @param example
	 * 		example object to construct query with
	 * @param sort
	 * 		sort object to sort results
	 * @param <S>
	 * @return
	 * 		sorted iterable of all matching documents
	 */
	@Override
	public <S extends T> Iterable<S> findAll(Example<S> example, Sort sort) {
		Map<String, Object> bindVars = new HashMap<>();
		String predicate = exampleConverter.convertExampleToPredicate(example, bindVars);
		String filter = predicate.length() == 0 ? "" : " FILTER " + predicate;
		String sortString = DerivedQueryCreator.buildSortString(sort);
		ArangoCursor cursor = execute(filter, sortString, "", bindVars);
		return cursor.asListRemaining();
	}

	/**
	 * Finds all documents which match with the given example, with pagination
	 *
	 * @param example
	 * 		example object to construct query with
	 * @param pageable
	 * 		pageable object to apply pagination with
	 * @param <S>
	 * @return
	 * 		iterable of all matching documents, with pagination
	 */
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

	/**
	 * Counts the number of documents in the collection which match with the given example
	 * @param example
	 * 		example object to construct query with
	 * @param <S>
	 * @return
	 * 		number of matching documents found
	 */
	@Override
	public <S extends T> long count(Example<S> example) {
		Map<String, Object> bindVars = new HashMap<>();
		String predicate = exampleConverter.convertExampleToPredicate(example, bindVars);
		String filter = predicate.length() == 0 ? "" : " FILTER " + predicate;
		String query = String.format("FOR e IN %s%s COLLECT WITH COUNT INTO length RETURN length", getCollectionName(), filter);
		ArangoCursor<Long> cursor = arangoOperations.query(query, bindVars, null, Long.class);
		return cursor.next();
	}

	/**
	 * Checks if any documents match with the given example
	 * @param example
	 * @param <S>
	 * @return
	 * 		true if any matches are found, else false
	 */
	@Override
	public <S extends T> boolean exists(Example<S> example) {
		return count(example) > 0;
	}

	/**
	 * Execute a query to the database
	 * @param filter
	 * 		filter statement to be put in query
	 * @param sort
	 * 		any sort to be applied
	 * @param limit
	 * 		a limit if one exists
	 * @param bindVars
	 * 		bindVars for the query being executed
	 * @return
	 * 		ArangoCursor<T> with the results of the executed query
	 */
	private ArangoCursor<T> execute(String filter, String sort, String limit, Map<String, Object> bindVars) {
		String query = String.format("FOR e IN %s%s%s%s RETURN e", getCollectionName(), filter, sort, limit);
		return arangoOperations.query(query, bindVars, limit.length() == 0 ? null : new AqlQueryOptions().fullCount(true), domainClass);
	}
}
