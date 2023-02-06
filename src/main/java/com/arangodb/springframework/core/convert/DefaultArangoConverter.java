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
import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.Edge;
import com.arangodb.springframework.annotation.From;
import com.arangodb.springframework.annotation.Ref;
import com.arangodb.springframework.annotation.Relations;
import com.arangodb.springframework.annotation.To;
import com.arangodb.springframework.core.convert.resolver.LazyLoadingProxy;
import com.arangodb.springframework.core.convert.resolver.ReferenceResolver;
import com.arangodb.springframework.core.convert.resolver.RelationResolver;
import com.arangodb.springframework.core.convert.resolver.ResolverFactory;
import com.arangodb.springframework.core.mapping.ArangoPersistentEntity;
import com.arangodb.springframework.core.mapping.ArangoPersistentProperty;
import com.arangodb.springframework.core.mapping.ArangoSimpleTypes;
import com.arangodb.springframework.core.util.MetadataUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
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
    public <R> R read(final Class<R> type, final JsonNode source) {
        return (R) readInternal(ClassTypeInformation.from(type), source);
    }

    private Object readInternal(final TypeInformation<?> type, final JsonNode source) {
        if (source == null) {
            return null;
        }

        if (JsonNode.class.isAssignableFrom(type.getType())) {
            return source;
        }

        TypeInformation<?> typeToUse = (source.isArray() || source.isObject()) ? typeMapper.readType(source, type)
                : type;
        Class<?> rawTypeToUse = typeToUse.getType();

        if (conversions.hasCustomReadTarget(JsonNode.class, typeToUse.getType())) {
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

        if (BaseEdgeDocument.class.isAssignableFrom(rawTypeToUse)) {
            return readBaseEdgeDocument(rawTypeToUse, source);
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

        ArangoPersistentEntity<?> entity = context.getRequiredPersistentEntity(rawTypeToUse);
        return readEntity(typeToUse, source, entity);
    }

    private Object readEntity(
            final TypeInformation<?> type,
            final JsonNode source,
            final ArangoPersistentEntity<?> entity) {

        if (!source.isObject()) {
            throw new MappingException(
                    String.format("Can't read entity type %s from VPack type %s!", type, source.getNodeType()));
        }

        EntityInstantiator instantiator = instantiators.getInstantiatorFor(entity);
        ParameterValueProvider<ArangoPersistentProperty> provider = getParameterProvider(entity, source);
        Object instance = instantiator.createInstance(entity, provider);
        PersistentPropertyAccessor<?> accessor = entity.getPropertyAccessor(instance);

        JsonNode idNode = getOrMissing(source, _ID);
        String id = idNode.isTextual() ? idNode.textValue() : null;

        entity.doWithProperties((ArangoPersistentProperty property) -> {
            if (!entity.isConstructorArgument(property)) {
                JsonNode value = getOrMissing(source, property.getFieldName());
                readProperty(entity, id, accessor, value, property);
            }
        });

        entity.doWithAssociations((Association<ArangoPersistentProperty> association) -> {
            ArangoPersistentProperty property = association.getInverse();
            if (!entity.isConstructorArgument(property)) {
                JsonNode value = getOrMissing(source, property.getFieldName());
                readProperty(entity, id, accessor, value, property);
            }
        });

        return instance;
    }

    private void readProperty(
            final ArangoPersistentEntity<?> entity,
            final String parentId,
            final PersistentPropertyAccessor<?> accessor,
            final JsonNode source,
            final ArangoPersistentProperty property) {

        Object propertyValue = readPropertyValue(entity, parentId, source, property);
        if (propertyValue != null || !property.getType().isPrimitive()) {
            accessor.setProperty(property, propertyValue);
        }
    }

    private Object readPropertyValue(
            final ArangoPersistentEntity<?> entity,
            final String parentId,
            final JsonNode source,
            final ArangoPersistentProperty property) {

        Optional<Ref> ref = property.getRef();
        if (ref.isPresent()) {
            return readReference(source, property, ref.get()).orElse(null);
        }

        Optional<Relations> relations = property.getRelations();
        if (relations.isPresent()) {
            return readRelation(entity, parentId, source, property, relations.get()).orElse(null);
        }

        Optional<From> from = property.getFrom();
        if (from.isPresent()) {
            return readRelation(entity, parentId, source, property, from.get()).orElse(null);
        }

        Optional<To> to = property.getTo();
        if (to.isPresent()) {
            return readRelation(entity, parentId, source, property, to.get()).orElse(null);
        }

        return readInternal(property.getTypeInformation(), source);
    }

    private Object readMap(final TypeInformation<?> type, final JsonNode source) {
        if (!source.isObject()) {
            throw new MappingException(
                    String.format("Can't read map type %s from type %s!", type, source.getNodeType()));
        }

        ObjectNode node = (ObjectNode) source;
        Class<?> keyType = getNonNullComponentType(type).getType();
        TypeInformation<?> valueType = getNonNullMapValueType(type);
        Map<Object, Object> map = CollectionFactory.createMap(type.getType(), keyType, node.size());
        Iterator<Entry<String, JsonNode>> iterator = node.fields();

        while (iterator.hasNext()) {
            Entry<String, JsonNode> entry = iterator.next();
            if (typeMapper.isTypeKey(entry.getKey())) {
                continue;
            }

            Object key = convertIfNecessary(entry.getKey(), keyType);
            JsonNode value = entry.getValue();

            map.put(key, readInternal(valueType, value));
        }

        return map;
    }

    private Collection<?> readCollection(final TypeInformation<?> type, final JsonNode source) {
        if (!source.isArray()) {
            throw new MappingException(
                    String.format("Can't read collection type %s from type %s!", type, source.getNodeType()));
        }

        ArrayNode node = (ArrayNode) source;
        TypeInformation<?> componentType = getNonNullComponentType(type);
        Class<?> collectionType = Iterable.class.equals(type.getType()) ? Collection.class : type.getType();

        Collection<Object> collection = Collection.class == collectionType || List.class == collectionType ?
                new ArrayList<>(node.size()) :
                CollectionFactory.createCollection(collectionType, componentType.getType(), node.size());

        Iterator<JsonNode> iterator = source.iterator();

        while (iterator.hasNext()) {
            JsonNode elem = iterator.next();
            collection.add(readInternal(componentType, elem));
        }

        return collection;
    }

    private Object readArray(final TypeInformation<?> type, final JsonNode source) {
        if (!source.isArray()) {
            throw new MappingException(
                    String.format("Can't read array type %s from type %s!", type, source.getNodeType()));
        }

        ArrayNode node = (ArrayNode) source;
        TypeInformation<?> componentType = getNonNullComponentType(type);
        int length = node.size();
        Object array = Array.newInstance(componentType.getType(), length);

        for (int i = 0; i < length; ++i) {
            Array.set(array, i, readInternal(componentType, node.get(i)));
        }

        return array;
    }

    @SuppressWarnings("unchecked")
    private Optional<Object> readReference(
            final JsonNode source,
            final ArangoPersistentProperty property,
            final Annotation annotation) {

        Optional<ReferenceResolver<Annotation>> resolver = resolverFactory.getReferenceResolver(annotation);

        if (!resolver.isPresent() || source.isMissingNode() || source.isNull()) {
            return Optional.empty();
        } else if (property.isCollectionLike()) {
            Collection<String> ids;
            try {
                ids = (Collection<String>) readCollection(ClassTypeInformation.COLLECTION, source);
            } catch (ClassCastException e) {
                throw new MappingException("All references must be of type String!", e);
            }

            return resolver.map(res -> res.resolveMultiple(ids, property.getTypeInformation(), annotation));
        } else {
            if (!source.isTextual()) {
                throw new MappingException(
                        String.format("A reference must be of type String, but got type %s!", source.getNodeType()));
            }

            return resolver.map(res -> res.resolveOne(source.textValue(), property.getTypeInformation(), annotation));
        }
    }

    private <A extends Annotation> Optional<Object> readRelation(
            final ArangoPersistentEntity<?> entity,
            final String parentId,
            final JsonNode source,
            final ArangoPersistentProperty property,
            final A annotation) {

        Class<? extends Annotation> collectionType = entity.findAnnotation(Edge.class) != null ? Edge.class
                : Document.class;
        Optional<RelationResolver<Annotation>> resolver = resolverFactory.getRelationResolver(annotation,
                collectionType);

        // FIXME: discover intermediate types, in case annotation is Relations and maxDepth > 1
        List<TypeInformation<?>> traversedTypes = Collections.singletonList(entity.getTypeInformation());

        if (!resolver.isPresent()) {
            return Optional.empty();
        } else if (property.isCollectionLike()) {
            if (parentId == null) {
                return Optional.empty();
            }
            return resolver.map(res -> res.resolveMultiple(parentId, property.getTypeInformation(), traversedTypes, annotation));
        } else if (source.isTextual()) {
            return resolver.map(res -> res.resolveOne(source.textValue(), property.getTypeInformation(), traversedTypes, annotation));
        } else {
            return resolver.map(res -> res.resolveOne(parentId, property.getTypeInformation(), traversedTypes, annotation));
        }

    }

    private Object readSimple(final Class<?> type, final JsonNode source) {
        if (source.isMissingNode() || source.isNull()) {
            return null;
        }

        if (source.isBoolean()) {
            return source.booleanValue();
        }

        if (source.isNumber()) {
            if (byte.class.isAssignableFrom(type) || Byte.class.isAssignableFrom(type)) {
                return source.numberValue().byteValue();
            } else if (short.class.isAssignableFrom(type) || Short.class.isAssignableFrom(type)) {
                return source.shortValue();
            } else if (int.class.isAssignableFrom(type) || Integer.class.isAssignableFrom(type)) {
                return source.intValue();
            } else if (long.class.isAssignableFrom(type) || Long.class.isAssignableFrom(type)) {
                return source.longValue();
            } else if (float.class.isAssignableFrom(type) || Float.class.isAssignableFrom(type)) {
                return source.floatValue();
            } else if (double.class.isAssignableFrom(type) || Double.class.isAssignableFrom(type)) {
                return source.doubleValue();
            } else if (BigInteger.class.isAssignableFrom(type) && (source.isIntegralNumber())) {
                return source.bigIntegerValue();
            } else if (BigDecimal.class.isAssignableFrom(type) && source.isFloatingPointNumber()) {
                return source.decimalValue();
            } else {
                return source.numberValue();
            }
        }

        if (source.isTextual()) {
            String value = source.textValue();
            if (Class.class.isAssignableFrom(type)) {
                try {
                    return Class.forName(value);
                } catch (ClassNotFoundException e) {
                    throw new MappingException(String.format("Could not load type %s!", value), e);
                }
            } else if (Enum.class.isAssignableFrom(type)) {
                @SuppressWarnings({"unchecked", "rawtypes"})
                Enum<?> e = Enum.valueOf((Class<? extends Enum>) type, value);
                return e;
            } else if (byte[].class.isAssignableFrom(type)) {
                return Base64Utils.decodeFromString(value);
            } else if (java.sql.Date.class.isAssignableFrom(type)) {
                return new java.sql.Date(parseDate(value).getTime());
            } else if (Timestamp.class.isAssignableFrom(type)) {
                return new Timestamp(parseDate(value).getTime());
            } else if (Date.class.isAssignableFrom(type)) {
                return parseDate(value);
            } else if (BigInteger.class.isAssignableFrom(type)) {
                return new BigInteger(value);
            } else if (BigDecimal.class.isAssignableFrom(type)) {
                return new BigDecimal(value);
            } else if (Instant.class.isAssignableFrom(type)) {
                return JavaTimeUtil.parseInstant(value);
            } else if (LocalDate.class.isAssignableFrom(type)) {
                return JavaTimeUtil.parseLocalDate(value);
            } else if (LocalDateTime.class.isAssignableFrom(type)) {
                return JavaTimeUtil.parseLocalDateTime(value);
            } else if (OffsetDateTime.class.isAssignableFrom(type)) {
                return JavaTimeUtil.parseOffsetDateTime(value);
            } else if (ZonedDateTime.class.isAssignableFrom(type)) {
                return JavaTimeUtil.parseZonedDateTime(value);
            } else {
                return value;
            }
        }

        if (source.isObject() && DBDocumentEntity.class.isAssignableFrom(type)) {
            return readDBDocumentEntity(source);
        }

        throw new MappingException(String.format("Can't read type %s from VPack type %s!", type, source.getNodeType()));
    }

    private BaseDocument readBaseDocument(final Class<?> type, final JsonNode source) {
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) readMap(ClassTypeInformation.MAP, source);

        if (BaseDocument.class.equals(type)) {
            return new BaseDocument(properties);
        } //
        else {
            throw new MappingException(String.format("Can't read type %s as %s!", type, BaseDocument.class));
        }
    }

    private BaseEdgeDocument readBaseEdgeDocument(final Class<?> type, final JsonNode source) {
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) readMap(ClassTypeInformation.MAP, source);

        if (BaseEdgeDocument.class.equals(type)) {
            return new BaseEdgeDocument(properties);
        } //
        else {
            throw new MappingException(String.format("Can't read type %s as %s!", type, BaseEdgeDocument.class));
        }
    }

    @SuppressWarnings("unchecked")
    private DBDocumentEntity readDBDocumentEntity(final JsonNode source) {
        return new DBDocumentEntity((Map<String, Object>) readMap(ClassTypeInformation.MAP, source));
    }

    private ParameterValueProvider<ArangoPersistentProperty> getParameterProvider(
            final ArangoPersistentEntity<?> entity,
            final JsonNode source) {

        PropertyValueProvider<ArangoPersistentProperty> provider = new ArangoPropertyValueProvider(entity, source);
        return new PersistentEntityParameterValueProvider<>(entity, provider, null);
    }

    private class ArangoPropertyValueProvider implements PropertyValueProvider<ArangoPersistentProperty> {

        private final ArangoPersistentEntity<?> entity;
        private final JsonNode source;
        private final String id;

        public ArangoPropertyValueProvider(final ArangoPersistentEntity<?> entity, final JsonNode source) {
            this.entity = entity;
            this.source = source;
            JsonNode idNode = getOrMissing(source, _ID);
            this.id = idNode.isTextual() ? idNode.textValue() : null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getPropertyValue(final ArangoPersistentProperty property) {
            JsonNode value = getOrMissing(source, property.getFieldName());
            return (T) readPropertyValue(entity, id, value, property);
        }

    }

    @Override
    public JsonNode write(final Object source) {
        Object entity = source instanceof LazyLoadingProxy ? ((LazyLoadingProxy) source).getEntity() : source;
        return createInternal(entity, ClassTypeInformation.OBJECT);
    }

    private JsonNode createInternal(final Object source, final TypeInformation<?> definedType) {
        if (source == null) {
            return JsonNodeFactory.instance.nullNode();
        }

        Class<?> rawType = source.getClass();
        TypeInformation<?> type = ClassTypeInformation.from(rawType);

        if (conversions.isSimpleType(rawType)) {
            Optional<Class<?>> customWriteTarget = conversions.getCustomWriteTarget(rawType);
            Class<?> targetType = customWriteTarget.orElse(rawType);
            return createSimpleJsonNode(conversionService.convert(source, targetType));
        } else if (BaseDocument.class.equals(rawType)) {
            return createBaseDocument((BaseDocument) source, definedType);
        } else if (BaseEdgeDocument.class.equals(rawType)) {
            return createBaseEdgeDocument((BaseEdgeDocument) source, definedType);
        } else if (type.isMap()) {
            return createMap((Map<?, ?>) source, definedType);
        } else if (type.getType().isArray()) {
            return createArray(source, definedType);
        } else if (type.isCollectionLike()) {
            return createCollection(source, definedType);
        } else {
            ArangoPersistentEntity<?> entity = context.getRequiredPersistentEntity(source.getClass());
            return createEntity(source, entity, definedType);
        }
    }

    private ObjectNode createEntity(final Object source, final ArangoPersistentEntity<?> entity, final TypeInformation<?> definedType) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        PersistentPropertyAccessor<?> accessor = entity.getPropertyAccessor(source);
        entity.doWithProperties((ArangoPersistentProperty property) -> {
            if (!property.isWritable()) {
                return;
            }
            if (property.isIdProperty()) {
                Object id = entity.getIdentifierAccessor(source).getIdentifier();
                if (id != null) {
                    node.put(_KEY, convertId(id));
                }
                return;
            }
            Object value = accessor.getProperty(property);
            if (value != null) {
                addProperty(value, property, node);
            }
        });

        entity.doWithAssociations((Association<ArangoPersistentProperty> association) -> {
            ArangoPersistentProperty inverse = association.getInverse();
            Object value = accessor.getProperty(inverse);
            if (value != null) {
                addProperty(value, inverse, node);
            }
        });

        addKeyIfNecessary(entity, source, node);
        addTypeKeyIfNecessary(definedType, source, node);

        return node;
    }

    private void addKeyIfNecessary(
            final ArangoPersistentEntity<?> entity,
            final Object source,
            final ObjectNode node) {
        if (!entity.hasIdProperty() || entity.getIdentifierAccessor(source).getIdentifier() == null) {
            Object id = entity.getArangoIdAccessor(source).getIdentifier();
            if (id != null) {
                node.put(_KEY, MetadataUtils.determineDocumentKeyFromId((String) id));
            }
        }
    }

    private void addProperty(final Object source, final ArangoPersistentProperty property, final ObjectNode node) {
        if (source == null) {
            return;
        }

        TypeInformation<?> sourceType = ClassTypeInformation.from(source.getClass());
        String fieldName = property.getFieldName();

        if (property.getRef().isPresent()) {
            if (sourceType.isCollectionLike()) {
                node.put(fieldName, createReferences(source, property.getRef().get()));
            } else {
                getRefId(source, property.getRef().get()).ifPresent(id -> node.put(fieldName, id));
            }
        } else if (property.getRelations().isPresent()) {
            // nothing to store
        } else if (property.getFrom().isPresent() || property.getTo().isPresent()) {
            if (!sourceType.isCollectionLike()) {
                getRefId(source, null).ifPresent(id -> node.put(fieldName, id));
            }
        } else {
            Object entity = source instanceof LazyLoadingProxy ? ((LazyLoadingProxy) source).getEntity() : source;
            node.put(fieldName, createInternal(entity, property.getTypeInformation()));
        }
    }

    private ObjectNode createMap(final Map<?, ?> source, final TypeInformation<?> definedType) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        for (Entry<?, ?> entry : source.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            String convertedKey = convertId(key);
            if (value != null) {
                node.put(convertedKey, createInternal(value, getNonNullMapValueType(definedType)));
            }
        }
        return node;
    }

    private ArrayNode createCollection(final Object source, final TypeInformation<?> definedType) {
        ArrayNode node = JsonNodeFactory.instance.arrayNode();
        for (Object entry : asCollection(source)) {
            if (entry == null) {
                node.addNull();
            } else {
                node.add(createInternal(entry, getNonNullComponentType(definedType)));
            }
        }
        return node;
    }

    private JsonNode createArray(final Object source, final TypeInformation<?> definedType) {
        if (byte[].class.equals(source.getClass())) {
            return JsonNodeFactory.instance.textNode(Base64Utils.encodeToString((byte[]) source));
        }

        ArrayNode node = JsonNodeFactory.instance.arrayNode();
        for (int i = 0; i < Array.getLength(source); ++i) {
            Object element = Array.get(source, i);
            if (element == null) {
                node.addNull();
            } else {
                node.add(createInternal(element, getNonNullComponentType(definedType)));
            }
        }
        return node;
    }

    private ArrayNode createReferences(final Object source, final Ref annotation) {
        ArrayNode node = JsonNodeFactory.instance.arrayNode();

        if (source.getClass().isArray()) {
            for (int i = 0; i < Array.getLength(source); ++i) {
                Object element = Array.get(source, i);
                getRefId(element, annotation).ifPresent(node::add);
            }
        } else {
            for (Object element : asCollection(source)) {
                getRefId(element, annotation).ifPresent(node::add);
            }
        }

        return node;
    }

    private void writeAttribute(final String attribute, final Object source, final ObjectNode sink) {
        sink.set(attribute, createSimpleJsonNode(source));
    }

    private JsonNode createSimpleJsonNode(final Object source) {
        if (source == null) {
            return JsonNodeFactory.instance.nullNode();
        } else if (source instanceof JsonNode) {
            return (JsonNode) source;
        } else if (source instanceof DBDocumentEntity) {
            return createMap((Map<?, ?>) source, ClassTypeInformation.MAP);
        } else if (source instanceof Boolean) {
            return JsonNodeFactory.instance.booleanNode((Boolean) source);
        } else if (source instanceof Byte) {
            return JsonNodeFactory.instance.numberNode((Byte) source);
        } else if (source instanceof Character) {
            return JsonNodeFactory.instance.textNode(source.toString());
        } else if (source instanceof Short) {
            return JsonNodeFactory.instance.numberNode((Short) source);
        } else if (source instanceof Integer) {
            return JsonNodeFactory.instance.numberNode((Integer) source);
        } else if (source instanceof Long) {
            return JsonNodeFactory.instance.numberNode((Long) source);
        } else if (source instanceof Float) {
            return JsonNodeFactory.instance.numberNode((Float) source);
        } else if (source instanceof Double) {
            return JsonNodeFactory.instance.numberNode((Double) source);
        } else if (source instanceof String) {
            return JsonNodeFactory.instance.textNode((String) source);
        } else if (source instanceof Class) {
            return JsonNodeFactory.instance.textNode(((Class<?>) source).getName());
        } else if (source instanceof Enum) {
            return JsonNodeFactory.instance.textNode(((Enum<?>) source).name());
        } else if (ClassUtils.isPrimitiveArray(source.getClass())) {
            return createArray(source, ClassTypeInformation.OBJECT);
        } else if (source instanceof Date) {
            return JsonNodeFactory.instance.textNode(JavaTimeUtil.format((Date) source));
        } else if (source instanceof BigInteger) {
            return JsonNodeFactory.instance.numberNode((BigInteger) source);
        } else if (source instanceof BigDecimal) {
            return JsonNodeFactory.instance.numberNode((BigDecimal) source);
        } else if (source instanceof Instant) {
            return JsonNodeFactory.instance.textNode(JavaTimeUtil.format((Instant) source));
        } else if (source instanceof LocalDate) {
            return JsonNodeFactory.instance.textNode(JavaTimeUtil.format((LocalDate) source));
        } else if (source instanceof LocalDateTime) {
            return JsonNodeFactory.instance.textNode(JavaTimeUtil.format((LocalDateTime) source));
        } else if (source instanceof OffsetDateTime) {
            return JsonNodeFactory.instance.textNode(JavaTimeUtil.format((OffsetDateTime) source));
        } else if (source instanceof ZonedDateTime) {
            return JsonNodeFactory.instance.textNode(JavaTimeUtil.format((ZonedDateTime) source));
        } else {
            throw new MappingException(String.format("Type %s is not a simple type!", source.getClass()));
        }
    }

    private ObjectNode createBaseDocument(final BaseDocument source, final TypeInformation<?> definedType) {
        return createMap(source.getProperties(), definedType)
                .put(_ID, source.getId())
                .put(_KEY, source.getKey())
                .put(_REV, source.getRevision());
    }

    private ObjectNode createBaseEdgeDocument(
            final BaseEdgeDocument source,
            final TypeInformation<?> definedType) {
        return createMap(source.getProperties(), definedType)
                .put(_ID, source.getId())
                .put(_KEY, source.getKey())
                .put(_REV, source.getRevision())
                .put(_FROM, source.getFrom())
                .put(_TO, source.getTo());
    }

    private Optional<String> getRefId(final Object source, final Ref annotation) {
        return getRefId(source, context.getPersistentEntity(source.getClass()), annotation);
    }

    private Optional<String> getRefId(final Object source, final ArangoPersistentEntity<?> entity, final Ref annotation) {
        if (source instanceof LazyLoadingProxy) {
            return Optional.of(((LazyLoadingProxy) source).getRefId());
        }

        Optional<Object> id = Optional.ofNullable(entity.getIdentifierAccessor(source).getIdentifier());
        if (id.isPresent()) {
            if (annotation != null) {
                Optional<ReferenceResolver<Annotation>> resolver = resolverFactory.getReferenceResolver(annotation);
                return id.map(key -> resolver.get().write(source, entity, convertId(key), annotation));
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
            final ObjectNode node) {

        Class<?> referenceType = definedType != null ? definedType.getType() : Object.class;
        Class<?> valueType = ClassUtils.getUserClass(value.getClass());
        if (!valueType.equals(referenceType)) {
            typeMapper.writeType(valueType, node);
        }
    }

    private boolean isValidId(final Object key) {
        if (key == null) {
            return false;
        }

        Class<?> type = key.getClass();
        if (DBDocumentEntity.class.isAssignableFrom(type)) {
            return false;
        } else if (JsonNode.class.isAssignableFrom(type)) {
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
        boolean hasCustomConverter = conversions.hasCustomWriteTarget(id.getClass(), String.class);
        return hasCustomConverter ? conversionService.convert(id, String.class) : id.toString();
    }

    private TypeInformation<?> getNonNullComponentType(final TypeInformation<?> type) {
        TypeInformation<?> compType = type.getComponentType();
        return compType != null ? compType : ClassTypeInformation.OBJECT;
    }

    private TypeInformation<?> getNonNullMapValueType(final TypeInformation<?> type) {
        TypeInformation<?> valueType = type.getMapValueType();
        return valueType != null ? valueType : ClassTypeInformation.OBJECT;
    }

    private Date parseDate(final String source) {
        try {
            return JavaTimeUtil.parse(source);
        } catch (ParseException e) {
            throw new MappingException(String.format("Can't parse java.util.Date from String %s!", source), e);
        }
    }

    private JsonNode getOrMissing(final JsonNode node, final String fieldName) {
        if (!node.has(fieldName)) {
            return JsonNodeFactory.instance.missingNode();
        }

        JsonNode value = node.get(fieldName);
        if (fieldName == null) {
            return JsonNodeFactory.instance.nullNode();
        }

        return value;
    }

}
