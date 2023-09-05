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

import com.arangodb.*;
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
import com.arangodb.model.PersistentIndexOptions;
import com.arangodb.model.TtlIndexOptions;
import com.arangodb.springframework.annotation.FulltextIndex;
import com.arangodb.springframework.annotation.GeoIndex;
import com.arangodb.springframework.annotation.PersistentIndex;
import com.arangodb.springframework.annotation.TtlIndex;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.CollectionOperations;
import com.arangodb.springframework.core.UserOperations;
import com.arangodb.springframework.core.convert.ArangoConverter;
import com.arangodb.springframework.core.convert.resolver.ResolverFactory;
import com.arangodb.springframework.core.mapping.ArangoPersistentEntity;
import com.arangodb.springframework.core.mapping.ArangoPersistentProperty;
import com.arangodb.springframework.core.mapping.event.AfterDeleteEvent;
import com.arangodb.springframework.core.mapping.event.AfterLoadEvent;
import com.arangodb.springframework.core.mapping.event.AfterSaveEvent;
import com.arangodb.springframework.core.mapping.event.ArangoMappingEvent;
import com.arangodb.springframework.core.mapping.event.BeforeDeleteEvent;
import com.arangodb.springframework.core.mapping.event.BeforeSaveEvent;
import com.arangodb.springframework.core.template.DefaultUserOperation.CollectionCallback;
import com.arangodb.springframework.core.util.ArangoExceptionTranslator;
import com.arangodb.springframework.core.util.MetadataUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.expression.BeanFactoryAccessor;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author Mark Vollmary
 * @author Christian Lechner
 * @author Reşat SABIQ
 */
public class ArangoTemplate implements ArangoOperations, CollectionCallback, ApplicationContextAware {

	private static final String REPSERT_QUERY_BODY =
			"UPSERT { _key: doc._key } " +
					"INSERT doc._key == null ? UNSET(doc, \"_key\") : doc " +
					"REPLACE doc " +
					"IN @@col " +
					"OPTIONS { ignoreRevs: false } " +
					"RETURN NEW";

	private static final String REPSERT_QUERY = "LET doc = @doc " + REPSERT_QUERY_BODY;
	private static final String REPSERT_MANY_QUERY = "FOR doc IN @docs " + REPSERT_QUERY_BODY;

	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

	private volatile ArangoDBVersion version;
	private final PersistenceExceptionTranslator exceptionTranslator;
	private final ArangoConverter converter;
	private final ResolverFactory resolverFactory;
	private final ArangoDB arango;
	private final String databaseName;
	private final Expression databaseExpression;
	private final Map<String, ArangoDatabase> databaseCache;
	private final Map<CollectionCacheKey, CollectionCacheValue> collectionCache;

	private final StandardEvaluationContext context;

	private ApplicationEventPublisher eventPublisher;

	public ArangoTemplate(final ArangoDB arango, final String database, final ArangoConverter converter,
			final ResolverFactory resolverFactory) {
		this(arango, database, converter, resolverFactory, new ArangoExceptionTranslator());
	}

	public ArangoTemplate(final ArangoDB arango, final String database, final ArangoConverter converter,
			final ResolverFactory resolverFactory, final PersistenceExceptionTranslator exceptionTranslator) {
		super();
		this.arango = arango;
		this.databaseName = database;
		this.databaseExpression = PARSER.parseExpression(databaseName, ParserContext.TEMPLATE_EXPRESSION);
		this.converter = converter;
		this.resolverFactory = resolverFactory;
		this.exceptionTranslator = exceptionTranslator;
		this.context = new StandardEvaluationContext();
		// set concurrency level to 1 as writes are very rare compared to reads
		collectionCache = new ConcurrentHashMap<>(8, 0.9f, 1);
		databaseCache = new ConcurrentHashMap<>(8, 0.9f, 1);
		version = null;
	}

	private ArangoDatabase db() {
		final String key = databaseExpression != null ? databaseExpression.getValue(context, String.class)
				: databaseName;
		return databaseCache.computeIfAbsent(key, name -> {
			final ArangoDatabase db = arango.db(name);
			if (!db.exists()) {
				db.create();
			}
			return db;
		});
	}

	private ArangoCollection _collection(final String name) {
		return _collection(name, null, null);
	}

