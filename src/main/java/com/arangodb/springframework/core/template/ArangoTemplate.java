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
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.support.PersistenceExceptionTranslator;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDBException;
import com.arangodb.entity.ArangoDBVersion;
import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.entity.DocumentDeleteEntity;
import com.arangodb.entity.DocumentUpdateEntity;
import com.arangodb.entity.MultiDocumentEntity;
import com.arangodb.model.AqlQueryOptions;
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
import com.arangodb.springframework.core.util.ArangoExceptionTranslator;

/**
 * @author Mark Vollmary
 *
 */
public class ArangoTemplate implements ArangoOperations {

	private final PersistenceExceptionTranslator exceptionTranslator;
	private final ArangoConverter converter;
	private final ArangoDB arango;
	private final String database;

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
		this.database = database;
		this.converter = converter;
		this.exceptionTranslator = exceptionTranslator;
	}

	private DataAccessException translateExceptionIfPossible(final RuntimeException exception) {
		return exceptionTranslator.translateExceptionIfPossible(exception);
	}

	@Override
	public String determineCollectionName(final Class<?> entityClass, final String id) {
		final String[] split = id.split("/");
		return split.length == 2 ? split[0] : determineCollectionName(entityClass);
	}

	@Override
	public String determineCollectionName(final Class<?> entityClass) {
		return converter.getMappingContext().getPersistentEntity(entityClass)
				.orElseThrow(() -> new InvalidDataAccessApiUsageException(
						"No persistent entity information found for the type " + entityClass.getName()))
				.getCollection();
	}

	private String determineDocumentKey(final String id) {
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
		return arango.db(database).query(query, DBDocumentEntity.class.cast(toDBEntity(bindVars)), options, type);
	}

	@Override
	public <T> MultiDocumentEntity<DocumentDeleteEntity<T>> deleteDocuments(
		final Collection<?> values,
		final Class<T> type,
		final DocumentDeleteOptions options) throws DataAccessException {
		try {
			// TODO determineCollectionName
			return arango.db(database).collection(determineCollectionName(values.iterator().next().getClass()))
					.deleteDocuments(DBCollectionEntity.class.cast(toDBEntity(values)), type, options);
		} catch (final ArangoDBException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public MultiDocumentEntity<DocumentDeleteEntity<Void>> deleteDocuments(final Collection<?> values)
			throws DataAccessException {
		return deleteDocuments(values, Void.class, new DocumentDeleteOptions());
	}

	@Override
	public <T> DocumentDeleteEntity<Void> deleteDocument(final String id, final Class<T> type)
			throws DataAccessException {
		try {
			return arango.db(database).collection(determineCollectionName(type, id))
					.deleteDocument(determineDocumentKey(id));
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
			return arango.db(database).collection(determineCollectionName(type, id))
					.deleteDocument(determineDocumentKey(id), type, options);
		} catch (final ArangoDBException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public MultiDocumentEntity<DocumentUpdateEntity<Object>> updateDocuments(
		final Collection<Object> values,
		final DocumentUpdateOptions options) throws DataAccessException {
		try {
			// TODO determineCollectionName
			return arango.db(database).collection(determineCollectionName(values.iterator().next().getClass()))
					.updateDocuments(DBCollectionEntity.class.cast(toDBEntity(values)), options);
		} catch (final ArangoDBException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public MultiDocumentEntity<DocumentUpdateEntity<Object>> updateDocuments(final Collection<Object> values)
			throws DataAccessException {
		return updateDocuments(values, new DocumentUpdateOptions());
	}

	@Override
	public DocumentUpdateEntity<Object> updateDocument(
		final String id,
		final Object value,
		final DocumentUpdateOptions options) throws DataAccessException {
		try {
			return arango.db(database).collection(determineCollectionName(value.getClass(), id))
					.updateDocument(determineDocumentKey(id), toDBEntity(value));
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
		final DocumentReplaceOptions options) throws DataAccessException {
		try {
			// TODO determineCollectionName
			return arango.db(database).collection(determineCollectionName(values.iterator().next().getClass()))
					.replaceDocuments(DBCollectionEntity.class.cast(toDBEntity(values)), options);
		} catch (final ArangoDBException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public MultiDocumentEntity<DocumentUpdateEntity<Object>> replaceDocuments(final Collection<Object> values)
			throws DataAccessException {
		return replaceDocuments(values, new DocumentReplaceOptions());
	}

	@Override
	public DocumentUpdateEntity<Object> replaceDocument(
		final String id,
		final Object value,
		final DocumentReplaceOptions options) throws DataAccessException {
		try {
			return arango.db(database).collection(determineCollectionName(value.getClass(), id))
					.replaceDocument(determineDocumentKey(id), toDBEntity(value), options);
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
			final DBEntity doc = arango.db(database).collection(determineCollectionName(type))
					.getDocument(determineDocumentKey(id), DBEntity.class, options);
			return fromDBEntity(type, doc);
		} catch (final ArangoDBException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> T getDocument(final String id, final Class<T> type) throws DataAccessException {
		return getDocument(determineDocumentKey(id), type, new DocumentReadOptions());
	}

	@Override
	public MultiDocumentEntity<DocumentCreateEntity<Object>> insertDocuments(
		final Collection<Object> values,
		final DocumentCreateOptions options) throws DataAccessException {
		try {
			// final Class<?> type = ClassTypeInformation.from(values.getClass()).getComponentType().get().getType();
			// TODO find a better way t determine the component type
			return arango.db(database).collection(determineCollectionName(values.iterator().next().getClass()))
					.insertDocuments(DBCollectionEntity.class.cast(toDBEntity(values)), options);
		} catch (final ArangoDBException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public MultiDocumentEntity<DocumentCreateEntity<Object>> insertDocuments(final Collection<Object> values)
			throws DataAccessException {
		return insertDocuments(values, new DocumentCreateOptions());
	}

	@Override
	public DocumentCreateEntity<Object> insertDocument(final Object value, final DocumentCreateOptions options)
			throws DataAccessException {
		try {
			return arango.db(database).collection(determineCollectionName(value.getClass()))
					.insertDocument(toDBEntity(value));
		} catch (final ArangoDBException e) {
			throw exceptionTranslator.translateExceptionIfPossible(e);
		}
	}

	@Override
	public DocumentCreateEntity<Object> insertDocument(final Object value) throws DataAccessException {
		return insertDocument(value, new DocumentCreateOptions());
	}

}
