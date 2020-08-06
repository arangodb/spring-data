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

import com.arangodb.entity.BaseDocument;
import com.arangodb.entity.BaseEdgeDocument;
import com.arangodb.springframework.annotation.*;
import com.arangodb.springframework.core.convert.resolver.LazyLoadingProxy;
import com.arangodb.springframework.core.convert.resolver.ReferenceResolver;
import com.arangodb.springframework.core.convert.resolver.RelationResolver;
import com.arangodb.springframework.core.convert.resolver.ResolverFactory;
import com.arangodb.springframework.core.mapping.ArangoPersistentEntity;
import com.arangodb.springframework.core.mapping.ArangoPersistentProperty;
import com.arangodb.springframework.core.mapping.ArangoSimpleTypes;
import com.arangodb.springframework.core.util.MetadataUtils;
import com.arangodb.velocypack.VPackBuilder;
import com.arangodb.velocypack.VPackSlice;
import com.arangodb.velocypack.ValueType;
import com.arangodb.velocypack.internal.util.DateUtil;
import com.arangodb.velocypack.module.jdk8.internal.util.JavaTimeUtil;
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
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.data.mapping.model.PersistentEntityParameterValueProvider;
import org.springframework.data.mapping.model.PropertyValueProvider;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Base64Utils;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.text.ParseException;
import java.time.*;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author Mark Vollmary
 * @author Christian Lechner
 * @author Reşat SABIQ
 */
public class DefaultArangoConverter implements ArangoConverter {

	private static final String _ID = "_id";
	private static final String _KEY = "_key";
	private static final String _REV = "_rev";
	private static final String _FROM = "_from";
	private static final String _TO = "_to";

	private final MappingContext<? extends ArangoPersistentEntity<?>, ArangoPersistentProperty> context;
	private final CustomConversions conversions;
	private final GenericConversionService conversionService;
	private final EntityInstantiators instantiators;
	private final ResolverFactory resolverFactory;
	private final ArangoTypeMapper typeMapper;

