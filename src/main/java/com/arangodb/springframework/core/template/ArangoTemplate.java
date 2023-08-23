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
import com.arangodb.entity.*;
import com.arangodb.model.*;
import com.arangodb.springframework.annotation.*;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.CollectionOperations;
import com.arangodb.springframework.core.UserOperations;
import com.arangodb.springframework.core.convert.ArangoConverter;
import com.arangodb.springframework.core.convert.resolver.ResolverFactory;
import com.arangodb.springframework.core.mapping.ArangoPersistentEntity;
import com.arangodb.springframework.core.mapping.ArangoPersistentProperty;
import com.arangodb.springframework.core.mapping.event.*;
import com.arangodb.springframework.core.template.DefaultUserOperation.CollectionCallback;
import com.arangodb.springframework.core.util.ArangoExceptionTranslator;
import com.arangodb.springframework.core.util.MetadataUtils;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.expression.BeanFactoryAccessor;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

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

	private static final Logger LOGGER = LoggerFactory.getLogger(ArangoTemplate.class);
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

	@Override
	public ArangoDatabase db() throws DataAccessException {
		final String key = databaseExpression != null ? databaseExpression.getValue(context, String.class)
				: databaseName;
        return databaseCache.computeIfAbsent(key, name -> {
            final ArangoDatabase db = arango.db(name);
			try {
            if (!db.exists()) {
                db.create();
            }
            return db;
			} catch (ArangoDBException error) {
				throw DataAccessUtils.translateIfNecessary(error, exceptionTranslator);
			}
        });
    }

	private ArangoCollection _collection(final String name, boolean transactional) {
		return _collection(name, null, null, transactional);
    }

	private ArangoCollection _collection(final Class<?> entityClass, boolean transactional) {
		return _collection(entityClass, null, transactional);
    }

	private ArangoCollection _collection(final Class<?> entityClass, final Object id, boolean transactional) {
        final ArangoPersistentEntity<?> persistentEntity = converter.getMappingContext()
                .getRequiredPersistentEntity(entityClass);
        final String name = determineCollectionFromId(id).orElse(persistentEntity.getCollection());
		return _collection(name, persistentEntity, persistentEntity.getCollectionOptions(), transactional);
    }

    private ArangoCollection _collection(final String name, final ArangoPersistentEntity<?> persistentEntity,
										 final CollectionCreateOptions options, boolean transactional) {

        final ArangoDatabase db = db();
        final Class<?> entityClass = persistentEntity != null ? persistentEntity.getType() : null;
        final CollectionCacheValue value = collectionCache.computeIfAbsent(new CollectionCacheKey(db.name(), name),
                key -> {
                    final ArangoCollection collection = db.collection(name);
                    if (!collection.exists()) {
						if (transactional) {
							throw new InvalidDataAccessResourceUsageException("Missing collection cannot be created during transaction");
						}
                        collection.create(options);
                    }
                    return new CollectionCacheValue(collection);
                });
        final Collection<Class<?>> entities = value.getEntities();
        final ArangoCollection collection = value.getCollection();
        if (persistentEntity != null && !entities.contains(entityClass)) {
            value.addEntityClass(entityClass);
			if (transactional) {
				LOGGER.debug("Not ensuring any indexes of collection {} for {} during transaction", collection.name(), entityClass);
			} else {
            	ensureCollectionIndexes(collection(collection), persistentEntity);
        	}
		}
        return collection;
    }

    @SuppressWarnings("deprecation")
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
        persistentEntity.getMDIndexes().forEach(index -> ensureMDIndex(collection, index));
        persistentEntity.getMDPrefixedIndexes().forEach(index -> ensureMDPrefixedIndex(collection, index));
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

    private static void ensureMDIndex(final CollectionOperations collection, final MDIndex annotation) {
        collection.ensureMDIndex(Arrays.asList(annotation.fields()),
                new MDIndexOptions()
                        .unique(annotation.unique())
                        .fieldValueTypes(annotation.fieldValueTypes())
                        .sparse(annotation.sparse())
        );
    }

    private static void ensureMDPrefixedIndex(final CollectionOperations collection, final MDPrefixedIndex annotation) {
        collection.ensureMDPrefixedIndex(Arrays.asList(annotation.fields()),
                new MDPrefixedIndexOptions()
                        .prefixFields(Arrays.asList(annotation.prefixFields()))
                        .unique(annotation.unique())
                        .fieldValueTypes(annotation.fieldValueTypes())
                        .sparse(annotation.sparse())
        );
    }

    private Optional<String> determineCollectionFromId(final Object id) {
        return id != null ? Optional.ofNullable(MetadataUtils.determineCollectionFromId(converter.convertId(id)))
                : Optional.empty();
    }

    private String determineDocumentKeyFromId(final Object id) {
        return MetadataUtils.determineDocumentKeyFromId(converter.convertId(id));
    }

    public RuntimeException translateException(RuntimeException e) {
        return DataAccessUtils.translateIfNecessary(e, exceptionTranslator);
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
            throw translateException(e);
        }
    }

    @Override
    public <T> ArangoCursor<T> query(final String query, final Map<String, Object> bindVars,
                                     final AqlQueryOptions options, final Class<T> entityClass) throws DataAccessException {
        try {
            ArangoCursor<T> cursor = db().query(query, entityClass, bindVars == null ? null : prepareBindVars(bindVars, false), options);
            return new ArangoExtCursor<>(cursor, entityClass, eventPublisher);
        } catch (final ArangoDBException e) {
            throw translateException(e);
        }
    }

	private Map<String, Object> prepareBindVars(final Map<String, Object> bindVars, boolean transactional) {
        final Map<String, Object> prepared = new HashMap<>(bindVars.size());
        for (final Entry<String, Object> entry : bindVars.entrySet()) {
            if (entry.getKey().startsWith("@") && entry.getValue() instanceof Class<?> clazz) {
				prepared.put(entry.getKey(), _collection(clazz, transactional).name());
            } else {
                prepared.put(entry.getKey(), entry.getValue());
            }
        }
        return prepared;
    }

    @Override
    public <T> MultiDocumentEntity<DocumentDeleteEntity<T>> deleteAll(
            final Iterable<?> values,
            final DocumentDeleteOptions options,
            final Class<T> entityClass
    ) throws DataAccessException {

        potentiallyEmitBeforeDeleteEvent(values, entityClass);

        MultiDocumentEntity<DocumentDeleteEntity<T>> result;
        try {
			result = _collection(entityClass, options.getStreamTransactionId() != null).deleteDocuments(toList(values), options, entityClass);
        } catch (final ArangoDBException e) {
            throw translateException(e);
        }

        potentiallyEmitAfterDeleteEvent(values, entityClass, result);
        return result;
    }

    @Override
    public <T> MultiDocumentEntity<DocumentDeleteEntity<T>> deleteAllById(Iterable<?> ids, DocumentDeleteOptions options, Class<T> entityClass) throws DataAccessException {
        if (ids == null) {
            throw new IllegalArgumentException("ids must not be null");
        }
        ArrayList<String> convertedIds = new ArrayList<>();
        for (Object id : ids) {
            if (id == null) {
                throw new IllegalArgumentException("id must not be null");
            }
            convertedIds.add(converter.convertId(id));
        }
        return deleteAll(convertedIds, options, entityClass);
    }

    @Override
    public <T> DocumentDeleteEntity<T> delete(final Object id, final DocumentDeleteOptions options, final Class<T> entityClass)
            throws DataAccessException {

        potentiallyEmitEvent(new BeforeDeleteEvent<>(id, entityClass));

        DocumentDeleteEntity<T> result;
        try {
			result = _collection(entityClass, id, options.getStreamTransactionId() != null).deleteDocument(determineDocumentKeyFromId(id), options, entityClass);
        } catch (final ArangoDBException e) {
            throw translateException(e);
        }

        potentiallyEmitEvent(new AfterDeleteEvent<>(id, entityClass));
        return result;
    }

    @Override
    public <T> MultiDocumentEntity<DocumentUpdateEntity<T>> updateAll(
            final Iterable<? extends T> values,
            final DocumentUpdateOptions options,
            final Class<T> entityClass
    ) throws DataAccessException {

        potentiallyEmitBeforeSaveEvent(values);

        MultiDocumentEntity<DocumentUpdateEntity<T>> result;
        try {
			result = _collection(entityClass, options.getStreamTransactionId() != null).updateDocuments(toList(values), options, entityClass);
        } catch (final ArangoDBException e) {
            throw translateException(e);
        }

        updateDBFields(values, result);
        potentiallyEmitAfterSaveEvent(values, result);
        return result;
    }

    @Override
    public <T> DocumentUpdateEntity<T> update(final Object id, final T value, final DocumentUpdateOptions options)
            throws DataAccessException {

        potentiallyEmitEvent(new BeforeSaveEvent<>(value));

        DocumentUpdateEntity<T> result;
        try {
			result = _collection(value.getClass(), id, options.getStreamTransactionId() != null).updateDocument(determineDocumentKeyFromId(id), value, options);
        } catch (final ArangoDBException e) {
            throw translateException(e);
        }

        updateDBFields(value, result);
        potentiallyEmitEvent(new AfterSaveEvent<>(value));
        return result;
    }

    @Override
    public <T> MultiDocumentEntity<DocumentUpdateEntity<T>> replaceAll(
            final Iterable<? extends T> values,
            final DocumentReplaceOptions options,
            final Class<T> entityClass
    ) throws DataAccessException {

        potentiallyEmitBeforeSaveEvent(values);

        MultiDocumentEntity<DocumentUpdateEntity<T>> result;
        try {
            result = _collection(entityClass, options.getStreamTransactionId() != null).replaceDocuments(toList(values), options, entityClass);
        } catch (final ArangoDBException e) {
            throw translateException(e);
        }

        updateDBFields(values, result);
        potentiallyEmitAfterSaveEvent(values, result);
        return result;
    }

    @Override
    public <T> DocumentUpdateEntity<T> replace(final Object id, final T value, final DocumentReplaceOptions options)
            throws DataAccessException {
        potentiallyEmitEvent(new BeforeSaveEvent<>(value));

        DocumentUpdateEntity<T> result;
        try {
            result = _collection(value.getClass(), id, false).replaceDocument(determineDocumentKeyFromId(id), value, options);
        } catch (final ArangoDBException e) {
            throw translateException(e);
        }

        updateDBFields(value, result);
        potentiallyEmitEvent(new AfterSaveEvent<>(value));
        return result;
    }

    @Override
    public <T> Optional<T> find(final Object id, final Class<T> entityClass, final DocumentReadOptions options)
            throws DataAccessException {
        try {
            T res = _collection(entityClass, id, options.getStreamTransactionId() != null).getDocument(determineDocumentKeyFromId(id), entityClass, options);
            if (res != null) {
                potentiallyEmitEvent(new AfterLoadEvent<>(res));
            }
            return Optional.ofNullable(res);
        } catch (final ArangoDBException e) {
            throw translateException(e);
        }
    }

    @Override
	public <T> Iterable<T> findAll(final Class<T> entityClass, DocumentReadOptions options) throws DataAccessException {
        final String query = "FOR entity IN @@col RETURN entity";
        final Map<String, Object> bindVars = Collections.singletonMap("@col", entityClass);
		return query(query, bindVars, asQueryOptions(options), entityClass).asListRemaining();
    }

    @Override
	public <T> Iterable<T> findAll(final Iterable<?> ids, final Class<T> entityClass, DocumentReadOptions options)
            throws DataAccessException {
        try {
            final Collection<String> keys = new ArrayList<>();
            ids.forEach(id -> keys.add(determineDocumentKeyFromId(id)));
			Collection<T> docs = _collection(entityClass, options.getStreamTransactionId() != null).getDocuments(keys, entityClass).getDocuments();
            for (T doc : docs) {
                if (doc != null) {
                    potentiallyEmitEvent(new AfterLoadEvent<>(doc));
                }
            }
            return docs;
        } catch (final ArangoDBException e) {
            throw translateException(e);
        }
    }

    @Override
    public <T> MultiDocumentEntity<DocumentCreateEntity<T>> insertAll(
            final Iterable<? extends T> values, final DocumentCreateOptions options, final Class<T> entityClass) throws DataAccessException {

        potentiallyEmitBeforeSaveEvent(values);

        MultiDocumentEntity<DocumentCreateEntity<T>> result;
        try {
            result = _collection(entityClass, options.getStreamTransactionId() != null).insertDocuments(toList(values), options, entityClass);
        } catch (final ArangoDBException e) {
            throw translateException(e);
        }

        updateDBFields(values, result);
        potentiallyEmitAfterSaveEvent(values, result);
        return result;
    }

    @Override
    public <T> DocumentCreateEntity<T> insert(final T value, final DocumentCreateOptions options) throws DataAccessException {
        potentiallyEmitEvent(new BeforeSaveEvent<>(value));

        DocumentCreateEntity<T> result;
        try {
            result = _collection(value.getClass(), options.getStreamTransactionId() != null).insertDocument(value, options);
        } catch (final ArangoDBException e) {
            throw translateException(e);
        }

        updateDBFields(value, result);
        potentiallyEmitEvent(new AfterSaveEvent<>(value));
        return result;
    }

    @Override
    public <T> T repsert(final T value, AqlQueryOptions options) throws DataAccessException {
        @SuppressWarnings("unchecked") final Class<T> clazz = (Class<T>) value.getClass();
		final String collectionName = _collection(clazz, options.getStreamTransactionId() != null).name();

        potentiallyEmitEvent(new BeforeSaveEvent<>(value));

        Map<String, Object> bindVars = new HashMap<>();
        bindVars.put("@col", collectionName);
        bindVars.put("doc", value);

        final T result;
        try {
            ArangoCursor<T> it = query(
                    REPSERT_QUERY,
                    bindVars,
					options, clazz
            );
            result = it.hasNext() ? it.next() : null;
        } catch (final ArangoDBException e) {
            throw translateException(e);
        }

        updateDBFieldsFromObject(value, result);
        potentiallyEmitEvent(new AfterSaveEvent<>(result));
        return result;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public <T> Iterable<T> repsertAll(final Iterable<T> values, final Class<? super T> entityClass, AqlQueryOptions options) throws DataAccessException {
        if (!values.iterator().hasNext()) {
            return Collections.emptyList();
        }

		final String collectionName = _collection(entityClass, options.getStreamTransactionId() != null).name();
        potentiallyEmitBeforeSaveEvent(values);

        Map<String, Object> bindVars = new HashMap<>();
        bindVars.put("@col", collectionName);
        bindVars.put("docs", values);

        List result;
        try {
            result = query(
                    REPSERT_MANY_QUERY,
                    bindVars,
					options, entityClass
            ).asListRemaining();
        } catch (final ArangoDBException e) {
            throw translateException(e);
        }

        updateDBFieldsFromObjects(values, result);
        result.forEach(it -> potentiallyEmitEvent(new AfterSaveEvent<>(it)));
        return result;
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

        for (ArangoPersistentProperty propRead : entityToRead.getComputedValuesProperties().values()) {
            ArangoPersistentProperty propWrite = entityToModify.getComputedValuesProperties().get(propRead.getName());
            if (propWrite != null && !propWrite.isImmutable()) {
                accessorToWrite.setProperty(propWrite, accessorToRead.getProperty(propRead));
            }
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
            converter.readProperty(entity, accessor, JsonNodeFactory.instance.textNode(documentEntity.getKey()), idProperty);
        }
        entity.getArangoIdProperty().filter(arangoId -> !arangoId.isImmutable())
                .ifPresent(arangoId -> accessor.setProperty(arangoId, documentEntity.getId()));
        entity.getRevProperty().filter(rev -> !rev.isImmutable())
                .ifPresent(rev -> accessor.setProperty(rev, documentEntity.getRev()));
    }

    @Override
	public boolean exists(final Object id, final Class<?> entityClass, DocumentExistsOptions options) throws DataAccessException {
        try {
			return _collection(entityClass, options.getStreamTransactionId() != null).documentExists(determineDocumentKeyFromId(id), options);
        } catch (final ArangoDBException e) {
            throw translateException(e);
        }
    }

    @Override
    public void dropDatabase() throws DataAccessException {
        final ArangoDatabase db = db();
        try {
            db.drop();
        } catch (final ArangoDBException e) {
            throw translateException(e);
        }
        databaseCache.remove(db.name());
        collectionCache.keySet().stream().filter(key -> key.getDb().equals(db.name()))
                .forEach(collectionCache::remove);
    }

    @Override
    public CollectionOperations collection(final Class<?> entityClass) throws DataAccessException {
		return collection(_collection(entityClass, false));
    }

    @Override
    public CollectionOperations collection(final String name, final CollectionCreateOptions options)
            throws DataAccessException {
		return collection(_collection(name, null, options, false));
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
            throw translateException(e);
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

    private <T> List<T> toList(Iterable<T> it) {
        ArrayList<T> l = new ArrayList<>();
        it.forEach(l::add);
        return l;
    }

	private AqlQueryOptions asQueryOptions(DocumentReadOptions source) {
		return new AqlQueryOptions().streamTransactionId(source.getStreamTransactionId()).allowDirtyRead(source.getAllowDirtyRead());
	}
}