	private ArangoCollection _collection(final Class<?> entityClass) {
		return _collection(entityClass, null);
	}

	private ArangoCollection _collection(final Class<?> entityClass, final Object id) {
		final ArangoPersistentEntity<?> persistentEntity = converter.getMappingContext()
				.getRequiredPersistentEntity(entityClass);
		final String name = determineCollectionFromId(id).orElse(persistentEntity.getCollection());
		return _collection(name, persistentEntity, persistentEntity.getCollectionOptions());
	}

	private ArangoCollection _collection(final String name, final ArangoPersistentEntity<?> persistentEntity,
			final CollectionCreateOptions options) {

		final ArangoDatabase db = db();
		final Class<?> entityClass = persistentEntity != null ? persistentEntity.getType() : null;
		final CollectionCacheValue value = collectionCache.computeIfAbsent(new CollectionCacheKey(db.name(), name),
				key -> {
					final ArangoCollection collection = db.collection(name);
					if (!collection.exists()) {
						collection.create(options);
					}
					return new CollectionCacheValue(collection);
				});
		final Collection<Class<?>> entities = value.getEntities();
		final ArangoCollection collection = value.getCollection();
		if (persistentEntity != null && !entities.contains(entityClass)) {
			value.addEntityClass(entityClass);
			ensureCollectionIndexes(collection(collection), persistentEntity);
		}
		return collection;
	}

	private static void ensureCollectionIndexes(final CollectionOperations collection,
			final ArangoPersistentEntity<?> persistentEntity) {
		persistentEntity.getPersistentIndexes().forEach(index -> ensurePersistentIndex(collection, index));
		persistentEntity.getPersistentIndexedProperties().forEach(p -> ensurePersistentIndex(collection, p));
		persistentEntity.getGeoIndexes().forEach(index -> ensureGeoIndex(collection, index));
		persistentEntity.getGeoIndexedProperties().forEach(p -> ensureGeoIndex(collection, p));
		persistentEntity.getFulltextIndexes().forEach(index -> ensureFulltextIndex(collection, index));
		persistentEntity.getFulltextIndexedProperties().forEach(p -> ensureFulltextIndex(collection, p));
		persistentEntity.getTtlIndex().ifPresent(index -> ensureTtlIndex(collection, index));
		persistentEntity.getTtlIndexedProperty().ifPresent(p -> ensureTtlIndex(collection, p));
	}

	private static void ensurePersistentIndex(final CollectionOperations collection, final PersistentIndex annotation) {
		collection.ensurePersistentIndex(Arrays.asList(annotation.fields()),
				new PersistentIndexOptions()
						.unique(annotation.unique())
						.sparse(annotation.sparse())
						.deduplicate(annotation.deduplicate()));
	}

	private static void ensurePersistentIndex(final CollectionOperations collection,
			final ArangoPersistentProperty value) {
		final PersistentIndexOptions options = new PersistentIndexOptions();
		value.getPersistentIndexed().ifPresent(i -> options
				.unique(i.unique())
				.sparse(i.sparse())
				.deduplicate(i.deduplicate()));
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

	@SuppressWarnings("deprecation")
	private static void ensureFulltextIndex(final CollectionOperations collection, final FulltextIndex annotation) {
		collection.ensureFulltextIndex(Collections.singleton(annotation.field()),
				new FulltextIndexOptions().minLength(annotation.minLength() > -1 ? annotation.minLength() : null));
	}

	@SuppressWarnings("deprecation")
	private static void ensureFulltextIndex(final CollectionOperations collection,
											final ArangoPersistentProperty value) {
		final FulltextIndexOptions options = new FulltextIndexOptions();
		value.getFulltextIndexed().ifPresent(i -> options.minLength(i.minLength() > -1 ? i.minLength() : null));
		collection.ensureFulltextIndex(Collections.singleton(value.getFieldName()), options);
	}

	private static void ensureTtlIndex(final CollectionOperations collection, final TtlIndex annotation) {
		collection.ensureTtlIndex(Collections.singleton(annotation.field()),
				new TtlIndexOptions().expireAfter(annotation.expireAfter()));
	}

	private static void ensureTtlIndex(final CollectionOperations collection, final ArangoPersistentProperty value) {
		final TtlIndexOptions options = new TtlIndexOptions();
		value.getTtlIndexed().ifPresent(i -> options.expireAfter(i.expireAfter()));
		collection.ensureTtlIndex(Collections.singleton(value.getFieldName()), options);
	}

	private Optional<String> determineCollectionFromId(final Object id) {
		return id != null ? Optional.ofNullable(MetadataUtils.determineCollectionFromId(converter.convertId(id)))
				: Optional.empty();
	}

	private String determineDocumentKeyFromId(final Object id) {
		return MetadataUtils.determineDocumentKeyFromId(converter.convertId(id));
	}

	private JsonNode toJsonNode(final Object source) {
		return converter.write(source);
	}

	private Collection<JsonNode> toJsonNodeCollection(final Iterable<?> values) {
		final Collection<JsonNode> nodes = new ArrayList<>();
		for (final Object value : values) {
			nodes.add(toJsonNode(value));
		}
		return nodes;
	}

	private <T> T fromJsonNode(final Class<T> entityClass, final JsonNode source) {
		final T result = converter.read(entityClass, source);
		if (result != null) {
			potentiallyEmitEvent(new AfterLoadEvent<>(result));
		}
		return result;
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
			throw DataAccessUtils.translateIfNecessary(e, exceptionTranslator);
		}
	}