	public DefaultArangoConverter(
		final MappingContext<? extends ArangoPersistentEntity<?>, ArangoPersistentProperty> context,
		final CustomConversions conversions, final ResolverFactory resolverFactory, final ArangoTypeMapper typeMapper) {

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
	public <R> R read(final Class<R> type, final VPackSlice source) {
		return (R) readInternal(ClassTypeInformation.from(type), source);
	}

	private Object readInternal(final TypeInformation<?> type, final VPackSlice source) {
		if (source == null) {
			return null;
		}

		if (VPackSlice.class.isAssignableFrom(type.getType())) {
			return source;
		}

		final TypeInformation<?> typeToUse = (source.isArray() || source.isObject()) ? typeMapper.readType(source, type)
				: type;
		final Class<?> rawTypeToUse = typeToUse.getType();

		if (conversions.hasCustomReadTarget(VPackSlice.class, typeToUse.getType())) {
			return conversionService.convert(source, rawTypeToUse);
		}

		if (conversions.hasCustomReadTarget(DBDocumentEntity.class, typeToUse.getType())) {
			return conversionService.convert(readSimple(DBDocumentEntity.class, source), rawTypeToUse);
		}

		if (!source.isArray() && !source.isObject()) {
			return convertIfNecessary(readSimple(rawTypeToUse, source), rawTypeToUse);
		}

		if (DBDocumentEntity.class.isAssignableFrom(rawTypeToUse)) {
			return readSimple(rawTypeToUse, source);
		}

		if (BaseDocument.class.isAssignableFrom(rawTypeToUse)) {
			return readBaseDocument(rawTypeToUse, source);
		}

		if (typeToUse.isMap()) {
			return readMap(typeToUse, source);
		}

		if (!source.isArray() && ClassTypeInformation.OBJECT.equals(typeToUse)) {
			return readMap(ClassTypeInformation.MAP, source);
		}

		if (typeToUse.getType().isArray()) {
			return readArray(typeToUse, source);
		}

		if (typeToUse.isCollectionLike()) {
			return readCollection(typeToUse, source);
		}

		if (ClassTypeInformation.OBJECT.equals(typeToUse)) {
			return readCollection(ClassTypeInformation.COLLECTION, source);
		}

		final ArangoPersistentEntity<?> entity = context.getRequiredPersistentEntity(rawTypeToUse);
		return readEntity(typeToUse, source, entity);
	}

	private Object readEntity(
		final TypeInformation<?> type,
		final VPackSlice source,
		final ArangoPersistentEntity<?> entity) {

		if (!source.isObject()) {
			throw new MappingException(
					String.format("Can't read entity type %s from VPack type %s!", type, source.getType()));
		}

		final EntityInstantiator instantiator = instantiators.getInstantiatorFor(entity);
		final ParameterValueProvider<ArangoPersistentProperty> provider = getParameterProvider(entity, source);
		final Object instance = instantiator.createInstance(entity, provider);
		final PersistentPropertyAccessor<?> accessor = entity.getPropertyAccessor(instance);

		final String id = source.get(_ID).isString() ? source.get(_ID).getAsString() : null;

		entity.doWithProperties((final ArangoPersistentProperty property) -> {
			if (!entity.isConstructorArgument(property)) {
				final VPackSlice value = source.get(property.getFieldName());
				readProperty(entity, id, accessor, value, property);
			}
		});

		entity.doWithAssociations((final Association<ArangoPersistentProperty> association) -> {
			final ArangoPersistentProperty property = association.getInverse();
			if (!entity.isConstructorArgument(property)) {
				final VPackSlice value = source.get(property.getFieldName());
				readProperty(entity, id, accessor, value, property);
			}
		});

		return instance;
	}

	private void readProperty(
		final ArangoPersistentEntity<?> entity,
		final String parentId,
		final PersistentPropertyAccessor<?> accessor,
		final VPackSlice source,
		final ArangoPersistentProperty property) {

		final Object propertyValue = readPropertyValue(entity, parentId, source, property);
		if (propertyValue != null || !property.getType().isPrimitive()) {
			accessor.setProperty(property, propertyValue);
		}
	}

	private Object readPropertyValue(
		final ArangoPersistentEntity<?> entity,
		final String parentId,
		final VPackSlice source,
		final ArangoPersistentProperty property) {

		final Optional<Ref> ref = property.getRef();
		if (ref.isPresent()) {
			return readReference(source, property, ref.get()).orElse(null);
		}

		final Optional<Relations> relations = property.getRelations();
		if (relations.isPresent()) {
			return readRelation(entity, parentId, source, property, relations.get()).orElse(null);
		}

		final Optional<From> from = property.getFrom();
		if (from.isPresent()) {
			return readRelation(entity, parentId, source, property, from.get()).orElse(null);
		}

		final Optional<To> to = property.getTo();
		if (to.isPresent()) {
			return readRelation(entity, parentId, source, property, to.get()).orElse(null);
		}

		return readInternal(property.getTypeInformation(), source);
	}

	private Object readMap(final TypeInformation<?> type, final VPackSlice source) {
		if (!source.isObject()) {
			throw new MappingException(
					String.format("Can't read map type %s from VPack type %s!", type, source.getType()));
		}

		final Class<?> keyType = getNonNullComponentType(type).getType();
		final TypeInformation<?> valueType = getNonNullMapValueType(type);
		final Map<Object, Object> map = CollectionFactory.createMap(type.getType(), keyType, source.size());

		final Iterator<Entry<String, VPackSlice>> iterator = source.objectIterator();

		while (iterator.hasNext()) {
			final Entry<String, VPackSlice> entry = iterator.next();
			if (typeMapper.isTypeKey(entry.getKey())) {
				continue;
			}

			final Object key = convertIfNecessary(entry.getKey(), keyType);
			final VPackSlice value = entry.getValue();

			map.put(key, readInternal(valueType, value));
		}

		return map;
	}

	private Collection<?> readCollection(final TypeInformation<?> type, final VPackSlice source) {
		if (!source.isArray()) {
			throw new MappingException(
					String.format("Can't read collection type %s from VPack type %s!", type, source.getType()));
		}

		final TypeInformation<?> componentType = getNonNullComponentType(type);
		final Class<?> collectionType = Iterable.class.equals(type.getType()) ? Collection.class : type.getType();

		final Collection<Object> collection = Collection.class == collectionType || List.class == collectionType ?
				new ArrayList<>(source.getLength()) :
				CollectionFactory.createCollection(collectionType, componentType.getType(), source.getLength());

		final Iterator<VPackSlice> iterator = source.arrayIterator();

		while (iterator.hasNext()) {
			final VPackSlice elem = iterator.next();
			collection.add(readInternal(componentType, elem));
		}

		return collection;
	}

	private Object readArray(final TypeInformation<?> type, final VPackSlice source) {
		if (!source.isArray()) {
			throw new MappingException(
					String.format("Can't read array type %s from VPack type %s!", type, source.getType()));
		}

		final TypeInformation<?> componentType = getNonNullComponentType(type);
		final int length = source.getLength();
		final Object array = Array.newInstance(componentType.getType(), length);

		for (int i = 0; i < length; ++i) {
			Array.set(array, i, readInternal(componentType, source.get(i)));
		}

		return array;
	}

	@SuppressWarnings("unchecked")
	private Optional<Object> readReference(
		final VPackSlice source,
		final ArangoPersistentProperty property,
		final Annotation annotation) {

		final Optional<ReferenceResolver<Annotation>> resolver = resolverFactory.getReferenceResolver(annotation);

		if (!resolver.isPresent() || source.isNone()) {
			return Optional.empty();
		}

		else if (property.isCollectionLike()) {
			final Collection<String> ids;
			try {
				ids = (Collection<String>) readCollection(ClassTypeInformation.COLLECTION, source);
			} catch (final ClassCastException e) {
				throw new MappingException("All references must be of type String!", e);
			}

			return resolver.map(res -> res.resolveMultiple(ids, property.getTypeInformation(), annotation));
		}

		else {
			if (!source.isString()) {
				throw new MappingException(
						String.format("A reference must be of type String, but got VPack type %s!", source.getType()));
			}

			return resolver.map(res -> res.resolveOne(source.getAsString(), property.getTypeInformation(), annotation));
		}
	}

	private <A extends Annotation> Optional<Object> readRelation(
		final ArangoPersistentEntity<?> entity,
		final String parentId,
		final VPackSlice source,
		final ArangoPersistentProperty property,
		final A annotation) {

		final Class<? extends Annotation> collectionType = entity.findAnnotation(Edge.class) != null ? Edge.class
				: Document.class;
		final Optional<RelationResolver<Annotation>> resolver = resolverFactory.getRelationResolver(annotation,
			collectionType);

		if (!resolver.isPresent()) {
			return Optional.empty();
		}

		else if (property.isCollectionLike()) {
			if (parentId == null) {
				return Optional.empty();
			}
			return resolver.map(res -> res.resolveMultiple(parentId, property.getTypeInformation(), annotation));
		}

		else if (source.isString()) {
			return resolver.map(res -> res.resolveOne(source.getAsString(), property.getTypeInformation(), annotation));
		}

		else {
			return resolver.map(res -> res.resolveOne(parentId, property.getTypeInformation(), annotation));
		}

	}

	private Object readSimple(final Class<?> type, final VPackSlice source) {
		if (source.isNone() || source.isNull()) {
			return null;
		}

		if (source.isBoolean()) {
			return source.getAsBoolean();
		}

		if (source.isNumber()) {
			// primitives & wrappers
			if (byte.class.isAssignableFrom(type) || Byte.class.isAssignableFrom(type)) {
				return source.getAsByte();
			} //
			else if (short.class.isAssignableFrom(type) || Short.class.isAssignableFrom(type)) {
				return source.getAsShort();
			} //
			else if (int.class.isAssignableFrom(type) || Integer.class.isAssignableFrom(type)) {
				return source.getAsInt();
			} //
			else if (long.class.isAssignableFrom(type) || Long.class.isAssignableFrom(type)) {
				return source.getAsLong();
			} //
			else if (float.class.isAssignableFrom(type) || Float.class.isAssignableFrom(type)) {
				return source.getAsFloat();
			} //
			else if (double.class.isAssignableFrom(type) || Double.class.isAssignableFrom(type)) {
				return source.getAsDouble();
			}
			// java.math.*
			else if (BigInteger.class.isAssignableFrom(type)
					&& (source.isSmallInt() || source.isInt() || source.isUInt())) {
				return source.getAsBigInteger();
			} //
			else if (BigDecimal.class.isAssignableFrom(type) && source.isDouble()) {
				return source.getAsBigDecimal();
			} //
			else {
				return source.getAsNumber();
			}
		}

		if (source.isString()) {
			// java.lang.*
			if (Class.class.isAssignableFrom(type)) {
				try {
					return Class.forName(source.getAsString());
				} catch (final ClassNotFoundException e) {
					throw new MappingException(String.format("Could not load type %s!", source.getAsString()), e);
				}
			} //
			else if (Enum.class.isAssignableFrom(type)) {
				@SuppressWarnings({ "unchecked", "rawtypes" })
				final Enum<?> e = Enum.valueOf((Class<? extends Enum>) type, source.getAsString());
				return e;
			}
			// primitive array
			else if (byte[].class.isAssignableFrom(type)) {
				return Base64Utils.decodeFromString(source.getAsString());
			}
			// java.sql.*
			else if (java.sql.Date.class.isAssignableFrom(type)) {
				return new java.sql.Date(parseDate(source.getAsString()).getTime());
			} //
			else if (Timestamp.class.isAssignableFrom(type)) {
				return new Timestamp(parseDate(source.getAsString()).getTime());
			}
			// java.util.*
			else if (Date.class.isAssignableFrom(type)) {
				return parseDate(source.getAsString());
			}
			// java.math.*
			else if (BigInteger.class.isAssignableFrom(type)) {
				return source.getAsBigInteger();
			} //
			else if (BigDecimal.class.isAssignableFrom(type)) {
				return source.getAsBigDecimal();
			}
			// java.time.*
			else if (Instant.class.isAssignableFrom(type)) {
				return JavaTimeUtil.parseInstant(source.getAsString());
			} //
			else if (LocalDate.class.isAssignableFrom(type)) {
				return JavaTimeUtil.parseLocalDate(source.getAsString());
			} //
			else if (LocalDateTime.class.isAssignableFrom(type)) {
				return JavaTimeUtil.parseLocalDateTime(source.getAsString());
			} //
			else if (OffsetDateTime.class.isAssignableFrom(type)) {
				return JavaTimeUtil.parseOffsetDateTime(source.getAsString());
			} //
			else if (ZonedDateTime.class.isAssignableFrom(type)) {
				return JavaTimeUtil.parseZonedDateTime(source.getAsString());
			} //
			else {
				return source.getAsString();
			}
		}

		if (source.isObject()) {
			if (DBDocumentEntity.class.isAssignableFrom(type)) {
				return readDBDocumentEntity(source);
			}
		}

		throw new MappingException(String.format("Can't read type %s from VPack type %s!", type, source.getType()));
	}

	private BaseDocument readBaseDocument(final Class<?> type, final VPackSlice source) {
		@SuppressWarnings("unchecked")
		final Map<String, Object> properties = (Map<String, Object>) readMap(ClassTypeInformation.MAP, source);

		if (BaseDocument.class.equals(type)) {
			return new BaseDocument(properties);
		} //
		else if (BaseEdgeDocument.class.equals(type)) {
			return new BaseEdgeDocument(properties);
		} //
		else {
			throw new MappingException(String.format("Can't read type %s as %s!", type, BaseDocument.class));
		}
	}

	@SuppressWarnings("unchecked")
	private DBDocumentEntity readDBDocumentEntity(final VPackSlice source) {
		return new DBDocumentEntity((Map<String, Object>) readMap(ClassTypeInformation.MAP, source));
	}

	private ParameterValueProvider<ArangoPersistentProperty> getParameterProvider(
		final ArangoPersistentEntity<?> entity,
		final VPackSlice source) {

		final PropertyValueProvider<ArangoPersistentProperty> provider = new ArangoPropertyValueProvider(entity,
				source);
		return new PersistentEntityParameterValueProvider<>(entity, provider, null);
	}

	private class ArangoPropertyValueProvider implements PropertyValueProvider<ArangoPersistentProperty> {

		private final ArangoPersistentEntity<?> entity;
		private final VPackSlice source;
		private final String id;

		public ArangoPropertyValueProvider(final ArangoPersistentEntity<?> entity, final VPackSlice source) {
			this.entity = entity;
			this.source = source;
			this.id = source.get(_ID).isString() ? source.get(_ID).getAsString() : null;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T getPropertyValue(final ArangoPersistentProperty property) {
			final VPackSlice value = source.get(property.getFieldName());
			return (T) readPropertyValue(entity, id, value, property);
		}

	}

	@Override
	public void write(final Object source, final VPackBuilder sink) {
		if (source == null) {
			writeSimple(null, null, sink);
			return;
		}

		final Object entity = source instanceof LazyLoadingProxy ? ((LazyLoadingProxy) source).getEntity() : source;

		writeInternal(null, entity, sink, ClassTypeInformation.OBJECT);
	}

	@SuppressWarnings("unchecked")
	private void writeInternal(
		final String attribute,
		final Object source,
		final VPackBuilder sink,
		final TypeInformation<?> definedType) {

		final Class<?> rawType = source.getClass();
		final TypeInformation<?> type = ClassTypeInformation.from(rawType);

		if (conversions.isSimpleType(rawType)) {
			final Optional<Class<?>> customWriteTarget = conversions.getCustomWriteTarget(rawType);
			final Class<?> targetType = customWriteTarget.orElse(rawType);
			writeSimple(attribute, conversionService.convert(source, targetType), sink);
		}

		else if (BaseDocument.class.equals(rawType)) {
			writeBaseDocument(attribute, (BaseDocument) source, sink, definedType);
		}

		else if (BaseEdgeDocument.class.equals(rawType)) {
			writeBaseEdgeDocument(attribute, (BaseEdgeDocument) source, sink, definedType);
		}

		else if (type.isMap()) {
			writeMap(attribute, (Map<Object, Object>) source, sink, definedType);
		}

		else if (type.getType().isArray()) {
			writeArray(attribute, source, sink, definedType);
		}

		else if (type.isCollectionLike()) {
			writeCollection(attribute, source, sink, definedType);
		}

		else {
			final ArangoPersistentEntity<?> entity = context.getRequiredPersistentEntity(source.getClass());
			writeEntity(attribute, source, sink, entity, definedType);
		}
	}

	private void writeEntity(
		final String attribute,
		final Object source,
		final VPackBuilder sink,
		final ArangoPersistentEntity<?> entity,
		final TypeInformation<?> definedType) {

		sink.add(attribute, ValueType.OBJECT);

		final PersistentPropertyAccessor<?> accessor = entity.getPropertyAccessor(source);

		entity.doWithProperties((final ArangoPersistentProperty property) -> {
			if (!property.isWritable()) {
				return;
			}
			if (property.isIdProperty()) {
				final Object id = entity.getIdentifierAccessor(source).getIdentifier();
				if (id != null) {
					sink.add(_KEY, convertId(id));
				}
				return;
			}
			final Object value = accessor.getProperty(property);
			if (value != null) {
				writeProperty(value, sink, property);
			}
		});

		entity.doWithAssociations((final Association<ArangoPersistentProperty> association) -> {
			final ArangoPersistentProperty inverse = association.getInverse();
			final Object value = accessor.getProperty(inverse);
			if (value != null) {
				writeProperty(value, sink, inverse);
			}
		});

		addKeyIfNecessary(entity, source, sink);
		addTypeKeyIfNecessary(definedType, source, sink);

		sink.close();
	}

	private void addKeyIfNecessary(
		final ArangoPersistentEntity<?> entity,
		final Object source,
		final VPackBuilder sink) {
		if (!entity.hasIdProperty() || entity.getIdentifierAccessor(source).getIdentifier() == null) {
			final Object id = entity.getArangoIdAccessor(source).getIdentifier();
			if (id != null) {
				sink.add(_KEY, MetadataUtils.determineDocumentKeyFromId((String) id));
			}
		}
	}

	private void writeProperty(final Object source, final VPackBuilder sink, final ArangoPersistentProperty property) {
		if (source == null) {
			return;
		}

		final TypeInformation<?> sourceType = ClassTypeInformation.from(source.getClass());
		final String fieldName = property.getFieldName();

		if (property.getRef().isPresent()) {
			if (sourceType.isCollectionLike()) {
				writeReferences(fieldName, source, sink,property.getRef().get());
			} else {
				writeReference(fieldName, source, sink,property.getRef().get());
			}
		}

		else if (property.getRelations().isPresent()) {
			// nothing to store
		}

		else if (property.getFrom().isPresent() || property.getTo().isPresent()) {
			if (!sourceType.isCollectionLike()) {
				writeReference(fieldName, source, sink, null);
			}
		}

		else {
			final Object entity = source instanceof LazyLoadingProxy ? ((LazyLoadingProxy) source).getEntity() : source;
			writeInternal(fieldName, entity, sink, property.getTypeInformation());
		}
	}

	private void writeMap(
		final String attribute,
		final Map<? extends Object, ? extends Object> source,
		final VPackBuilder sink,
		final TypeInformation<?> definedType) {

		sink.add(attribute, ValueType.OBJECT);

		for (final Entry<? extends Object, ? extends Object> entry : source.entrySet()) {
			final Object key = entry.getKey();
			final Object value = entry.getValue();

			String convertedKey = convertId(key);
			if (value != null) {
				writeInternal(convertedKey, value, sink, getNonNullMapValueType(definedType));
			}
		}

		sink.close();
	}

	private void writeCollection(
		final String attribute,
		final Object source,
		final VPackBuilder sink,
		final TypeInformation<?> definedType) {

		sink.add(attribute, ValueType.ARRAY);

		for (final Object entry : asCollection(source)) {
			if (entry == null) {
				writeSimple(null, null, sink);
			} else {
				writeInternal(null, entry, sink, getNonNullComponentType(definedType));
			}
		}

		sink.close();
	}

	private void writeArray(
		final String attribute,
		final Object source,
		final VPackBuilder sink,
		final TypeInformation<?> definedType) {

		if (byte[].class.equals(source.getClass())) {
			sink.add(attribute, Base64Utils.encodeToString((byte[]) source));
		}

		else {
			sink.add(attribute, ValueType.ARRAY);
			for (int i = 0; i < Array.getLength(source); ++i) {
				final Object element = Array.get(source, i);
				if (element == null) {
					writeSimple(null, null, sink);
				} else {
					writeInternal(null, element, sink, getNonNullComponentType(definedType));
				}
			}
			sink.close();
		}
	}

	private void writeReferences(final String attribute, final Object source, final VPackBuilder sink, final Ref annotation) {
		sink.add(attribute, ValueType.ARRAY);

		if (source.getClass().isArray()) {
			for (int i = 0; i < Array.getLength(source); ++i) {
				final Object element = Array.get(source, i);
				writeReference(null, element, sink,annotation);
			}
		}

		else {
			for (final Object element : asCollection(source)) {
				writeReference(null, element, sink,annotation);
			}
		}

		sink.close();
	}

	private void writeReference(final String attribute, final Object source, final VPackBuilder sink, final Ref annotation) {
		getRefId(source, annotation).ifPresent(id -> sink.add(attribute, id));
	}

	@SuppressWarnings("unchecked")
	private void writeSimple(final String attribute, final Object source, final VPackBuilder sink) {
		if (source == null) {
			sink.add(ValueType.NULL);
		}
		// com.arangodb.*
		else if (source instanceof VPackSlice) {
			sink.add(attribute, (VPackSlice) source);
		} //
		else if (source instanceof DBDocumentEntity) {
			writeMap(attribute, (Map<String, Object>) source, sink, ClassTypeInformation.MAP);
		}
		// java.lang.*
		else if (source instanceof Boolean) {
			sink.add(attribute, (Boolean) source);
		} //
		else if (source instanceof Byte) {
			sink.add(attribute, (Byte) source);
		} //
		else if (source instanceof Character) {
			sink.add(attribute, (Character) source);
		} //
		else if (source instanceof Short) {
			sink.add(attribute, (Short) source);
		} //
		else if (source instanceof Integer) {
			sink.add(attribute, (Integer) source);
		} //
		else if (source instanceof Long) {
			sink.add(attribute, (Long) source);
		} //
		else if (source instanceof Float) {
			sink.add(attribute, (Float) source);
		} //
		else if (source instanceof Double) {
			sink.add(attribute, (Double) source);
		} //
		else if (source instanceof String) {
			sink.add(attribute, (String) source);
		} //
		else if (source instanceof Class) {
			sink.add(attribute, ((Class<?>) source).getName());
		} //
		else if (source instanceof Enum) {
			sink.add(attribute, ((Enum<?>) source).name());
		}
		// primitive arrays
		else if (ClassUtils.isPrimitiveArray(source.getClass())) {
			writeArray(attribute, source, sink, ClassTypeInformation.OBJECT);
		}
		// java.util.Date / java.sql.Date / java.sql.Timestamp
		else if (source instanceof Date) {
			sink.add(attribute, DateUtil.format((Date) source));
		}
		// java.math.*
		else if (source instanceof BigInteger) {
			sink.add(attribute, (BigInteger) source);
		} //
		else if (source instanceof BigDecimal) {
			sink.add(attribute, (BigDecimal) source);
		}
		// java.time.*
		else if (source instanceof Instant) {
			sink.add(attribute, JavaTimeUtil.format((Instant) source));
		} //
		else if (source instanceof LocalDate) {
			sink.add(attribute, JavaTimeUtil.format((LocalDate) source));
		} //
		else if (source instanceof LocalDateTime) {
			sink.add(attribute, JavaTimeUtil.format((LocalDateTime) source));
		} //
		else if (source instanceof OffsetDateTime) {
			sink.add(attribute, JavaTimeUtil.format((OffsetDateTime) source));
		} //
		else if (source instanceof ZonedDateTime) {
			sink.add(attribute, JavaTimeUtil.format((ZonedDateTime) source));
		} //
		else {
			throw new MappingException(String.format("Type %s is not a simple type!", source.getClass()));
		}
	}

	private void writeBaseDocument(
		final String attribute,
		final BaseDocument source,
		final VPackBuilder sink,
		final TypeInformation<?> definedType) {

		final VPackBuilder builder = new VPackBuilder();
		writeMap(attribute, source.getProperties(), builder, definedType);
		builder.add(_ID, source.getId());
		builder.add(_KEY, source.getKey());
		builder.add(_REV, source.getRevision());
		sink.add(attribute, builder.slice());
	}

	private void writeBaseEdgeDocument(
		final String attribute,
		final BaseEdgeDocument source,
		final VPackBuilder sink,
		final TypeInformation<?> definedType) {

		final VPackBuilder builder = new VPackBuilder();
		writeMap(attribute, source.getProperties(), builder, definedType);
		builder.add(_ID, source.getId());
		builder.add(_KEY, source.getKey());
		builder.add(_REV, source.getRevision());
		builder.add(_FROM, source.getFrom());
		builder.add(_TO, source.getTo());
		sink.add(attribute, builder.slice());
	}

	private Optional<String> getRefId(final Object source, final Ref annotation) {
		return getRefId(source, context.getPersistentEntity(source.getClass()),annotation);
	}

	private Optional<String> getRefId(final Object source, final ArangoPersistentEntity<?> entity, final Ref annotation) {
		if (source instanceof LazyLoadingProxy) {
			return Optional.of(((LazyLoadingProxy) source).getRefId());
		}

		final Optional<Object> id = Optional.ofNullable(entity.getIdentifierAccessor(source).getIdentifier());
		if (id.isPresent()) {
			if(annotation != null){
				final Optional<ReferenceResolver<Annotation>> resolver = resolverFactory.getReferenceResolver(annotation);
				return id.map(key -> resolver.get().write(source, entity, convertId(key),annotation));
			} else {
				return id.map(key -> MetadataUtils.createIdFromCollectionAndKey(entity.getCollection(), convertId(key)));
			}

		}

		return Optional.ofNullable((String) entity.getArangoIdAccessor(source).getIdentifier());
	}

	private static Collection<?> asCollection(final Object source) {
		return (source instanceof Collection) ? Collection.class.cast(source)
				: source.getClass().isArray() ? CollectionUtils.arrayToList(source) : Collections.singleton(source);
	}

	private boolean isSimpleType(final Class<?> type) {
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
		return !isSimpleType(type) && !isMapType(type) && !isCollectionType(type);
	}

	@SuppressWarnings("unchecked")
	private <T> T convertIfNecessary(final Object source, final Class<T> type) {
		return (T) (source == null ? source
				: type.isAssignableFrom(source.getClass()) ? source : conversionService.convert(source, type));
	}

	private void addTypeKeyIfNecessary(
		final TypeInformation<?> definedType,
		final Object value,
		final VPackBuilder sink) {

		final Class<?> referenceType = definedType != null ? definedType.getType() : Object.class;
		final Class<?> valueType = ClassUtils.getUserClass(value.getClass());
		if (!valueType.equals(referenceType)) {
			typeMapper.writeType(valueType, sink);
		}
	}

	private boolean isValidId(final Object key) {
		if (key == null) {
			return false;
		}

		final Class<?> type = key.getClass();
		if (DBDocumentEntity.class.isAssignableFrom(type)) {
			return false;
		} else if (VPackSlice.class.isAssignableFrom(type)) {
			return false;
		} else if (type.isArray() && type.getComponentType() != byte.class) {
			return false;
		} else if (isSimpleType(type)) {
			return true;
		} else {
			return conversions.hasCustomWriteTarget(key.getClass(), String.class);
		}
	}

	@Override
	public String convertId(final Object id) {
		if (!isValidId(id)) {
			throw new MappingException(
					String.format("Type %s is not a valid id type!", id != null ? id.getClass() : "null"));
		}
		if (id instanceof String) {
			return id.toString();
		}
		final boolean hasCustomConverter = conversions.hasCustomWriteTarget(id.getClass(), String.class);
		return hasCustomConverter ? conversionService.convert(id, String.class) : id.toString();
	}

	private TypeInformation<?> getNonNullComponentType(final TypeInformation<?> type) {
		final TypeInformation<?> compType = type.getComponentType();
		return compType != null ? compType : ClassTypeInformation.OBJECT;
	}

	private TypeInformation<?> getNonNullMapValueType(final TypeInformation<?> type) {
		final TypeInformation<?> valueType = type.getMapValueType();
		return valueType != null ? valueType : ClassTypeInformation.OBJECT;
	}

	private Date parseDate(final String source) {
		try {
			return DateUtil.parse(source);
		} catch (final ParseException e) {
			throw new MappingException(String.format("Can't parse java.util.Date from String %s!", source), e);
		}
	}

}
