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

import static com.arangodb.springframework.core.util.MetadataUtils.determineDocumentKeyFromId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.domain.Persistable;
import org.springframework.data.mapping.PersistentPropertyAccessor;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.ArangoDBVersion;
import com.arangodb.entity.DocumentEntity;
import com.arangodb.entity.MultiDocumentEntity;
import com.arangodb.entity.UserEntity;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.model.CollectionCreateOptions;
import com.arangodb.model.DocumentCreateOptions;
import com.arangodb.model.DocumentDeleteOptions;
import com.arangodb.model.DocumentReadOptions;
import com.arangodb.model.DocumentReplaceOptions;
import com.arangodb.model.DocumentUpdateOptions;
import com.arangodb.model.FulltextIndexOptions;
import com.arangodb.model.GeoIndexOptions;
import com.arangodb.model.HashIndexOptions;
import com.arangodb.model.PersistentIndexOptions;
import com.arangodb.model.SkiplistIndexOptions;
import com.arangodb.springframework.annotation.FulltextIndex;
import com.arangodb.springframework.annotation.GeoIndex;
import com.arangodb.springframework.annotation.HashIndex;
import com.arangodb.springframework.annotation.PersistentIndex;
import com.arangodb.springframework.annotation.SkiplistIndex;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.CollectionOperations;
import com.arangodb.springframework.core.UserOperations;
import com.arangodb.springframework.core.convert.ArangoConverter;
import com.arangodb.springframework.core.convert.DBCollectionEntity;
import com.arangodb.springframework.core.convert.DBDocumentEntity;
import com.arangodb.springframework.core.convert.DBEntity;
import com.arangodb.springframework.core.mapping.ArangoPersistentEntity;
import com.arangodb.springframework.core.mapping.ArangoPersistentProperty;
import com.arangodb.springframework.core.template.DefaultUserOperation.CollectionCallback;
import com.arangodb.springframework.core.util.ArangoExceptionTranslator;
import com.arangodb.springframework.core.util.MetadataUtils;
import com.arangodb.util.MapBuilder;

/**
 * @author Mark Vollmary
 * @author Christian Lechner
 * @author Reşat SABIQ
 */
public class ArangoTemplate implements ArangoOperations, CollectionCallback {