	@Override
	public <T> ArangoCursor<T> query(final String query, final Class<T> entityClass) throws DataAccessException {
		return query(query, null, null, entityClass);
	}

	@Override
	public <T> ArangoCursor<T> query(final String query, final Map<String, Object> bindVars, final Class<T> entityClass)
			throws DataAccessException {
		return query(query, bindVars, null, entityClass);
	}

	@Override
	public <T> ArangoCursor<T> query(final String query, final AqlQueryOptions options, final Class<T> entityClass)
			throws DataAccessException {
		return query(query, null, options, entityClass);
	}

	@Override
	public <T> ArangoCursor<T> query(final String query, final Map<String, Object> bindVars,
									 final AqlQueryOptions options, final Class<T> entityClass) throws DataAccessException {
		try {
			ArangoCursor<JsonNode> cursor = db().query(query, JsonNode.class, bindVars == null ? null : prepareBindVars(bindVars), options);
			return new ArangoExtCursor<>(cursor, entityClass, converter, eventPublisher);
		} catch (final ArangoDBException e) {
			throw DataAccessUtils.translateIfNecessary(e, exceptionTranslator);
		}
	}

	private Map<String, Object> prepareBindVars(final Map<String, Object> bindVars) {
		final Map<String, Object> prepared = new HashMap<>(bindVars.size());
		for (final Entry<String, Object> entry : bindVars.entrySet()) {
			if (entry.getKey().startsWith("@") && entry.getValue() instanceof Class<?> clazz) {
				prepared.put(entry.getKey(), _collection(clazz).name());
			} else {
				prepared.put(entry.getKey(), toJsonNode(entry.getValue()));
			}
		}
		return prepared;
	}

	@Override
	public MultiDocumentEntity<? extends DocumentEntity> delete(final Iterable<Object> values,
			final Class<?> entityClass, final DocumentDeleteOptions options) throws DataAccessException {

		potentiallyEmitBeforeDeleteEvent(values, entityClass);

		MultiDocumentEntity<? extends DocumentEntity> result;
		try {
			result = _collection(entityClass).deleteDocuments(toJsonNodeCollection(values), options, entityClass);
		} catch (final ArangoDBException e) {
			throw DataAccessUtils.translateIfNecessary(e, exceptionTranslator);
		}

		potentiallyEmitAfterDeleteEvent(values, entityClass, result);
		return result;
	}

	@Override
	public MultiDocumentEntity<? extends DocumentEntity> delete(final Iterable<Object> values,
			final Class<?> entityClass) throws DataAccessException {
		return delete(values, entityClass, new DocumentDeleteOptions());
	}

	@Override
	public DocumentEntity delete(final Object id, final Class<?> entityClass, final DocumentDeleteOptions options)
			throws DataAccessException {

		potentiallyEmitEvent(new BeforeDeleteEvent<>(id, entityClass));

		final DocumentEntity result;
		try {
			result = _collection(entityClass, id).deleteDocument(determineDocumentKeyFromId(id), options, entityClass);
		} catch (final ArangoDBException e) {
			throw DataAccessUtils.translateIfNecessary(e, exceptionTranslator);
		}

		potentiallyEmitEvent(new AfterDeleteEvent<>(id, entityClass));
		return result;
	}

