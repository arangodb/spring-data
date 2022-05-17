/*
 * DISCLAIMER
 *
 * Copyright 2017 ArangoDB GmbH, Cologne, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright holder is ArangoDB GmbH, Cologne, Germany
 */

package com.arangodb.springframework.repository;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDBException;
import com.arangodb.entity.DocumentDeleteEntity;
import com.arangodb.entity.ErrorEntity;
import com.arangodb.entity.MultiDocumentEntity;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.model.DocumentDeleteOptions;
import com.arangodb.model.DocumentExistsOptions;
import com.arangodb.model.DocumentReadOptions;
import com.arangodb.springframework.core.DocumentNotFoundException;
import com.arangodb.springframework.core.convert.ArangoConverter;
import com.arangodb.springframework.core.mapping.ArangoMappingContext;
import com.arangodb.springframework.core.mapping.ArangoPersistentEntity;
import com.arangodb.springframework.core.template.ArangoTemplate;
import com.arangodb.springframework.core.util.AqlUtils;
import com.arangodb.springframework.repository.query.QueryTransactionBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.function.Function;

/**
 * The implementation of all CRUD, paging and sorting functionality in
 * ArangoRepository from the Spring Data Commons CRUD repository and
 * PagingAndSorting repository
 */
@Repository
@SuppressWarnings({ "rawtypes", "unchecked" })
public class SimpleArangoRepository<T, ID> implements ArangoRepository<T, ID> {

	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleArangoRepository.class);

	private final ArangoTemplate arangoTemplate;
	private final ArangoConverter converter;
	private final ArangoMappingContext mappingContext;
	private final ArangoExampleConverter exampleConverter;
	private final Class<T> domainClass;
	private final boolean returnOriginalEntities;
    private final ArangoPersistentEntity<?> persistentEntity;
	private final QueryTransactionBridge transactionBridge;

	/**
	 * @param arangoTemplate       The template used to execute much of the
	 *                               functionality of this class
	 * @param domainClass            the class type of this repository
	 * @param returnOriginalEntities whether save and saveAll should return the
	 *                               original entities or new ones
	 * @param transactionBridge the optional transaction bridge
	 */
	public SimpleArangoRepository(final ArangoTemplate arangoTemplate, final Class<T> domainClass, boolean returnOriginalEntities, final QueryTransactionBridge transactionBridge) {
		super();
		this.arangoTemplate = arangoTemplate;
		this.domainClass = domainClass;
		this.returnOriginalEntities = returnOriginalEntities;
        this.transactionBridge = transactionBridge;
		converter = arangoTemplate.getConverter();
		mappingContext = (ArangoMappingContext) converter.getMappingContext();
		exampleConverter = new ArangoExampleConverter(mappingContext, arangoTemplate.getResolverFactory());
        persistentEntity = mappingContext.getRequiredPersistentEntity(domainClass);
	}

	/**
	 * Saves the passed entity to the database using repsert from the template
	 *
	 * @param entity the entity to be saved to the database
	 * @return the updated entity with any id/key/rev saved
	 */
	@Override
	public <S extends T> S save(final S entity) {
		S saved = arangoTemplate.repsert(entity, defaultQueryOptions());
		return returnOriginalEntities ? entity : saved;
	}

	/**
	 * Saves the given iterable of entities to the database using repsert from the template
	 *
	 * @param entities the iterable of entities to be saved to the database
	 * @return the iterable of updated entities with any id/key/rev saved in each
	 *         entity
	 */
	@Override
	public <S extends T> Iterable<S> saveAll(final Iterable<S> entities) {
		Iterable<S> saved = arangoTemplate.repsertAll(entities, domainClass, defaultQueryOptions());
		return returnOriginalEntities ? entities : saved;
	}

	/**
	 * Finds if a document with the given id exists in the database
	 *
	 * @param id the id of the document to search for
	 * @return the object representing the document if found
	 */
	@Override
	public Optional<T> findById(final ID id) {
		return arangoTemplate.find(id, domainClass, defaultReadOptions());
	}

	/**
	 * Checks if a document exists or not based on the given id or key
	 *
	 * @param id represents either the key or id of a document to check for
	 * @return returns true if the document is found, false otherwise
	 */
	@Override
	public boolean existsById(final ID id) {
		return arangoTemplate.exists(id, domainClass, defaultExistsOptions());
	}

	/**
	 * Gets all documents in the collection for the class type of this repository
	 *
	 * @return an iterable with all the documents in the collection
	 */
	@Override
	public Iterable<T> findAll() {
		return arangoTemplate.findAll(domainClass, defaultReadOptions());
	}

	/**
	 * Finds all documents with the an id or key in the argument
	 *
	 * @param ids an iterable with ids/keys of documents to get
	 * @return an iterable with documents in the collection which have a id/key in
	 *         the argument
	 */
	@Override
	public Iterable<T> findAllById(final Iterable<ID> ids) {
		return arangoTemplate.findAll(ids, domainClass, defaultReadOptions());
	}

	/**
	 * Counts the number of documents in the collection for the type of this
	 * repository
	 *
	 * @return long with number of documents
	 */
	@Override
	public long count() {
		return arangoTemplate.collection(domainClass).count();
	}

	/**
	 * Deletes the document with the given id or key
	 *
	 * @param id id or key of document to be deleted
	 */
	@Override
	public void deleteById(final ID id) {
		try {
			arangoTemplate.delete(id, domainClass, defaultDeleteOptions());
		} catch (DocumentNotFoundException unknown) {
//			 silently ignored
		}
	}

	/**
	 * Deletes document in the database representing the given object, by getting
	 * it's id
	 *
	 * @param entity the entity to be deleted from the database
	 */
    @Override
    public void delete(final T entity) {
		Object id = persistentEntity.getIdentifierAccessor(entity).getRequiredIdentifier();
        DocumentDeleteOptions opts = new DocumentDeleteOptions();
		persistentEntity.getRevProperty()
                .map(persistentEntity.getPropertyAccessor(entity)::getProperty)
				.map(r -> converter.convertIfNecessary(r, String.class))
                .ifPresent(opts::ifMatch);

        try {
            arangoTemplate.delete(id, opts, domainClass, defaultDeleteOptions());
        } catch (DocumentNotFoundException e) {
            throw new OptimisticLockingFailureException(e.getMessage(), e);
        }
    }

    /**
     * Deletes all instances of the type {@code T} with the given IDs.
	 * @implNote do not add @Override annotation to keep backwards compatibility with spring-data-commons 2.4
     */
	public void deleteAllById(Iterable<? extends ID> ids) {
		MultiDocumentEntity<DocumentDeleteEntity<?>> res = arangoTemplate.deleteAllById(ids, domainClass, defaultDeleteOptions());
		for (ErrorEntity error : res.getErrors()) {
			// Entities that aren't found in the persistence store are silently ignored.
			if (error.getErrorNum() != 1202) {
				throw arangoTemplate.translateException(new ArangoDBException(error));
			}
		}
	}

	/**
	 * Deletes all the given documents from the database
	 *
	 * @param entities iterable of entities to be deleted from the database
	 */
	@Override
	public void deleteAll(final Iterable<? extends T> entities) {
		entities.forEach(this::delete);
	}

	/**
	 * Deletes all documents in the collection for this repository
	 */
	@Override
	public void deleteAll() {
		arangoTemplate.collection(domainClass).truncate();
	}

	/**
	 * Gets all documents in the collection for the class type of this repository,
	 * with the given sort applied
	 *
	 * @param sort the sort object to use for sorting
	 * @return an iterable with all the documents in the collection
	 */
	@Override
	public Iterable<T> findAll(final Sort sort) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return findAllInternal(sort, null, new HashMap<>());
			}
		};
	}

	/**
	 * Gets all documents in the collection for the class type of this repository,
	 * with pagination
	 *
	 * @param pageable the pageable object to use for pagination of the results
	 * @return an iterable with all the documents in the collection
	 */
	@Override
	public Page<T> findAll(final Pageable pageable) {
		if (pageable == null) {
			LOGGER.debug("Pageable in findAll(Pageable) is null");
		}

		final ArangoCursor<T> result = findAllInternal(pageable, null, new HashMap<>());
		final List<T> content = result.asListRemaining();
		return new PageImpl<>(content, pageable, ((Number) result.getStats().getFullCount()).longValue());
	}

	/**
	 * Gets the name of the collection for this repository
	 *
	 * @return the name of the collection
	 */
	private String getCollectionName() {
		return persistentEntity.getCollection();
	}

	/**
	 * Finds one document which matches the given example object
	 *
	 * @param example example object to construct query with
	 * @param <S>
	 * @return An object representing the example if it exists, else null
	 */
	@Override
	public <S extends T> Optional<S> findOne(final Example<S> example) {
		final ArangoCursor cursor = findAllInternal((Pageable) null, example, new HashMap());
		return cursor.hasNext() ? Optional.ofNullable((S) cursor.next()) : Optional.empty();
	}

	/**
	 * Finds all documents which match with the given example
	 *
	 * @param example example object to construct query with
	 * @param <S>
	 * @return iterable of all matching documents
	 */
	@Override
	public <S extends T> Iterable<S> findAll(final Example<S> example) {
		return (ArangoCursor) findAllInternal((Pageable) null, example, new HashMap<>());
	}

	/**
	 * Finds all documents which match with the given example, then apply the given
	 * sort to results
	 *
	 * @param example example object to construct query with
	 * @param sort    sort object to sort results
	 * @param <S>
	 * @return sorted iterable of all matching documents
	 */
	@Override
	public <S extends T> Iterable<S> findAll(final Example<S> example, final Sort sort) {
		return findAllInternal(sort, example, new HashMap());
	}

	/**
	 * Finds all documents which match with the given example, with pagination
	 *
	 * @param example  example object to construct query with
	 * @param pageable pageable object to apply pagination with
	 * @param <S>
	 * @return iterable of all matching documents, with pagination
	 */
	@Override
	public <S extends T> Page<S> findAll(final Example<S> example, final Pageable pageable) {
		final ArangoCursor cursor = findAllInternal(pageable, example, new HashMap());
		final List<T> content = cursor.asListRemaining();
		return new PageImpl<>((List<S>) content, pageable, ((Number) cursor.getStats().getFullCount()).longValue());
	}

	/**
	 * Counts the number of documents in the collection which match with the given
	 * example
	 *
	 * @param example example object to construct query with
	 * @param <S>
	 * @return number of matching documents found
	 */
	@Override
	public <S extends T> long count(final Example<S> example) {
		final Map<String, Object> bindVars = new HashMap<>();
		bindVars.put("@col", getCollectionName());
		final String predicate = exampleConverter.convertExampleToPredicate(example, bindVars);
		final String filter = predicate.length() == 0 ? "" : " FILTER " + predicate;
		final String query = String.format("FOR e IN @@col %s COLLECT WITH COUNT INTO length RETURN length", filter);
		arangoTemplate.collection(domainClass);
		final ArangoCursor<Long> cursor = arangoTemplate.query(query, bindVars, defaultQueryOptions(), Long.class);
		return cursor.next();
	}

	/**
	 * Checks if any documents match with the given example
	 *
	 * @param example
	 * @param <S>
	 * @return true if any matches are found, else false
	 */
	@Override
	public <S extends T> boolean exists(final Example<S> example) {
		return count(example) > 0;
	}

	public <S extends T, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
		throw new UnsupportedOperationException();
	}

	private <S extends T> ArangoCursor<T> findAllInternal(final Sort sort, @Nullable final Example<S> example,
			final Map<String, Object> bindVars) {
		bindVars.put("@col", getCollectionName());
		final String query = String.format("FOR e IN @@col %s %s RETURN e",
				buildFilterClause(example, bindVars), buildSortClause(sort, "e"));
		arangoTemplate.collection(domainClass);
		return arangoTemplate.query(query, bindVars, defaultQueryOptions(), domainClass);
	}

	private <S extends T> ArangoCursor<T> findAllInternal(final Pageable pageable, @Nullable final Example<S> example,
			final Map<String, Object> bindVars) {
		bindVars.put("@col", getCollectionName());
		final String query = String.format("FOR e IN @@col %s %s RETURN e",
				buildFilterClause(example, bindVars), buildPageableClause(pageable, "e"));
		arangoTemplate.collection(domainClass);
		return arangoTemplate.query(query, bindVars,
				pageable != null ? new AqlQueryOptions().fullCount(true) : defaultQueryOptions(), domainClass);
	}

	private <S extends T> String buildFilterClause(final Example<S> example, final Map<String, Object> bindVars) {
		if (example == null) {
			return "";
		}

		final String predicate = exampleConverter.convertExampleToPredicate(example, bindVars);
		return predicate == null ? "" : "FILTER " + predicate;
	}

    private String buildPageableClause(final Pageable pageable, final String varName) {
        if (pageable == null) return "";
        Sort persistentSort = AqlUtils.toPersistentSort(pageable.getSort(), mappingContext, domainClass);
		Pageable persistentPageable;
		if (pageable.isPaged()) {
			persistentPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), persistentSort);
		} else {
			persistentPageable = pageable;
		}
        return AqlUtils.buildPageableClause(persistentPageable, varName);
    }

    private String buildSortClause(final Sort sort, final String varName) {
        return sort == null ? "" : AqlUtils.buildSortClause(AqlUtils.toPersistentSort(sort, mappingContext, domainClass), varName);
    }

	private DocumentReadOptions defaultReadOptions() {
		DocumentReadOptions options = new DocumentReadOptions();
		if (transactionBridge != null) {
			options.streamTransactionId(transactionBridge.getCurrentTransaction(Collections.singleton(getCollectionName())));
		}
		return options;
	}

	private AqlQueryOptions defaultQueryOptions() {
		AqlQueryOptions options = new AqlQueryOptions();
		if (transactionBridge != null) {
			options.streamTransactionId(transactionBridge.getCurrentTransaction(Collections.singleton(getCollectionName())));
		}
		return options;
	}

	private DocumentExistsOptions defaultExistsOptions() {
		DocumentExistsOptions options = new DocumentExistsOptions();
		if (transactionBridge != null) {
			options.streamTransactionId(transactionBridge.getCurrentTransaction(Collections.singleton(getCollectionName())));
		}
		return options;
	}

	private DocumentDeleteOptions defaultDeleteOptions() {
		DocumentDeleteOptions options = new DocumentDeleteOptions();
		if (transactionBridge != null) {
			options.streamTransactionId(transactionBridge.getCurrentTransaction(Collections.singleton(getCollectionName())));
		}
		return options;
	}

}