	private volatile ArangoDBVersion version;
	private final PersistenceExceptionTranslator exceptionTranslator;
	private final ArangoConverter converter;
	private final ArangoDB arango;
	private volatile ArangoDatabase database;
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
		this.arango = arango.build()._setCursorInitializer(
			new com.arangodb.springframework.core.template.ArangoCursorInitializer(converter));
		this.databaseName = database;
		this.converter = converter;
		this.exceptionTranslator = exceptionTranslator;
		// set concurrency level to 1 as writes are very rare compared to reads
		collectionCache = new ConcurrentHashMap<>(8, 0.9f, 1);
		version = null;
	}

	private ArangoDatabase db() {
		// guard against NPE because database can be set to null by dropDatabase() by another thread
		ArangoDatabase db = database;
		if (db != null) {
			return db;
		}
		// make sure the database is only created once
		synchronized (this) {
			db = database;
			if (db != null) {
				return db;
			}
			db = arango.db(databaseName);
			try {
				db.getInfo();
			} catch (final ArangoDBException e) {
				if (new Integer(404).equals(e.getResponseCode())) {
					try {
						arango.createDatabase(databaseName);
					} catch (final ArangoDBException e1) {
						throw translateExceptionIfPossible(e1);
					}
				} else {
					throw translateExceptionIfPossible(e);
				}
			}
			database = db;
			return db;
		}
	}

	private DataAccessException translateExceptionIfPossible(final RuntimeException exception) {
		return exceptionTranslator.translateExceptionIfPossible(exception);
	}

	private ArangoCollection _collection(final String name) {
		return _collection(name, null, null);
	}

	private ArangoCollection _collection(final Class<?> entityClass) {
		return _collection(entityClass, null);
	}

	private ArangoCollection _collection(final Class<?> entityClass, final String id) {
		final ArangoPersistentEntity<?> persistentEntity = converter.getMappingContext()
				.getRequiredPersistentEntity(entityClass);
		final String name = determineCollectionFromId(Optional.ofNullable(id)).orElse(persistentEntity.getCollection());
		return _collection(name, persistentEntity, persistentEntity.getCollectionOptions());
	}

	private ArangoCollection _collection(
		final String name,
		final ArangoPersistentEntity<?> persistentEntity,
		final CollectionCreateOptions options) {

		return collectionCache.computeIfAbsent(name, collName -> {
			final ArangoCollection collection = db().collection(collName);
			try {
				collection.getInfo();
			} catch (final ArangoDBException e) {
				if (new Integer(404).equals(e.getResponseCode())) {
					try {
						db().createCollection(collName, options);
					} catch (final ArangoDBException e1) {
						throw translateExceptionIfPossible(e1);
					}
				} else {
					throw translateExceptionIfPossible(e);
				}
			}
			if (persistentEntity != null) {
				ensureCollectionIndexes(collection(collection), persistentEntity);
			}
			return collection;
		});
	}

	private static void ensureCollectionIndexes(
		final CollectionOperations collection,
		final ArangoPersistentEntity<?> persistentEntity) {
		persistentEntity.getHashIndexes().stream().forEach(index -> ensureHashIndex(collection, index));
		persistentEntity.getHashIndexedProperties().stream().forEach(p -> ensureHashIndex(collection, p));
		persistentEntity.getSkiplistIndexes().stream().forEach(index -> ensureSkiplistIndex(collection, index));
		persistentEntity.getSkiplistIndexedProperties().stream().forEach(p -> ensureSkiplistIndex(collection, p));
		persistentEntity.getPersistentIndexes().stream().forEach(index -> ensurePersistentIndex(collection, index));
		persistentEntity.getPersistentIndexedProperties().stream().forEach(p -> ensurePersistentIndex(collection, p));
		persistentEntity.getGeoIndexes().stream().forEach(index -> ensureGeoIndex(collection, index));
		persistentEntity.getGeoIndexedProperties().stream().forEach(p -> ensureGeoIndex(collection, p));
		persistentEntity.getFulltextIndexes().stream().forEach(index -> ensureFulltextIndex(collection, index));
		persistentEntity.getFulltextIndexedProperties().stream().forEach(p -> ensureFulltextIndex(collection, p));
	}

	private static void ensureHashIndex(final CollectionOperations collection, final HashIndex annotation) {
		collection.ensureHashIndex(Arrays.asList(annotation.fields()), new HashIndexOptions()
				.unique(annotation.unique()).sparse(annotation.sparse()).deduplicate(annotation.deduplicate()));
	}

	private static void ensureHashIndex(final CollectionOperations collection, final ArangoPersistentProperty value) {
		final HashIndexOptions options = new HashIndexOptions();
		value.getHashIndexed()
				.ifPresent(i -> options.unique(i.unique()).sparse(i.sparse()).deduplicate(i.deduplicate()));
		collection.ensureHashIndex(Collections.singleton(value.getFieldName()), options);
	}

	private static void ensureSkiplistIndex(final CollectionOperations collection, final SkiplistIndex annotation) {
		collection.ensureSkiplistIndex(Arrays.asList(annotation.fields()), new SkiplistIndexOptions()
				.unique(annotation.unique()).sparse(annotation.sparse()).deduplicate(annotation.deduplicate()));
	}

	private static void ensureSkiplistIndex(
		final CollectionOperations collection,
		final ArangoPersistentProperty value) {
		final SkiplistIndexOptions options = new SkiplistIndexOptions();
		value.getSkiplistIndexed()
				.ifPresent(i -> options.unique(i.unique()).sparse(i.sparse()).deduplicate(i.deduplicate()));
		collection.ensureSkiplistIndex(Collections.singleton(value.getFieldName()), options);
	}

	private static void ensurePersistentIndex(final CollectionOperations collection, final PersistentIndex annotation) {
		collection.ensurePersistentIndex(Arrays.asList(annotation.fields()),
			new PersistentIndexOptions().unique(annotation.unique()).sparse(annotation.sparse()));
	}

	private static void ensurePersistentIndex(
		final CollectionOperations collection,
		final ArangoPersistentProperty value) {
		final PersistentIndexOptions options = new PersistentIndexOptions();
		value.getPersistentIndexed().ifPresent(i -> options.unique(i.unique()).sparse(i.sparse()));
		collection.ensurePersistentIndex(Collections.singleton(value.getFieldName()), options);
	}

	private static void ensureGeoIndex(final CollectionOperations collection, final GeoIndex annotation) {
		collection.ensureGeoIndex(Arrays.asList(annotation.fields()),
			new GeoIndexOptions().geoJson(annotation.geoJson()));
	}

	private static void ensureGeoIndex(final CollectionOperations collection, final ArangoPersistentProperty value) {
		final GeoIndexOptions options = new GeoIndexOptions();
		value.getGeoIndexed().ifPresent(i -> options.geoJson(i.geoJson()));
		collection.ensureGeoIndex(Collections.singleton(value.getFieldName()), options);
	}

	private static void ensureFulltextIndex(final CollectionOperations collection, final FulltextIndex annotation) {
		collection.ensureFulltextIndex(Collections.singleton(annotation.field()),
			new FulltextIndexOptions().minLength(annotation.minLength() > -1 ? annotation.minLength() : null));
	}

	private static void ensureFulltextIndex(
		final CollectionOperations collection,
		final ArangoPersistentProperty value) {
		final FulltextIndexOptions options = new FulltextIndexOptions();
		value.getFulltextIndexed().ifPresent(i -> options.minLength(i.minLength() > -1 ? i.minLength() : null));
		collection.ensureFulltextIndex(Collections.singleton(value.getFieldName()), options);
	}

	private Optional<String> determineCollectionFromId(final Optional<String> id) {
		return id.map(i -> MetadataUtils.determineCollectionFromId(i));
	}

	private DBEntity toDBEntity(final Object value) {
		final DBEntity entity = converter.isCollectionType(value.getClass()) ? new DBCollectionEntity()
				: new DBDocumentEntity();
		converter.write(value, entity);
		return entity;
	}

	private <T> T fromDBEntity(final Class<T> entityClass, final DBEntity doc) {
		return converter.read(entityClass, doc);
	}

	@Override
	public ArangoDB driver() {
		return arango;
	}

	@Override
	public ArangoDBVersion getVersion() throws DataAccessException {
		try {
			if (version == null) {
				version = db().getVersion();
			}
			return version;
		} catch (final ArangoDBException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> ArangoCursor<T> query(final String query, final AqlQueryOptions options, final Class<T> entityClass)
			throws DataAccessException {
		return db().query(query, null, options, entityClass);
	}

	@Override
	public <T> ArangoCursor<T> query(
		final String query,
		final Map<String, Object> bindVars,
		final AqlQueryOptions options,
		final Class<T> entityClass) throws DataAccessException {
		return db().query(query,
			bindVars == null ? null : DBDocumentEntity.class.cast(toDBEntity(prepareBindVars(bindVars))), options,
			entityClass);
	}

	private Map<String, Object> prepareBindVars(final Map<String, Object> bindVars) {
		for (final Map.Entry<String, Object> entry : new HashMap<>(bindVars).entrySet()) {
			if (entry.getKey().startsWith("@") && entry.getValue() instanceof Class) {
				bindVars.put(entry.getKey(), _collection((Class<?>) entry.getValue()).name());
			}
		}
		return bindVars;
	}

	@Override
	public MultiDocumentEntity<? extends DocumentEntity> delete(
		final Iterable<Object> values,
		final Class<?> entityClass,
		final DocumentDeleteOptions options) throws DataAccessException {
		try {
			return _collection(entityClass).deleteDocuments(DBCollectionEntity.class.cast(toDBEntity(values)),
				entityClass, options);
		} catch (final ArangoDBException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public MultiDocumentEntity<? extends DocumentEntity> delete(
		final Iterable<Object> values,
		final Class<?> entityClass) throws DataAccessException {
		return delete(values, entityClass, new DocumentDeleteOptions());
	}

	@Override
	public DocumentEntity delete(final String id, final Class<?> entityClass, final DocumentDeleteOptions options)
			throws DataAccessException {
		try {
			return _collection(entityClass, id).deleteDocument(determineDocumentKeyFromId(id), entityClass, options);
		} catch (final ArangoDBException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public DocumentEntity delete(final String id, final Class<?> entityClass) throws DataAccessException {
		return delete(id, entityClass, new DocumentDeleteOptions());
	}

	@Override
	public <T> MultiDocumentEntity<? extends DocumentEntity> update(
		final Iterable<T> values,
		final Class<T> entityClass,
		final DocumentUpdateOptions options) throws DataAccessException {
		try {
			final MultiDocumentEntity<? extends DocumentEntity> res = _collection(entityClass)
					.updateDocuments(DBCollectionEntity.class.cast(toDBEntity(values)), options);
			updateDBFields(values, res);
			return res;
		} catch (final ArangoDBException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> MultiDocumentEntity<? extends DocumentEntity> update(
		final Iterable<T> values,
		final Class<T> entityClass) throws DataAccessException {
		return update(values, entityClass, new DocumentUpdateOptions());
	}

	@Override
	public DocumentEntity update(final String id, final Object value, final DocumentUpdateOptions options)
			throws DataAccessException {
		try {
			final DocumentEntity res = _collection(value.getClass(), id).updateDocument(determineDocumentKeyFromId(id),
				toDBEntity(value), options);
			updateDBFields(value, res);
			return res;
		} catch (final ArangoDBException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public DocumentEntity update(final String id, final Object value) throws DataAccessException {
		return update(id, value, new DocumentUpdateOptions());
	}

	@Override
	public <T> MultiDocumentEntity<? extends DocumentEntity> replace(
		final Iterable<T> values,
		final Class<T> entityClass,
		final DocumentReplaceOptions options) throws DataAccessException {
		try {
			final MultiDocumentEntity<? extends DocumentEntity> res = _collection(entityClass)
					.replaceDocuments(DBCollectionEntity.class.cast(toDBEntity(values)), options);
			updateDBFields(values, res);
			return res;
		} catch (final ArangoDBException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> MultiDocumentEntity<? extends DocumentEntity> replace(
		final Iterable<T> values,
		final Class<T> entityClass) throws DataAccessException {
		return replace(values, entityClass, new DocumentReplaceOptions());
	}

	@Override
	public DocumentEntity replace(final String id, final Object value, final DocumentReplaceOptions options)
			throws DataAccessException {
		try {
			final DocumentEntity res = _collection(value.getClass(), id).replaceDocument(determineDocumentKeyFromId(id),
				toDBEntity(value), options);
			updateDBFields(value, res);
			return res;
		} catch (final ArangoDBException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public DocumentEntity replace(final String id, final Object value) throws DataAccessException {
		return replace(id, value, new DocumentReplaceOptions());
	}

	@Override
	public <T> Optional<T> find(final String id, final Class<T> entityClass, final DocumentReadOptions options)
			throws DataAccessException {
		try {
			final DBEntity doc = _collection(entityClass, id).getDocument(determineDocumentKeyFromId(id),
				DBEntity.class, options);
			return Optional.ofNullable(fromDBEntity(entityClass, doc));
		} catch (final ArangoDBException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> Optional<T> find(final String id, final Class<T> entityClass) throws DataAccessException {
		return find(id, entityClass, new DocumentReadOptions());
	}

	@Override
	public <T> Iterable<T> findAll(final Class<T> entityClass) throws DataAccessException {
		final String query = "FOR entity IN @@col RETURN entity";
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return query(query, new MapBuilder().put("@col", entityClass).get(), null, entityClass);
			}
		};
	}

	@Override
	public <T> Iterable<T> find(final Iterable<String> ids, final Class<T> entityClass) throws DataAccessException {
		try {
			final Collection<String> keys = new ArrayList<>();
			ids.forEach(id -> keys.add(determineDocumentKeyFromId(id)));
			final MultiDocumentEntity<DBEntity> docs = _collection(entityClass).getDocuments(keys, DBEntity.class);
			return docs.getDocuments().stream().map(doc -> fromDBEntity(entityClass, doc)).collect(Collectors.toList());
		} catch (final ArangoDBException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> MultiDocumentEntity<? extends DocumentEntity> insert(
		final Iterable<T> values,
		final Class<T> entityClass,
		final DocumentCreateOptions options) throws DataAccessException {
		try {
			final MultiDocumentEntity<? extends DocumentEntity> res = _collection(entityClass)
					.insertDocuments(DBCollectionEntity.class.cast(toDBEntity(values)), options);
			updateDBFields(values, res);
			return res;
		} catch (final ArangoDBException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> MultiDocumentEntity<? extends DocumentEntity> insert(
		final Iterable<T> values,
		final Class<T> entityClass) throws DataAccessException {
		return insert(values, entityClass, new DocumentCreateOptions());
	}

	@Override
	public DocumentEntity insert(final Object value, final DocumentCreateOptions options) throws DataAccessException {
		try {
			final DocumentEntity res = _collection(value.getClass()).insertDocument(toDBEntity(value), options);
			updateDBFields(value, res);
			return res;
		} catch (final ArangoDBException e) {
			throw exceptionTranslator.translateExceptionIfPossible(e);
		}
	}

	@Override
	public DocumentEntity insert(final Object value) throws DataAccessException {
		return insert(value, new DocumentCreateOptions());
	}

	@Override
	public DocumentEntity insert(final String collectionName, final Object value, final DocumentCreateOptions options)
			throws DataAccessException {
		try {
			final DocumentEntity res = _collection(collectionName).insertDocument(toDBEntity(value), options);
			updateDBFields(value, res);
			return res;
		} catch (final ArangoDBException e) {
			throw exceptionTranslator.translateExceptionIfPossible(e);
		}
	}

	@Override
	public DocumentEntity insert(final String collectionName, final Object value) throws DataAccessException {
		return insert(collectionName, value, new DocumentCreateOptions());
	}

	@Override
	public <T> void upsert(final T value, final UpsertStrategy strategy) throws DataAccessException {
		final Class<? extends Object> entityClass = value.getClass();
		final ArangoPersistentEntity<?> entity = getConverter().getMappingContext().getPersistentEntity(entityClass);

		final PersistentPropertyAccessor accessor = entity.getPropertyAccessor(value);
		final Object id = entity.getKeyProperty().map(property -> accessor.getProperty(property)).orElseGet(() -> {
			return entity.getIdProperty() != null ? accessor.getProperty(entity.getIdProperty()) : null;
		});
		if (id != null && (!(value instanceof Persistable) || !Persistable.class.cast(value).isNew())) {
			switch (strategy) {
			case UPDATE:
				update(id.toString(), value);
				break;
			case REPLACE:
			default:
				replace(id.toString(), value);
				break;
			}
			return;
		}
		insert(value);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> void upsert(final Iterable<T> value, final UpsertStrategy strategy) throws DataAccessException {
		final Optional<T> first = StreamSupport.stream(value.spliterator(), false).findFirst();
		if (!first.isPresent()) {
			return;
		}
		final Class<T> entityClass = (Class<T>) first.get().getClass();
		final ArangoPersistentEntity<?> entity = getConverter().getMappingContext().getPersistentEntity(entityClass);
		final ArangoPersistentProperty idProperty = entity.getIdProperty();
		final Optional<ArangoPersistentProperty> keyProperty = entity.getKeyProperty();

		final Collection<T> withId = new ArrayList<>();
		final Collection<T> withoutId = new ArrayList<>();
		for (final T e : value) {
			if (keyProperty.isPresent() || idProperty != null) {
				final PersistentPropertyAccessor accessor = entity.getPropertyAccessor(e);
				final Object id = keyProperty.map(property -> accessor.getProperty(property)).orElseGet(() -> {
					return idProperty != null ? accessor.getProperty(entity.getIdProperty()) : null;
				});
				if (id != null && (!(e instanceof Persistable) || !Persistable.class.cast(e).isNew())) {
					withId.add(e);
					continue;
				}
			}
			withoutId.add(e);
		}
		if (!withoutId.isEmpty()) {
			insert(withoutId, entityClass);
		}
		if (!withId.isEmpty()) {
			switch (strategy) {
			case UPDATE:
				update(withId, entityClass);
				break;
			case REPLACE:
			default:
				replace(withId, entityClass);
				break;
			}
		}
	}

	@Override
	public <T> void repsert(final T value) throws DataAccessException {
		insert(value, new DocumentCreateOptions().overwrite(true));
	}

	@Override
	public <T> void repsert(final Iterable<T> value, final Class<T> entityClass) throws DataAccessException {
		insert(value, entityClass, new DocumentCreateOptions().overwrite(true));
	}

	private <T> void updateDBFields(final Iterable<T> values, final MultiDocumentEntity<? extends DocumentEntity> res) {
		final Iterator<T> valueIterator = values.iterator();
		if (res.getErrors().isEmpty()) {
			final Iterator<? extends DocumentEntity> documentIterator = res.getDocuments().iterator();
			for (; valueIterator.hasNext() && documentIterator.hasNext();) {
				updateDBFields(valueIterator.next(), documentIterator.next());
			}
		} else {
			final Iterator<Object> documentIterator = res.getDocumentsAndErrors().iterator();
			for (; valueIterator.hasNext() && documentIterator.hasNext();) {
				final Object nextDoc = documentIterator.next();
				final Object nextValue = valueIterator.next();
				if (nextDoc instanceof DocumentEntity) {
					updateDBFields(nextValue, (DocumentEntity) nextDoc);
				}
			}
		}
	}

	private void updateDBFields(final Object value, final DocumentEntity documentEntity) {
		final ArangoPersistentEntity<?> entity = converter.getMappingContext().getPersistentEntity(value.getClass());
		final PersistentPropertyAccessor accessor = entity.getPropertyAccessor(value);
		final ArangoPersistentProperty idProperty = entity.getIdProperty();
		if (idProperty != null) {
			accessor.setProperty(idProperty, documentEntity.getId());
		}
		entity.getKeyProperty().ifPresent(key -> accessor.setProperty(key, documentEntity.getKey()));
		entity.getRevProperty().ifPresent(rev -> accessor.setProperty(rev, documentEntity.getRev()));
	}

	@Override
	public boolean exists(final String id, final Class<?> entityClass) throws DataAccessException {
		try {
			return _collection(entityClass).documentExists(determineDocumentKeyFromId(id));
		} catch (final ArangoDBException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public void dropDatabase() throws DataAccessException {
		// guard against NPE because another thread could also call dropDatabase()
		ArangoDatabase db = database;
		if (db == null) {
			db = arango.db(databaseName);
		}
		try {
			db.drop();
		} catch (final ArangoDBException e) {
			throw translateExceptionIfPossible(e);
		}
		database = null;
		collectionCache.clear();
	}

	@Override
	public CollectionOperations collection(final Class<?> entityClass) throws DataAccessException {
		return collection(_collection(entityClass));
	}

	@Override
	public CollectionOperations collection(final String name) throws DataAccessException {
		return collection(_collection(name));
	}

	@Override
	public CollectionOperations collection(final String name, final CollectionCreateOptions options)
			throws DataAccessException {
		return collection(_collection(name, null, options));
	}

	private CollectionOperations collection(final ArangoCollection collection) {
		return new DefaultCollectionOperations(collection, collectionCache, exceptionTranslator);
	}

	@Override
	public UserOperations user(final String username) {
		return new DefaultUserOperation(db(), username, exceptionTranslator, this);
	}

	@Override
	public Iterable<UserEntity> getUsers() throws DataAccessException {
		try {
			return arango.getUsers();
		} catch (final ArangoDBException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public ArangoConverter getConverter() {
		return this.converter;
	}

}