	@Override
	public DocumentEntity delete(final Object id, final Class<?> entityClass) throws DataAccessException {
		return delete(id, entityClass, new DocumentDeleteOptions());
	}

	@Override
	public <T> MultiDocumentEntity<? extends DocumentEntity> update(final Iterable<T> values,
			final Class<T> entityClass, final DocumentUpdateOptions options) throws DataAccessException {

		potentiallyEmitBeforeSaveEvent(values);

		final MultiDocumentEntity<? extends DocumentEntity> result;
		try {
			result = _collection(entityClass).updateDocuments(toJsonNodeCollection(values), options);
		} catch (final ArangoDBException e) {
			throw DataAccessUtils.translateIfNecessary(e, exceptionTranslator);
		}

		updateDBFields(values, result);
		potentiallyEmitAfterSaveEvent(values, result);
		return result;
	}

	@Override
	public <T> MultiDocumentEntity<? extends DocumentEntity> update(final Iterable<T> values,
			final Class<T> entityClass) throws DataAccessException {
		return update(values, entityClass, new DocumentUpdateOptions());
	}

	@Override
	public DocumentEntity update(final Object id, final Object value, final DocumentUpdateOptions options)
			throws DataAccessException {

		potentiallyEmitEvent(new BeforeSaveEvent<>(value));

		final DocumentEntity result;
		try {
			result = _collection(value.getClass(), id).updateDocument(determineDocumentKeyFromId(id), toJsonNode(value),
					options);
		} catch (final ArangoDBException e) {
			throw DataAccessUtils.translateIfNecessary(e, exceptionTranslator);
		}

		updateDBFields(value, result);
		potentiallyEmitEvent(new AfterSaveEvent<>(value));
		return result;
	}

	@Override
	public DocumentEntity update(final Object id, final Object value) throws DataAccessException {
		return update(id, value, new DocumentUpdateOptions());
	}

	@Override
	public <T> MultiDocumentEntity<? extends DocumentEntity> replace(final Iterable<T> values,
			final Class<T> entityClass, final DocumentReplaceOptions options) throws DataAccessException {

		potentiallyEmitBeforeSaveEvent(values);

		final MultiDocumentEntity<? extends DocumentEntity> result;
		try {
			result = _collection(entityClass).replaceDocuments(toJsonNodeCollection(values), options);
		} catch (final ArangoDBException e) {
			throw DataAccessUtils.translateIfNecessary(e, exceptionTranslator);
		}

		updateDBFields(values, result);
		potentiallyEmitAfterSaveEvent(values, result);
		return result;
	}

	@Override
	public <T> MultiDocumentEntity<? extends DocumentEntity> replace(final Iterable<T> values,
			final Class<T> entityClass) throws DataAccessException {
		return replace(values, entityClass, new DocumentReplaceOptions());
	}

	@Override
	public DocumentEntity replace(final Object id, final Object value, final DocumentReplaceOptions options)
			throws DataAccessException {
		potentiallyEmitEvent(new BeforeSaveEvent<>(value));

		final DocumentEntity result;
		try {
			result = _collection(value.getClass(), id).replaceDocument(determineDocumentKeyFromId(id), toJsonNode(value),
					options);
		} catch (final ArangoDBException e) {
			throw DataAccessUtils.translateIfNecessary(e, exceptionTranslator);
		}

		updateDBFields(value, result);
		potentiallyEmitEvent(new AfterSaveEvent<>(value));
		return result;
	}

	@Override
	public DocumentEntity replace(final Object id, final Object value) throws DataAccessException {
		return replace(id, value, new DocumentReplaceOptions());
	}

	@Override
	public <T> Optional<T> find(final Object id, final Class<T> entityClass, final DocumentReadOptions options)
			throws DataAccessException {
		try {
			final JsonNode doc = _collection(entityClass, id).getDocument(determineDocumentKeyFromId(id),
					JsonNode.class, options);
			return Optional.ofNullable(fromJsonNode(entityClass, doc));
		} catch (final ArangoDBException e) {
			throw DataAccessUtils.translateIfNecessary(e, exceptionTranslator);
		}
	}

