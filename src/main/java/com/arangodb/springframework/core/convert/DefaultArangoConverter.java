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

package com.arangodb.springframework.core.convert;

import static com.arangodb.springframework.core.util.MetadataUtils.determineDocumentKeyFromId;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.convert.EntityInstantiator;
import org.springframework.data.convert.EntityInstantiators;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.ConvertingPropertyAccessor;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.data.mapping.model.PersistentEntityParameterValueProvider;
import org.springframework.data.mapping.model.PropertyValueProvider;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import com.arangodb.springframework.core.convert.resolver.LazyLoadingProxy;
import com.arangodb.springframework.core.convert.resolver.ResolverFactory;
import com.arangodb.springframework.core.mapping.ArangoPersistentEntity;
import com.arangodb.springframework.core.mapping.ArangoPersistentProperty;
import com.arangodb.springframework.core.mapping.ArangoSimpleTypes;
import com.arangodb.springframework.core.util.MetadataUtils;

/**
 * @author Mark Vollmary
 * @author Christian Lechner
 * @author Reşat SABIQ
 */
public class DefaultArangoConverter implements ArangoConverter {

	private static final String _ID = "_id";
	private static final String _KEY = "_key";
	private final MappingContext<? extends ArangoPersistentEntity<?>, ArangoPersistentProperty> context;
	private final CustomConversions conversions;
	private final GenericConversionService conversionService;
	private final EntityInstantiators instantiators;
	private final ResolverFactory resolverFactory;
	private final ArangoTypeMapper typeMapper;

	public DefaultArangoConverter(
		final MappingContext<? extends ArangoPersistentEntity<?>, ArangoPersistentProperty> context,
		final CustomConversions conversions, final ResolverFactory resolverFactory, final ArangoTypeMapper typeMapper) {
		super();
		this.context = context;
		this.conversions = conversions;
		this.resolverFactory = resolverFactory;
		this.typeMapper = typeMapper;
		conversionService = new DefaultConversionService();
		conversions.registerConvertersIn(conversionService);
		instantiators = new EntityInstantiators();
	}

	@Override
	public MappingContext<? extends ArangoPersistentEntity<?>, ArangoPersistentProperty> getMappingContext() {
		return context;
	}

