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

package com.arangodb.springframework.core.template;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.support.PersistenceExceptionTranslator;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.ArangoDBVersion;
import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.entity.DocumentDeleteEntity;
import com.arangodb.entity.DocumentUpdateEntity;
import com.arangodb.entity.MultiDocumentEntity;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.model.CollectionCreateOptions;
import com.arangodb.model.DocumentCreateOptions;
import com.arangodb.model.DocumentDeleteOptions;
import com.arangodb.model.DocumentReadOptions;
import com.arangodb.model.DocumentReplaceOptions;
import com.arangodb.model.DocumentUpdateOptions;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.convert.ArangoConverter;
import com.arangodb.springframework.core.convert.DBCollectionEntity;
import com.arangodb.springframework.core.convert.DBDocumentEntity;
import com.arangodb.springframework.core.convert.DBEntity;
import com.arangodb.springframework.core.convert.DBEntityDeserializer;
import com.arangodb.springframework.core.mapping.ArangoPersistentEntity;
import com.arangodb.springframework.core.util.ArangoExceptionTranslator;

/**
 * @author Mark Vollmary
 *
 */
public class ArangoTemplate implements ArangoOperations {

	private final PersistenceExceptionTranslator exceptionTranslator;
	private final ArangoConverter converter;
	private final ArangoDB arango;
	private ArangoDatabase database;
	private final String databaseName;
	private final Map<String, ArangoCollection> collectionCache;

	public ArangoTemplate(final ArangoDB.Builder arango, final String database) {
		this(arango, database, null);
	}

	public ArangoTemplate(final ArangoDB.Builder arango, final String database, final ArangoConverter converter) {
		this(arango, database, converter, new ArangoExceptionTranslator());
	}

	public ArangoTemplate(final ArangoDB.Builder arango, final String database, final ArangoConverter converter,
		final PersistenceExceptionTranslator exceptionTranslator) {
		super();
		this.arango = arango.registerDeserializer(DBEntity.class, new DBEntityDeserializer()).build()
				._setCursorInitializer(
					new com.arangodb.springframework.core.template.ArangoCursorInitializer(converter));
		this.databaseName = database;
		this.converter = converter;
		this.exceptionTranslator = exceptionTranslator;
		collectionCache = new HashMap<>();
	}

	private ArangoDatabase db() {
		if (database == null) {
			database = arango.db(databaseName);
			try {
				database.getInfo();
			} catch (final ArangoDBException e) {
				if (new Integer(404).equals(e.getResponseCode())) {
					arango.createDatabase(databaseName);
				}
			}
		}
		return database;
	}

	private DataAccessException translateExceptionIfPossible(final RuntimeException exception) {
		return exceptionTranslator.translateExceptionIfPossible(exception);
	}

	private ArangoCollection collection(final Class<?> entityClass) {
		return collection(entityClass, null);
	}

	private ArangoCollection collection(final Class<?> entityClass, final String id) {
		final String name = determineCollectionFromId(Optional.ofNullable(id))
				.orElse(getPersistentEntity(entityClass).getCollection());
		ArangoCollection collection = collectionCache.get(name);
		if (collection == null) {
			collection = db().collection(name);
			try {
				collection.getInfo();
			} catch (final ArangoDBException e) {
				if (new Integer(404).equals(e.getResponseCode())) {
					createCollection(name, getPersistentEntity(entityClass));
				}
			}
			collectionCache.put(name, collection);
		}
		return collection;
	}

	private void createCollection(final String name, final ArangoPersistentEntity<?> persistentEntity) {
		db().createCollection(name, new CollectionCreateOptions().type(persistentEntity.getCollectionType()));
	}

	private ArangoPersistentEntity<?> getPersistentEntity(final Class<?> entityClass) {
		final ArangoPersistentEntity<?> persistentEntity = converter.getMappingContext()
				.getPersistentEntity(entityClass);
		if (persistentEntity == null) {
			new InvalidDataAccessApiUsageException(
					"No persistent entity information found for the type " + entityClass.getName());
		}
		return persistentEntity;
	}

	private Optional<String> determineCollectionFromId(final Optional<String> id) {
		return id.map(i -> {
			final String[] split = i.split("/");
			return split.length == 2 ? split[0] : null;
		});
	}

	private String determineDocumentKeyFromId(final String id) {
		final String[] split = id.split("/");
		return split[split.length - 1];
	}

	private DBEntity toDBEntity(final Object value) {
		final DBEntity entity = converter.isCollectionType(value.getClass()) ? new DBCollectionEntity()
				: new DBDocumentEntity();
		converter.write(value, entity);
		return entity;
	}

	private <T> T fromDBEntity(final Class<T> type, final DBEntity doc) {
		return converter.read(type, doc);
	}

	@Override
	public ArangoDB driver() {
		return arango;
	}