	@Override
	public <T> Optional<T> find(final Object id, final Class<T> entityClass) throws DataAccessException {
		return find(id, entityClass, new DocumentReadOptions());
	}

	@Override
	public <T> Iterable<T> findAll(final Class<T> entityClass) throws DataAccessException {
		final String query = "FOR entity IN @@col RETURN entity";
		final Map<String, Object> bindVars = Collections.singletonMap("@col", entityClass);
		return query(query, bindVars, null, entityClass).asListRemaining();
	}

	@Override
	public <T> Iterable<T> find(final Iterable<?> ids, final Class<T> entityClass)
			throws DataAccessException {
		try {
			final Collection<String> keys = new ArrayList<>();
			ids.forEach(id -> keys.add(determineDocumentKeyFromId(id)));
			final MultiDocumentEntity<JsonNode> docs = _collection(entityClass).getDocuments(keys, JsonNode.class);
			return docs.getDocuments().stream().map(doc -> fromJsonNode(entityClass, doc)).collect(Collectors.toList());
		} catch (final ArangoDBException e) {
			throw DataAccessUtils.translateIfNecessary(e, exceptionTranslator);
		}
	}

	@Override
	public <T> MultiDocumentEntity<? extends DocumentEntity> insert(final Iterable<T> values,
			final Class<T> entityClass, final DocumentCreateOptions options) throws DataAccessException {

		potentiallyEmitBeforeSaveEvent(values);

		final MultiDocumentEntity<? extends DocumentEntity> result;
		try {
			result = _collection(entityClass).insertDocuments(toJsonNodeCollection(values), options);
		} catch (final ArangoDBException e) {
			throw DataAccessUtils.translateIfNecessary(e, exceptionTranslator);
		}

		updateDBFields(values, result);
		potentiallyEmitAfterSaveEvent(values, result);
		return result;
	}

	@Override
	public <T> MultiDocumentEntity<? extends DocumentEntity> insert(final Iterable<T> values,
			final Class<T> entityClass) throws DataAccessException {
		return insert(values, entityClass, new DocumentCreateOptions());
	}

	@Override
	public DocumentEntity insert(final Object value, final DocumentCreateOptions options) throws DataAccessException {
		potentiallyEmitEvent(new BeforeSaveEvent<>(value));

		final DocumentEntity result;
		try {
			result = _collection(value.getClass()).insertDocument(toJsonNode(value), options);
		} catch (final ArangoDBException e) {
			throw DataAccessUtils.translateIfNecessary(e, exceptionTranslator);
		}

		updateDBFields(value, result);
		potentiallyEmitEvent(new AfterSaveEvent<>(value));
		return result;
	}

	@Override
	public DocumentEntity insert(final Object value) throws DataAccessException {
		return insert(value, new DocumentCreateOptions());
	}

	@Override
	public DocumentEntity insert(final String collectionName, final Object value, final DocumentCreateOptions options)
			throws DataAccessException {
		potentiallyEmitEvent(new BeforeSaveEvent<>(value));

		final DocumentEntity result;
		try {
			result = _collection(collectionName).insertDocument(toJsonNode(value), options);
		} catch (final ArangoDBException e) {
			throw DataAccessUtils.translateIfNecessary(e, exceptionTranslator);
		}

		updateDBFields(value, result);
		potentiallyEmitEvent(new AfterSaveEvent<>(value));
		return result;
	}

	@Override
	public DocumentEntity insert(final String collectionName, final Object value) throws DataAccessException {
		return insert(collectionName, value, new DocumentCreateOptions());
	}

	@Override
	public <T> void repsert(final T value) throws DataAccessException {
		@SuppressWarnings("unchecked") final Class<T> clazz = (Class<T>) value.getClass();
		final String collectionName = _collection(clazz).name();

		potentiallyEmitEvent(new BeforeSaveEvent<>(value));

		Map<String, Object> bindVars = new HashMap<>();
		bindVars.put("@col", collectionName);
		bindVars.put("doc", value);

		final T result;
		try {
			ArangoCursor<T> it = query(
					REPSERT_QUERY,
					bindVars,
					clazz
			);
			result = it.hasNext() ? it.next() : null;
		} catch (final ArangoDBException e) {
			throw DataAccessUtils.translateIfNecessary(e, exceptionTranslator);
		}

		updateDBFieldsFromObject(value, result);
		potentiallyEmitEvent(new AfterSaveEvent<>(result));
	}