	@Override
	public ArangoTypeMapper getTypeMapper() {
		return typeMapper;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <R> R read(final Class<R> type, final DBEntity source) {
		return (R) read(ClassTypeInformation.from(type), source);
	}

	private Object read(final TypeInformation<?> type, final DBEntity source) {
		if (source == null) {
			return null;
		}

		final TypeInformation<?> typeToUse = typeMapper.readType(source, type);

		if (conversions.hasCustomReadTarget(source.getClass(), typeToUse.getType())) {
			return conversionService.convert(source, typeToUse.getType());
		}

		if (DBEntity.class.isAssignableFrom(typeToUse.getType())) {
			return source;
		}

		if (typeToUse.isMap()) {
			return readMap(typeToUse, DBDocumentEntity.class.cast(source));
		}
		if (typeToUse.isCollectionLike()) {
			return readCollection(typeToUse, DBCollectionEntity.class.cast(source));
		}

		// no type information available => stick to the given type of the source
		if (typeToUse.equals(ClassTypeInformation.OBJECT)) {
			if (source instanceof DBDocumentEntity) {
				return readMap(ClassTypeInformation.MAP, DBDocumentEntity.class.cast(source));
			} else if (source instanceof DBCollectionEntity) {
				return readCollection(ClassTypeInformation.LIST, DBCollectionEntity.class.cast(source));
			}
			return source;
		}

		final ArangoPersistentEntity<?> entity = context.getRequiredPersistentEntity(typeToUse.getType());
		return read(typeToUse, source, entity);
	}

	private Object readMap(final TypeInformation<?> type, final DBDocumentEntity source) {
		final Class<?> keyType = getNonNullComponentType(type).getType();
		final TypeInformation<?> valueType = getNonNullMapValueType(type);
		final Map<Object, Object> map = CollectionFactory.createMap(type.getType(), keyType, source.size());
		for (final Map.Entry<String, Object> entry : source.entrySet()) {
			if (typeMapper.isTypeKey(entry.getKey())) {
				continue;
			}
			final Object key = convertIfNecessary(entry.getKey(), keyType);
			final Object value = entry.getValue();
			if (value instanceof DBEntity) {
				map.put(key, read(valueType, (DBEntity) value));
			} else {
				map.put(key, convertIfNecessary(value, valueType.getType()));
			}
		}
		return map;
	}

	private Object readCollection(final TypeInformation<?> type, final DBCollectionEntity source) {
		final Class<?> collectionType = Collection.class.isAssignableFrom(type.getType()) ? type.getType() : List.class;
		final TypeInformation<?> componentType = getNonNullComponentType(type);
		final Collection<Object> entries = type.getType().isArray() ? new ArrayList<>()
				: CollectionFactory.createCollection(collectionType, componentType.getType(), source.size());
		for (final Object entry : source) {
			if (entry instanceof DBEntity) {
				entries.add(read(componentType, (DBEntity) entry));
			} else {
				entries.add(convertIfNecessary(entry, componentType.getType()));
			}
		}
		return entries;
	}

	private Object read(final TypeInformation<?> type, final DBEntity source, final ArangoPersistentEntity<?> entity) {
		final EntityInstantiator instantiatorFor = instantiators.getInstantiatorFor(entity);
		final ParameterValueProvider<ArangoPersistentProperty> provider = getParameterProvider(entity, source);
		final Object instance = instantiatorFor.createInstance(entity, provider);
		final ConvertingPropertyAccessor accessor = new ConvertingPropertyAccessor(entity.getPropertyAccessor(instance),
				conversionService);

		entity.doWithProperties((final ArangoPersistentProperty property) -> {
			if (!entity.isConstructorArgument(property)) {
				readProperty(source.get(_ID), accessor, source.get(property.getFieldName()), property);
			}
		});
		entity.doWithAssociations((final Association<ArangoPersistentProperty> association) -> {
			final ArangoPersistentProperty property = association.getInverse();
			if (!entity.isConstructorArgument(property)) {
				readProperty(source.get(_ID), accessor, source.get(property.getFieldName()), property);
			}
		});
		return instance;
	}

	private ParameterValueProvider<ArangoPersistentProperty> getParameterProvider(
		final ArangoPersistentEntity<?> entity,
		final DBEntity source) {
		final PropertyValueProvider<ArangoPersistentProperty> provider = new ArangoPropertyValueProvider(source);
		return new PersistentEntityParameterValueProvider<>(entity, provider, null);
	}

	private class ArangoPropertyValueProvider implements PropertyValueProvider<ArangoPersistentProperty> {
		private final DBEntity source;

		public ArangoPropertyValueProvider(final DBEntity source) {
			super();
			this.source = source;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T getPropertyValue(final ArangoPersistentProperty property) {
			final Optional<Object> referenceOrRelation = readReferenceOrRelation(source.get(_ID),
				source.get(property.getFieldName()), property);
			return (T) referenceOrRelation.orElseGet(() -> convertIfNecessary(
				read(source.get(property.getFieldName()), property.getTypeInformation()), property.getType()));
		}

	}

	private void readProperty(
		final Object parentId,
		final ConvertingPropertyAccessor accessor,
		final Object source,
		final ArangoPersistentProperty property) {
		final Optional<Object> referenceOrRelation = readReferenceOrRelation(parentId, source, property);
		accessor.setProperty(property,
			referenceOrRelation.orElseGet(() -> read(source, property.getTypeInformation())));
	}

	private Optional<Object> readReferenceOrRelation(
		final Object parentId,
		final Object source,
		final ArangoPersistentProperty property) {
		Optional<Object> tmp = Optional.empty();
		if (source != null) {
			if (!tmp.isPresent()) {
				final Optional<Object> ref = property.getRef()
						.flatMap(annotation -> readReference(source, property, annotation));
				if (ref.isPresent()) {
					tmp = ref;
				}
			}
		}
		for (final Optional<? extends Annotation> annotation : Arrays.asList(property.getRelations(),
			property.getFrom(), property.getTo())) {
			final Optional<Object> relation = annotation.flatMap(a -> readRelation(parentId, source, property, a));
			if (relation.isPresent()) {
				tmp = relation;
				break;
			}
		}
		return tmp;
	}

	@SuppressWarnings("unchecked")
	private Optional<Object> readReference(
		final Object source,
		final ArangoPersistentProperty property,
		final Annotation annotation) {
		return resolverFactory.getReferenceResolver(annotation).flatMap(resolver -> {
			if (property.isCollectionLike()) {
				final Collection<String> ids;
				try {
					ids = (Collection<String>) asCollection(source);
				} catch (final Exception e) {
					throw new MappingException(
							"Collection of type String expected for references but found type " + source.getClass());
				}
				return Optional.ofNullable(resolver.resolveMultiple(ids, property.getTypeInformation(), annotation));
			} else {
				if (!(source instanceof String)) {
					throw new MappingException(
							"Type String expected for reference but found type " + source.getClass());
				}
				return Optional
						.ofNullable(resolver.resolveOne(source.toString(), property.getTypeInformation(), annotation));
			}
		});
	}

	private <A extends Annotation> Optional<Object> readRelation(
		final Object parentId,
		final Object source,
		final ArangoPersistentProperty property,
		final A annotation) {
		return resolverFactory.getRelationResolver(annotation).flatMap(resolver -> {
			if (property.isCollectionLike() && parentId != null) {
				return Optional
						.of(resolver.resolveMultiple(parentId.toString(), property.getTypeInformation(), annotation));
			} else if (source != null) {
				return Optional.of(resolver.resolveOne(source.toString(), property.getTypeInformation(), annotation));
			}
			return Optional.empty();
		});
	}

	@SuppressWarnings("unchecked")
	private <T> T read(final Object source, final TypeInformation<?> type) {
		if (source == null) {
			return null;
		}
		if (conversions.hasCustomReadTarget(source.getClass(), type.getType())) {
			return (T) conversionService.convert(source, type.getType());
		}
		if (source instanceof DBEntity) {
			return (T) read(type, DBEntity.class.cast(source));
		}
		return (T) source;
	}

	@Override
	public void write(final Object source, final DBEntity sink) {
		if (source == null) {
			return;
		}

		final Object entity = source instanceof LazyLoadingProxy ? ((LazyLoadingProxy) source).getEntity() : source;

		if (sink instanceof DBDocumentEntity
				&& conversions.hasCustomWriteTarget(entity.getClass(), DBDocumentEntity.class)) {
			final DBDocumentEntity result = conversionService.convert(entity, DBDocumentEntity.class);
			((DBDocumentEntity) sink).putAll(result);
			return;
		}

		final TypeInformation<?> type = ClassTypeInformation.from(ClassUtils.getUserClass(entity.getClass()));
		final TypeInformation<?> definedType = ClassTypeInformation.OBJECT;

		write(entity, type, sink, definedType);
	}

	@SuppressWarnings("unchecked")
	private void write(
		final Object source,
		final TypeInformation<?> type,
		final DBEntity sink,
		final TypeInformation<?> definedType) {

		if (type.isMap()) {
			writeMap((Map<Object, Object>) source, sink, definedType);
			return;
		}
		if (type.isCollectionLike()) {
			writeCollection(source, sink, definedType);
			return;
		}
		write(source, sink, context.getRequiredPersistentEntity(type));
		addTypeKeyIfNecessary(definedType, source, sink);
	}

	private void write(final Object source, final DBEntity sink, final ArangoPersistentEntity<?> entity) {

		final PersistentPropertyAccessor accessor = entity.getPropertyAccessor(source);

		entity.doWithProperties((final ArangoPersistentProperty property) -> {
			if (!property.isWritable()) {
				return;
			}
			final Object propertyObj = accessor.getProperty(property);
			if (propertyObj != null) {
				writeProperty(propertyObj, sink, property);
			}
		});
		entity.doWithAssociations((final Association<ArangoPersistentProperty> association) -> {
			final ArangoPersistentProperty inverse = association.getInverse();
			final Object property = accessor.getProperty(inverse);
			if (property != null) {
				writeProperty(property, sink, inverse);
			}
		});
		final Object id = sink.get(_ID);
		if (id != null && sink.get(_KEY) == null) {
			sink.put(_KEY, determineDocumentKeyFromId(id.toString()));
		}
	}

	@SuppressWarnings("unchecked")
	private void writeProperty(final Object source, final DBEntity sink, final ArangoPersistentProperty property) {
		if (source == null) {
			return;
		}

		final TypeInformation<?> sourceType = ClassTypeInformation.from(source.getClass());
		final String fieldName = property.getFieldName();

		if (property.getRef().isPresent()) {
			if (sourceType.isCollectionLike()) {
				final Collection<Object> ids = new ArrayList<>();
				for (final Object ref : createCollection(asCollection(source), property)) {
					getId(ref).ifPresent(id -> ids.add(id));
				}
				sink.put(fieldName, ids);
			} else {
				getId(source).ifPresent(id -> sink.put(fieldName, id));
			}
			return;
		}
		if (property.getRelations().isPresent()) {
			return;
		}
		if (property.getFrom().isPresent() || property.getTo().isPresent()) {
			if (!sourceType.isCollectionLike()) {
				getId(source).ifPresent(id -> sink.put(fieldName, id));
			}
			return;
		}
		
		final Object entity = source instanceof LazyLoadingProxy ? ((LazyLoadingProxy) source).getEntity() : source;
		final TypeInformation<?> entityType = (entity == source) ? sourceType : ClassTypeInformation.from(entity.getClass());
		
		if (conversions.isSimpleType(entityType.getType())) {
			final Optional<Class<?>> customWriteTarget = conversions.getCustomWriteTarget(entityType.getType());
			final Class<?> targetType = customWriteTarget.orElseGet(() -> entityType.getType());
			sink.put(fieldName, conversionService.convert(entity, targetType));
			return;
		}
		if (entityType.isCollectionLike()) {
			final DBEntity collection = new DBCollectionEntity();
			writeCollection(entity, collection, property.getTypeInformation());
			sink.put(fieldName, collection);
			return;
		}
		if (entityType.isMap()) {
			final DBEntity map = new DBDocumentEntity();
			writeMap((Map<Object, Object>) entity, map, property.getTypeInformation());
			sink.put(fieldName, map);
			return;
		}
		final ArangoPersistentEntity<?> persistentEntity = context.getRequiredPersistentEntity(entityType);
		final DBEntity document = new DBDocumentEntity();
		write(entity, document, persistentEntity);
		addTypeKeyIfNecessary(property.getTypeInformation(), entity, document);
		sink.put(fieldName, document);
		return;
	}

	private void writeMap(final Map<Object, Object> source, final DBEntity sink, final TypeInformation<?> definedType) {
		for (final Entry<Object, Object> entry : source.entrySet()) {
			final Object key = entry.getKey();
			if (!conversions.isSimpleType(key.getClass()) || key instanceof DBEntity) {
				throw new MappingException(
						"Complex type " + key.getClass().getName() + " is not allowed as a map key!");
			}
			final Object value = entry.getValue();
			final Class<? extends Object> valueType = value.getClass();
			if (conversions.isSimpleType(valueType)) {
				final Optional<Class<?>> customWriteTarget = conversions.getCustomWriteTarget(valueType);
				final Class<?> targetType = customWriteTarget.orElseGet(() -> valueType);
				sink.put(convertKey(key), conversionService.convert(value, targetType));
			} else {
				final DBEntity entity = createDBEntity(valueType);
				write(value, ClassTypeInformation.from(valueType), entity, getNonNullMapValueType(definedType));
				sink.put(convertKey(key), entity);
			}
		}
	}

	private void writeCollection(final Object source, final DBEntity sink, final TypeInformation<?> definedType) {
		for (final Object entry : asCollection(source)) {
			final Class<? extends Object> valueType = entry.getClass();
			if (conversions.isSimpleType(valueType)) {
				final Optional<Class<?>> customWriteTarget = conversions.getCustomWriteTarget(valueType);
				final Class<?> targetType = customWriteTarget.orElseGet(() -> valueType);
				sink.add(conversionService.convert(entry, targetType));
			} else {
				final DBEntity entity = createDBEntity(valueType);
				write(entry, ClassTypeInformation.from(valueType), entity, getNonNullComponentType(definedType));
				sink.add(entity);
			}
		}
	}

	private Optional<Object> getId(final Object source) {
		return getId(source, context.getPersistentEntity(source.getClass()));
	}

	private Optional<Object> getId(final Object source, final ArangoPersistentEntity<?> entity) {
		if (source instanceof LazyLoadingProxy) {
			return Optional.of(((LazyLoadingProxy) source).getRefId());
		}
		
		final Object id = entity.getIdentifierAccessor(source).getIdentifier();
		if (id != null) {
			return Optional.of(id);
		}

		final Optional<Object> optKey = entity.getKeyProperty()
				.map(prop -> entity.getPropertyAccessor(source).getProperty(prop));
		return optKey.map(key -> MetadataUtils.createIdFromCollectionAndKey(entity.getCollection(), convertKey(key)));
	}

	private Collection<?> createCollection(final Collection<?> source, final ArangoPersistentProperty property) {
		return source.stream()
				.map(
					s -> conversionService.convert(s, getNonNullComponentType(property.getTypeInformation()).getType()))
				.collect(Collectors.toList());
	}

	private static Collection<?> asCollection(final Object source) {
		return (source instanceof Collection) ? Collection.class.cast(source)
				: source.getClass().isArray() ? CollectionUtils.arrayToList(source) : Collections.singleton(source);
	}

	private DBEntity createDBEntity(final Class<?> type) {
		return isCollectionType(type) ? new DBCollectionEntity() : new DBDocumentEntity();
	}

	private boolean isArangoSimpleType(final Class<?> type) {
		return ArangoSimpleTypes.HOLDER.isSimpleType(type);
	}

	@Override
	public boolean isCollectionType(final Class<?> type) {
		return type.isArray() || Iterable.class.equals(type) || Collection.class.isAssignableFrom(type);
	}

	private boolean isMapType(final Class<?> type) {
		return Map.class.isAssignableFrom(type);
	}

	@Override
	public GenericConversionService getConversionService() {
		return conversionService;
	}

	@Override
	public boolean isEntityType(final Class<?> type) {
		return !isArangoSimpleType(type) && !isMapType(type) && !isCollectionType(type);
	}

	@SuppressWarnings("unchecked")
	private <T> T convertIfNecessary(final Object source, final Class<T> type) {
		return (T) (source == null ? source
				: type.isAssignableFrom(source.getClass()) ? source : conversionService.convert(source, type));
	}

	private void addTypeKeyIfNecessary(final TypeInformation<?> definedType, final Object value, final DBEntity sink) {
		final Class<?> referenceType = definedType != null ? definedType.getType() : Object.class;
		final Class<?> valueType = ClassUtils.getUserClass(value.getClass());
		if (!valueType.equals(referenceType)) {
			typeMapper.writeType(valueType, sink);
		}
	}

	private String convertKey(final Object key) {
		if (key instanceof String) {
			return (String) key;
		}
		final boolean hasCustomConverter = conversions.hasCustomWriteTarget(key.getClass(), String.class);
		return hasCustomConverter ? conversionService.convert(key, String.class) : key.toString();
	}

	private TypeInformation<?> getNonNullComponentType(final TypeInformation<?> type) {
		final TypeInformation<?> compType = type.getComponentType();
		return compType != null ? compType : ClassTypeInformation.OBJECT;
	}

	private TypeInformation<?> getNonNullMapValueType(final TypeInformation<?> type) {
		final TypeInformation<?> valueType = type.getMapValueType();
		return valueType != null ? valueType : ClassTypeInformation.OBJECT;
	}

}