	@Override
	public ArangoDBVersion getVersion() throws DataAccessException {
		try {
			return arango.getVersion();
		} catch (final ArangoDBException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> ArangoCursor<T> query(
		final String query,
		final Map<String, Object> bindVars,
		final AqlQueryOptions options,
		final Class<T> type) throws DataAccessException {
		return db().query(query, DBDocumentEntity.class.cast(toDBEntity(prepareBindVars(bindVars))), options, type);
	}

	private Map<String, Object> prepareBindVars(final Map<String, Object> bindVars) {
		for (final Map.Entry<String, Object> entry : new HashMap<>(bindVars).entrySet()) {
			if (entry.getKey().startsWith("@") && Class.class.isAssignableFrom(entry.getValue().getClass())) {
				bindVars.put(entry.getKey(), collection((Class<?>) entry.getValue()).name());
			}
		}
		return bindVars;
	}

	@Override
	public <T> MultiDocumentEntity<DocumentDeleteEntity<T>> deleteDocuments(
		final Collection<Object> values,
		final Class<T> type,
		final DocumentDeleteOptions options) throws DataAccessException {
		try {
			return collection(type).deleteDocuments(DBCollectionEntity.class.cast(toDBEntity(values)), type, options);
		} catch (final ArangoDBException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> MultiDocumentEntity<DocumentDeleteEntity<T>> deleteDocuments(
		final Collection<Object> values,
		final Class<T> type) throws DataAccessException {
		return deleteDocuments(values, type, new DocumentDeleteOptions());
	}

	@Override
	public <T> DocumentDeleteEntity<Void> deleteDocument(final String id, final Class<T> type)
			throws DataAccessException {
		try {
			return collection(type, id).deleteDocument(determineDocumentKeyFromId(id));
		} catch (final ArangoDBException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> DocumentDeleteEntity<T> deleteDocument(
		final String id,
		final Class<T> type,
		final DocumentDeleteOptions options) throws DataAccessException {
		try {
			return collection(type, id).deleteDocument(determineDocumentKeyFromId(id), type, options);
		} catch (final ArangoDBException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public MultiDocumentEntity<DocumentUpdateEntity<Object>> updateDocuments(
		final Collection<Object> values,
		final Class<?> type,
		final DocumentUpdateOptions options) throws DataAccessException {
		try {
			return collection(type).updateDocuments(DBCollectionEntity.class.cast(toDBEntity(values)), options);
		} catch (final ArangoDBException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public MultiDocumentEntity<DocumentUpdateEntity<Object>> updateDocuments(
		final Collection<Object> values,
		final Class<?> type) throws DataAccessException {
		return updateDocuments(values, type, new DocumentUpdateOptions());
	}

	@Override
	public DocumentUpdateEntity<Object> updateDocument(
		final String id,
		final Object value,
		final DocumentUpdateOptions options) throws DataAccessException {
		try {
			return collection(value.getClass(), id).updateDocument(determineDocumentKeyFromId(id), toDBEntity(value));
		} catch (final ArangoDBException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public DocumentUpdateEntity<Object> updateDocument(final String id, final Object value) throws DataAccessException {
		return updateDocument(id, value, new DocumentUpdateOptions());
	}

	@Override
	public MultiDocumentEntity<DocumentUpdateEntity<Object>> replaceDocuments(
		final Collection<Object> values,
		final Class<?> type,
		final DocumentReplaceOptions options) throws DataAccessException {
		try {
			return collection(type).replaceDocuments(DBCollectionEntity.class.cast(toDBEntity(values)), options);
		} catch (final ArangoDBException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public MultiDocumentEntity<DocumentUpdateEntity<Object>> replaceDocuments(
		final Collection<Object> values,
		final Class<?> type) throws DataAccessException {
		return replaceDocuments(values, type, new DocumentReplaceOptions());
	}

	@Override
	public DocumentUpdateEntity<Object> replaceDocument(
		final String id,
		final Object value,
		final DocumentReplaceOptions options) throws DataAccessException {
		try {
			return collection(value.getClass(), id).replaceDocument(determineDocumentKeyFromId(id), toDBEntity(value),
				options);
		} catch (final ArangoDBException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public DocumentUpdateEntity<Object> replaceDocument(final String id, final Object value)
			throws DataAccessException {
		return replaceDocument(id, value, new DocumentReplaceOptions());
	}

	@Override
	public <T> T getDocument(final String id, final Class<T> type, final DocumentReadOptions options)
			throws DataAccessException {
		try {
			final DBEntity doc = collection(type, id).getDocument(determineDocumentKeyFromId(id), DBEntity.class,
				options);
			return fromDBEntity(type, doc);
		} catch (final ArangoDBException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> T getDocument(final String id, final Class<T> type) throws DataAccessException {
		return getDocument(id, type, new DocumentReadOptions());
	}

	@Override
	public MultiDocumentEntity<DocumentCreateEntity<Object>> insertDocuments(
		final Collection<Object> values,
		final Class<?> type,
		final DocumentCreateOptions options) throws DataAccessException {
		try {
			return collection(type).insertDocuments(DBCollectionEntity.class.cast(toDBEntity(values)), options);
		} catch (final ArangoDBException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public MultiDocumentEntity<DocumentCreateEntity<Object>> insertDocuments(
		final Collection<Object> values,
		final Class<?> type) throws DataAccessException {
		return insertDocuments(values, type, new DocumentCreateOptions());
	}

	@Override
	public DocumentCreateEntity<Object> insertDocument(final Object value, final DocumentCreateOptions options)
			throws DataAccessException {
		try {
			return collection(value.getClass()).insertDocument(toDBEntity(value));
		} catch (final ArangoDBException e) {
			throw exceptionTranslator.translateExceptionIfPossible(e);
		}
	}

	@Override
	public DocumentCreateEntity<Object> insertDocument(final Object value) throws DataAccessException {
		return insertDocument(value, new DocumentCreateOptions());
	}

	@Override
	public void dropCollection(final Class<?> type) {
		final ArangoCollection collection = collectionCache.remove(type);
		if (collection != null) {
			collection.drop();
		}
	}

	@Override
	public void dropDatabase() {
		db().drop();
		database = null;
		collectionCache.clear();
	}

}