	@Override
	public <T> void repsert(final Iterable<? extends T> values, final Class<T> entityClass) throws DataAccessException {
		if (!values.iterator().hasNext()) {
			return;
		}

		final String collectionName = _collection(entityClass).name();
		potentiallyEmitBeforeSaveEvent(values);

		Map<String, Object> bindVars = new HashMap<>();
		bindVars.put("@col", collectionName);
		bindVars.put("docs", values);

		final Iterable<? extends T> result;
		try {
			result = query(
					REPSERT_MANY_QUERY,
					bindVars,
					entityClass
			).asListRemaining();
		} catch (final ArangoDBException e) {
			throw DataAccessUtils.translateIfNecessary(e, exceptionTranslator);
		}

		updateDBFieldsFromObjects(values, result);
		result.forEach(it -> potentiallyEmitEvent(new AfterSaveEvent<>(it)));
	}

	private void updateDBFieldsFromObjects(final Iterable<?> values, final Iterable<?> res) {
		final Iterator<?> valueIterator = values.iterator();
		final Iterator<?> resIterator = res.iterator();
		while (valueIterator.hasNext() && resIterator.hasNext()) {
			updateDBFieldsFromObject(valueIterator.next(), resIterator.next());
		}
	}

	private void updateDBFieldsFromObject(final Object toModify, final Object toRead) {
		final ArangoPersistentEntity<?> entityToRead = converter.getMappingContext().getRequiredPersistentEntity(toRead.getClass());
		final PersistentPropertyAccessor<?> accessorToRead = entityToRead.getPropertyAccessor(toRead);
		final ArangoPersistentProperty idPropertyToRead = entityToRead.getIdProperty();
		final Optional<ArangoPersistentProperty> arangoIdPropertyToReadOptional = entityToRead.getArangoIdProperty();
		final Optional<ArangoPersistentProperty> revPropertyToReadOptional = entityToRead.getRevProperty();

		final ArangoPersistentEntity<?> entityToModify = converter.getMappingContext().getRequiredPersistentEntity(toModify.getClass());
		final PersistentPropertyAccessor<?> accessorToWrite = entityToModify.getPropertyAccessor(toModify);
		final ArangoPersistentProperty idPropertyToWrite = entityToModify.getIdProperty();

		if (idPropertyToWrite != null && !idPropertyToWrite.isImmutable()) {
			accessorToWrite.setProperty(idPropertyToWrite, accessorToRead.getProperty(idPropertyToRead));
		}

		if (arangoIdPropertyToReadOptional.isPresent()) {
			ArangoPersistentProperty arangoIdPropertyToRead = arangoIdPropertyToReadOptional.get();
			entityToModify.getArangoIdProperty().filter(arangoId -> !arangoId.isImmutable())
					.ifPresent(arangoId -> accessorToWrite.setProperty(arangoId, accessorToRead.getProperty(arangoIdPropertyToRead)));
		}

		if (revPropertyToReadOptional.isPresent()) {
			ArangoPersistentProperty revPropertyToRead = revPropertyToReadOptional.get();
			entityToModify.getRevProperty().filter(rev -> !rev.isImmutable())
					.ifPresent(rev -> accessorToWrite.setProperty(rev, accessorToRead.getProperty(revPropertyToRead)));
		}
	}

	private <T> void updateDBFields(final Iterable<T> values, final MultiDocumentEntity<? extends DocumentEntity> res) {
		final Iterator<T> valueIterator = values.iterator();
		if (res.getErrors().isEmpty()) {
			final Iterator<? extends DocumentEntity> documentIterator = res.getDocuments().iterator();
			while (valueIterator.hasNext() && documentIterator.hasNext()) {
				updateDBFields(valueIterator.next(), documentIterator.next());
			}
		} else {
			final Iterator<Object> documentIterator = res.getDocumentsAndErrors().iterator();
			while (valueIterator.hasNext() && documentIterator.hasNext()) {
				final Object nextDoc = documentIterator.next();
				final Object nextValue = valueIterator.next();
				if (nextDoc instanceof DocumentEntity doc) {
					updateDBFields(nextValue, doc);
				}
			}
		}
	}

	private void updateDBFields(final Object value, final DocumentEntity documentEntity) {
		final ArangoPersistentEntity<?> entity = converter.getMappingContext().getRequiredPersistentEntity(value.getClass());
		final PersistentPropertyAccessor<?> accessor = entity.getPropertyAccessor(value);
		final ArangoPersistentProperty idProperty = entity.getIdProperty();
		if (idProperty != null && !idProperty.isImmutable()) {
			accessor.setProperty(idProperty, documentEntity.getKey());
		}
		entity.getArangoIdProperty().filter(arangoId -> !arangoId.isImmutable())
				.ifPresent(arangoId -> accessor.setProperty(arangoId, documentEntity.getId()));
		entity.getRevProperty().filter(rev -> !rev.isImmutable())
				.ifPresent(rev -> accessor.setProperty(rev, documentEntity.getRev()));
	}

	@Override
	public boolean exists(final Object id, final Class<?> entityClass) throws DataAccessException {
		try {
			return _collection(entityClass).documentExists(determineDocumentKeyFromId(id));
		} catch (final ArangoDBException e) {
			throw DataAccessUtils.translateIfNecessary(e, exceptionTranslator);
		}
	}

	@Override
	public void dropDatabase() throws DataAccessException {
		final ArangoDatabase db = db();
		try {
			db.drop();
		} catch (final ArangoDBException e) {
			throw DataAccessUtils.translateIfNecessary(e, exceptionTranslator);
		}
		databaseCache.remove(db.name());
		collectionCache.keySet().stream().filter(key -> key.getDb().equals(db.name()))
				.forEach(collectionCache::remove);
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
			throw DataAccessUtils.translateIfNecessary(e, exceptionTranslator);
		}
	}

	@Override
	public ArangoConverter getConverter() {
		return this.converter;
	}

	@Override
	public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
		context.setRootObject(applicationContext);
		context.setBeanResolver(new BeanFactoryResolver(applicationContext));
		context.addPropertyAccessor(new BeanFactoryAccessor());
		eventPublisher = applicationContext;
	}

	private void potentiallyEmitEvent(final ArangoMappingEvent<?> event) {
		if (eventPublisher != null) {
			eventPublisher.publishEvent(event);
		}
	}

	private void potentiallyEmitBeforeSaveEvent(final Iterable<?> values) {
		for (final Object value : values) {
			potentiallyEmitEvent(new BeforeSaveEvent<>(value));
		}
	}

	private void potentiallyEmitAfterSaveEvent(final Iterable<?> values,
			final MultiDocumentEntity<? extends DocumentEntity> result) {

		final Iterator<?> valueIterator = values.iterator();
		final Iterator<?> documentIterator = result.getDocumentsAndErrors().iterator();

		while (valueIterator.hasNext() && documentIterator.hasNext()) {
			final Object nextDoc = documentIterator.next();
			final Object nextValue = valueIterator.next();
			if (nextDoc instanceof DocumentEntity) {
				potentiallyEmitEvent(new AfterSaveEvent<>(nextValue));
			}
		}
	}

	private void potentiallyEmitBeforeDeleteEvent(final Iterable<?> values, final Class<?> type) {
		for (final Object value : values) {
			potentiallyEmitEvent(new BeforeDeleteEvent<>(value, type));
		}
	}

	private void potentiallyEmitAfterDeleteEvent(final Iterable<?> values, final Class<?> entityClass,
			final MultiDocumentEntity<? extends DocumentEntity> result) {

		final Iterator<?> valueIterator = values.iterator();
		final Iterator<?> documentIterator = result.getDocumentsAndErrors().iterator();

		while (valueIterator.hasNext() && documentIterator.hasNext()) {
			final Object nextDoc = documentIterator.next();
			final Object nextValue = valueIterator.next();
			if (nextDoc instanceof DocumentEntity) {
				potentiallyEmitEvent(new AfterDeleteEvent<>(nextValue, entityClass));
			}
		}
	}

	@Override
	public ResolverFactory getResolverFactory() {
		return this.resolverFactory;
	}

}
